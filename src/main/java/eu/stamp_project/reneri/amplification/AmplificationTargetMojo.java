package eu.stamp_project.reneri.amplification;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import eu.stamp_project.reneri.MutationInfo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

@Mojo(name = "amplification-targets", requiresDependencyResolution = ResolutionScope.TEST)
public class AmplificationTargetMojo extends TargetMojo {

    private CtModel projectModel;

    private HashMap<Test, AmplificationTarget> targets = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Building code model");
        buildCodeModel();
        getLog().info("Inspecting transformations");
        inspectTransformations();
        getLog().info("Writing report");
        writeReport();
    }


    private void buildCodeModel() {
        MavenLauncher launcher = new MavenLauncher(getProject().getBasedir().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.TEST_SOURCE);
        projectModel = launcher.buildModel();
    }



    private void writeReport() throws MojoExecutionException {

        try(FileWriter writer = new FileWriter(getOutputFile("amplification-targets.detailed.json"))) {
            getGson().toJson(targets.values().toArray(), writer);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not write detailed report", exc);
        }

        try(FileWriter writer = new FileWriter(getOutputFile("amplification-targets.json"))) {
            getGson().toJson(targets.values().stream().map(AmplificationTarget::getTest).toArray(), writer);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not write report", exc);
        }
    }


    @Override
    protected void readMutation(JsonReader reader) throws IOException {

        String status = null;
        String mutator = null;
        Method method = null;

        List<Test> tests = null;

        reader.beginObject();

        while(JsonToken.NAME.equals(reader.peek())) {
            String property = reader.nextName();
            switch (property) {
                case "status":
                    status = reader.nextString();
                    break;
                case "mutator":
                    mutator = reader.nextString();
                    break;
                case "method":
                    method = getGson().fromJson(reader, Method.class);
                    break;
                case "tests":
                    tests = readTests(reader);
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();


        if(status == null || mutator == null || method == null || tests == null) {
            throw new IOException("Mutation definition is missing some fields");
        }

        if(status.equals("SURVIVED")) {
            handleMutation(new Mutation(mutator, method), tests);
        }
    }

    private void handleMutation(Mutation mutation, List<Test> tests) {

        getLog().debug(String.format("Handling mutation, %s", mutation.toString()));

        for(Test test: tests) {
            //Not using HashMap::merge to avoid looking for the declaring class every time
            if(targets.containsKey(test)) {
                targets.get(test).getMutations().add(mutation);
                continue;
            }

            getLog().debug(String.format("Adquiring target %s", test));

            AmplificationTarget target =  test.isFullClass()?
                    new AmplificationTarget(test)
                    :
                    new AmplificationTarget(test, findDeclaringClass(test));
            target.getMutations().add(mutation);
            targets.put(target.getTest(), target);
        }
    }

    private List<Test> readTests(JsonReader reader) throws IOException {
        List<Test> result = Collections.emptyList();
        reader.beginObject();
        while(JsonToken.NAME.equals(reader.peek())) {
            String property = reader.nextName();
            if(property.equals("ordered")) {
                result = readTestArray(reader);
            }
            else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return result;
    }

    private List<Test> readTestArray(JsonReader reader) throws IOException {
        ArrayList<Test> result = new ArrayList<>();
        reader.beginArray();
        while(JsonToken.STRING.equals(reader.peek())) {
            result.add(fromReportedName(reader.nextString()));
        }
        reader.endArray();
        return result;
    }

    private Test fromReportedName(String name) {
        Matcher matcher = MutationInfo.TEST_CASE_NAME.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        return new Test(matcher.group("class"), matcher.group("method"));
    }

    private String findDeclaringClass(Test test) {

        String method = test.getMethod();
        String className = test.getClassName();

        List<CtClass> classes = projectModel.getElements((CtClass testClass) -> testClass.getQualifiedName().equals(className));
        if(classes.isEmpty()) {
            getLog().warn("Class " + className + " not found");
            return className;
        }
        if(classes.size() > 1) {
            getLog().warn("More than one class matching name " + className + " were found");
            return className;
        }
        CtType testClass = classes.get(0);

        if(!testClass.getMethodsByName(method).isEmpty()) {
            return className; // Method declared in the same class
        }
        CtTypeReference superClass = testClass.getSuperclass();
        while(superClass != null) {
            getLog().debug("---> Superclass: " + superClass.getQualifiedName());
            CtType superClassDeclaration = superClass.getDeclaration();
            if(superClassDeclaration == null) {
                break;
            }
            if(!superClassDeclaration.getMethodsByName(method).isEmpty()) {
                return superClassDeclaration.getQualifiedName();
            }
            superClass = superClass.getSuperclass();
        }
        return className;
    }
}


