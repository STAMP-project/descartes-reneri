package eu.stamp_project.reneri;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.stamp_project.mutationtest.descartes.codegeneration.MutationClassAdapter;
import eu.stamp_project.reneri.instrumentation.ObserverClassProcessor;
import eu.stamp_project.reneri.instrumentation.StateObserver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassWriter;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "observeTests", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class TestObservationMojo extends AbstractObservationMojo {

    @Parameter(property = "transformations", defaultValue = "${project.build.directory/mutations.json}")
    private File transformations;

    public File getTransformations() {
        return transformations;
    }

    public void setTransformations(File transformations) {
        this.transformations = transformations;
    }

    private List<MutationIdentifier> mutations;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            cleanEnvironment();

            instrumentTestClasses();

            if(noClassWasInstrumented()) {
                getLog().info("Stopping analysis as no test class was found or instrumented");
                return;
            }

            compileInstrumentedTestClasses();

            loadTransformations();

            //loadStackTraces();

            observeOriginalValues();

            observeTransformations();
        }
        catch (Throwable exc) {
            throw new MojoExecutionException("Process failed", exc);
        }
    }

    private void cleanEnvironment() throws MojoExecutionException {
        getLog().info("Cleaning previous results");
        try {
            Path testObservationDirectory = getPathTo("observations", "tests");
            FileUtils.createEmptyDirectory(testObservationDirectory);
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Clould not clean output folder", exc);
        }
    }

    private boolean noClassWasInstrumented() {
        Set<CtClass<?>> classes = getTestClasses();
        return classes == null || classes.isEmpty();
    }

    private void instrumentTestClasses() throws MojoExecutionException {
        getLog().info("Instrumenting test classes");
        try {
            Path testSourceOutputPath = getGeneratedTestsPath();
            FileUtils.createEmptyDirectory(testSourceOutputPath);
            setTestClasses(instrumentTestClasses(testSourceOutputPath.toString()));
            FileUtils.copyDirectory(Paths.get(getProject().getBuild().getTestSourceDirectory()), testSourceOutputPath);
        }
        catch(IOException exc) {
            throw new MojoExecutionException("An error occurred while creating the instrumented test output folder or while copying the non-instrumented test classes", exc);
        }
    }

    private Path getGeneratedTestsPath() throws IOException {
        return getPathTo("generated");
    }

    private Set<CtClass<?>> instrumentTestClasses(String folder) {
        MavenLauncher launcher = getLauncherForProject();

        // Adding the observer to the path so it can be compiled at the same time as the project
        launcher.addInputResource(new ResourceJavaFile("utils/StateObserver.java"));
        CtModel model = launcher.buildModel();

        Set<CtClass<?>> testClassesFound = TestClassFinder.findTestClasses(model);

        if(getLog().isDebugEnabled()) {
            getLog().debug("Test classes found: " + testClassesFound.size());
            for(CtClass type : testClassesFound) {
                getLog().debug(type.getQualifiedName());
            }
        }

        launcher.addProcessor( new ObserverClassProcessor(testClassesFound));
        launcher.process();

        getLog().debug("Saving instrumented classes");
        launcher.setSourceOutputDirectory(folder);

        // Not too pretty :(
        CtClass<?> observerClass = (CtClass<?>)model.getRootPackage().getFactory().Type().get(StateObserver.class);
        testClassesFound.add(observerClass);
        launcher.setOutputFilter( testClassesFound::contains);
        launcher.prettyprint();
        testClassesFound.remove(observerClass);

        return testClassesFound;
    }

    private void compileInstrumentedTestClasses() throws MojoExecutionException {

        getLog().info("Compiling instrumented test classes");

        String instrumentedTestsPath = "";
        try {
            instrumentedTestsPath = getGeneratedTestsPath().toString();
        }
        catch(IOException exc) {
            throw new MojoExecutionException("An error occurred while accessing the instrumented tests folder", exc);
        }

        executePlugin("org.apache.maven.plugins",
                "maven-compiler-plugin",
                "3.8.0",
                configuration(
                        element(name("compileSourceRoots"),
                                element("value", instrumentedTestsPath)),
                        element("testSource", "1.8"),
                        element("testTarget", "1.8")
                ), "testCompile");
    }

    private void loadTransformations() throws MojoExecutionException {
        getLog().info("Loading undetected transformations");

        try {
            mutations = loadUndetectedMutations();
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not load transformations from " + transformations.getAbsolutePath(), exc);
        }
    }

    private  List<MutationIdentifier> loadUndetectedMutations() throws IOException {
        Gson gson = new Gson();
        FileReader fileReader = new FileReader(transformations);
        JsonObject document = gson.fromJson(fileReader, JsonObject.class);
        JsonArray mutations = document.getAsJsonArray("mutations");
        List<MutationIdentifier> result = new ArrayList<>(mutations.size()/2);

        mutations.forEach(mutationElement -> {
            JsonObject mutation = mutationElement.getAsJsonObject();
            if(!mutation.getAsJsonPrimitive("status").getAsString().equals("SURVIVED")) {
                return;
            }
            JsonObject method = mutation.getAsJsonObject("method");
            ClassName className = ClassName.fromString(method.getAsJsonPrimitive("package").getAsString() + "." + method.getAsJsonPrimitive("class").getAsString());
            MethodName methodName = MethodName.fromString(method.getAsJsonPrimitive("name").getAsString());
            Location location = new Location(className, methodName, method.getAsJsonPrimitive("description").getAsString());
            MutationIdentifier mID = new MutationIdentifier(location, 0, mutation.getAsJsonPrimitive("mutator").getAsString());
            result.add(mID);
        });

        return result;
    }

    private void observeOriginalValues() throws MojoExecutionException {
        getLog().info("Executing test classes to observe original values");

        try {
            executeTests(getPathTo("observations", "tests", "original"), getTestsToExecute(mutations));
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Could not create directory to store original observations", exc);
        }
    }

    private void executeTests(Path resultFolder, Set<String> classes)  throws MojoExecutionException {

        for(int iteration = 0; iteration < getTestTimes(); iteration++) {
            getLog().info("Executing tests");
            String testsToRun = classes.stream().collect(Collectors.joining(", "));
            getLog().debug("Test classes to execute: " + testsToRun);

            try {
                Path executionOutputFolder = resultFolder.resolve(Long.toString(System.currentTimeMillis()));

                setUserPropertyInSession("stamp.reneri.folder", executionOutputFolder.toAbsolutePath().toString());
                executePlugin("org.apache.maven.plugins",
                        "maven-surefire-plugin",
                        "2.22.1",
                        configuration(
                                element("reportsDirectory", executionOutputFolder.resolve("surefire-reports").toAbsolutePath().toString()),
                                element("redirectTestOutputToFile", "true"),
                                element("test", testsToRun)
                        ),
                        "test"
                );
            } catch (MojoExecutionException exc) {
                getLog().warn("Issues while executing the tests ", exc);
            }
        }

    }

    private void observeTransformations() throws MojoExecutionException {
        getLog().info("Observing transformation effects");

        try {
            List<MutationIdentifier> mutations = loadUndetectedMutations();

            getLog().debug(String.format("Found %d undetected transformations", mutations.size()));

            for (int index = 0; index < mutations.size(); index++) {

                getLog().debug("Executing transformation " + index);
                try {
                    executeMutation(getPathTo("observations", "tests", Integer.toString(index)), mutations.get(index));
                }
                catch (Exception exc) {
                    getLog().error("Error executing transformation " + index, exc);
                }
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not read mutation file", exc);
        }
    }



    private void executeMutation(Path resultFolderPath, MutationIdentifier mutation) throws IOException, MojoExecutionException {
        // Mutate the source code
        Path pathToClass = getClassFilePath(mutation);
        byte[] originalClass = Files.readAllBytes(pathToClass);
        FileUtils.write(pathToClass, mutate(originalClass, mutation));
        //Select the tests to execute
        Set<String> tests = getTestsToExecute(mutation);
        //Save the information
        saveMutationInfo(resultFolderPath, mutation, tests);
        // Execute the tests
        executeTests(resultFolderPath, tests);
        //Restore the original code
        FileUtils.write(pathToClass, originalClass);
    }

    private byte[] mutate(byte[] originalClass, MutationIdentifier mutation) {
        ClassReader classReader = new ClassReader(originalClass);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        MutationClassAdapter adapter = new MutationClassAdapter(mutation, classWriter);
        classReader.accept(adapter, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private void saveMutationInfo(Path folder, MutationIdentifier mutation, Set<String> tests) throws IOException {
        Location location = mutation.getLocation();
        JsonObject obj = new JsonObject();
        obj.addProperty("mutator", mutation.getMutator());
        obj.addProperty("class", location.getClassName().getNameWithoutPackage().toString());
        obj.addProperty("package", location.getClassName().getPackage().toString());
        obj.addProperty("method", location.getMethodName().toString());
        obj.addProperty("description", location.getMethodDesc());

        if(!tests.isEmpty()) {
            JsonArray testArray = new JsonArray();
            tests.forEach(testArray::add);
            obj.add("tests", testArray);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try(FileWriter writer = new FileWriter(folder.resolve("mutation.json").toFile())) {
            gson.toJson(obj, writer);
        }
    }
}
