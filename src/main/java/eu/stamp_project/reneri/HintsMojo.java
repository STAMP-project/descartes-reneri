package eu.stamp_project.reneri;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import eu.stamp_project.reneri.codeanalysis.EntryPointFinder;
import eu.stamp_project.reneri.datastructures.Trie;
import eu.stamp_project.reneri.utils.FileUtils;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


@Mojo(name = "hints", requiresDependencyResolution = ResolutionScope.TEST)
public class HintsMojo extends ReneriMojo {
    // Requires the dependency resolution for test to get the full classpath

    private Gson gson = new GsonBuilder().create();

    private EntryPointFinder finder;

    private Set<MutationInfo> observedMutations = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createEntryPointFinder();

        generateHintsForObservedTests();

        generateHintsForObservedMethods();
    }

    private void createEntryPointFinder() throws MojoExecutionException {
        try {
            ClassPool classPool = new ClassPool();
            for (String element : getProject().getTestClasspathElements()) {
                classPool.appendClassPath(element);
            }
            finder = new EntryPointFinder(classPool);

        }
        catch (DependencyResolutionRequiredException | NotFoundException exc) {
            throw new MojoExecutionException("Could not load classpath elements", exc);
        }
    }

    private Trie<JsonObject> loadLocations() throws MojoExecutionException {

        Trie<JsonObject> locations = new Trie<>();

        try {
            JsonReader reader = new JsonReader(new FileReader(getPathTo("observations", "tests").resolve("locations.json").toFile()));
            reader.beginArray();
            while(reader.hasNext()) {
                JsonObject item = gson.fromJson(reader, JsonObject.class);
                locations.add(item.getAsJsonPrimitive("point").getAsString(), item);
            }
            reader.endArray();
            return locations;
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not load pointcut locations", exc);
        }
    }

    //TODO: Need to represent locations as a Java class see comment below
    private MutationInfo loadMutationFromDir(Path directory) throws MojoExecutionException {
        Path pathToMutationDetails = directory.resolve("mutation.json");
        try(FileReader reader = new FileReader(pathToMutationDetails.toFile())) {
            //TODO: Consider a GSON type adapted and a unified way to present things
            JsonObject mutationInfo = gson.fromJson(reader, JsonObject.class);
            return new MutationInfo(
                    mutationInfo.getAsJsonPrimitive("mutator").getAsString(),
                    mutationInfo.getAsJsonPrimitive("class").getAsString(),
                    mutationInfo.getAsJsonPrimitive("package").getAsString(),
                    mutationInfo.getAsJsonPrimitive("method").getAsString(),
                    mutationInfo.getAsJsonPrimitive("description").getAsString());
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not read mutation details from " + directory.toString(), exc);
        }
    }

    // TODO: Using the JsonOject class as an intermediate representation, in fact it would good to have a class representing the concept and using observation objects, again, a GSON type adaptor is needed
    private Collection<JsonObject> getMeaningfulDifferencesFromDir(Path directory) throws MojoExecutionException {
        Path pathToDiff = directory.resolve("diff.json");
        if(!Files.exists(pathToDiff)) {
            return Collections.emptyList();
        }

        try(FileReader reader = new FileReader(pathToDiff.toFile())) {

            JsonArray diff = gson.fromJson(reader, JsonArray.class);
            ArrayList<JsonObject> result = new ArrayList<>(diff.size());

            for (JsonElement element : diff) {
                JsonObject difference = element.getAsJsonObject();
                JsonArray expectedValues = difference.getAsJsonArray("expected");
                if(expectedValues.size() != 1) {
                    // 0 or many
                    continue;
                }
                JsonObject obj = new JsonObject();
                obj.addProperty("pointcut", difference.getAsJsonPrimitive("pointcut").getAsString());
                obj.add("expected", expectedValues.get(0));
                result.add(obj);
            }
            return result;
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Could not read diff file from " + directory.toString(), exc);
        }
    }

    //TODO: Consider using a disconnected database to handle all the values being generated
    private String getFieldNameFromPointCut(String pointcut) {
        // class | test method | expression index | variable | size or length
        String[] fragments = pointcut.split("\\|");
        if(fragments.length > 3) {
            return fragments[3];
        }
        return null;
    }

    private JsonArray reportAccessors(String field, String type) {
        try {
            return toJsonArray(finder.findAccessors(field, type));
        }
        catch (NotFoundException exc) {

            getLog().warn("Could not find accessors for field: " + field + " " + type);
            getLog().error(exc);
            return new JsonArray();
        }
    }

    private JsonArray toJsonArray(Collection<CtMethod> methods) {

        JsonArray array = new JsonArray();

        for (CtMethod method : methods) {

            JsonObject accessor = new JsonObject();

            accessor.addProperty("method", method.getName());
            accessor.addProperty("desc", method.getSignature());
            accessor.addProperty("class", method.getDeclaringClass().getName());

            array.add(accessor);
        }

        return array;
    }

    private JsonArray reportEntryPoints(String className, String method, String desc) {
        try {
            return toJsonArray(finder.findEntryPointsFor(className, method, desc));
        }
        catch (NotFoundException  exc) {
            getLog().warn("Could not find entry points for " + method + desc + " declared in " + className);
            getLog().error(exc);
            return new JsonArray();
        }


    }

    private void writeHints(Path directory, JsonElement hints) throws MojoExecutionException {

        try(FileWriter writer = new FileWriter(directory.resolve("hints.json").toFile())) {
            gson.toJson(hints, writer);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not write hints for " + directory.toString(), exc);
        }
    }

    private void generateHintsForObservedTests () throws MojoExecutionException {

        Path testObservations;
        try {
            testObservations = getPathTo("observations", "tests");
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not read test observation folders", exc);
        }

        Trie<JsonObject> pointcutLocations = loadLocations();

        for (File directory : FileUtils.getChildrenDirectories(testObservations)) {
            Path directoryPath = directory.toPath();
            Collection<JsonObject> reportedDifferences = getMeaningfulDifferencesFromDir(directoryPath);

            if(reportedDifferences.isEmpty()) {
                continue;
            }

            MutationInfo mutation = loadMutationFromDir(directoryPath);

            // Set the mutation as observed
            observedMutations.add(mutation);

            //TODO: Once again, we need a representation for this
            JsonArray hints = new JsonArray();
            for (JsonObject difference : reportedDifferences) {
                String pointcut = difference.get("pointcut").getAsString();

                JsonObject hint = new JsonObject();
                hint.addProperty("pointcut", pointcut);
                hint.addProperty("hint-type", "observation");

                hints.add(hint);

                JsonObject location = pointcutLocations.getClosestMatch(pointcut);
                hint.add("location", location);

                if (location != null) {
                    String field = getFieldNameFromPointCut(pointcut);
                    if(field != null) {
                        hint.add("accessors", reportAccessors(field, location.getAsJsonPrimitive("type").getAsString()));
                    }
                }
            }
            if(hints.size() != 0) {
                writeHints(directoryPath, hints);
            }
        }

    }

    private void generateHintsForObservedMethods() throws MojoExecutionException {

        Path methodObservationsRoot;
        try {
            methodObservationsRoot = getPathTo("observations", "methods");
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not read method observation folders", exc);
        }

        for (File methodDir : FileUtils.getChildrenDirectories(methodObservationsRoot)) {

            // Load entry points

            JsonArray entryPoints = null;

            for(File mutationDir : FileUtils.getChildrenDirectories(methodDir)) {

                Path mutationDirPath = mutationDir.toPath();
                MutationInfo info = loadMutationFromDir(mutationDirPath);

                if(observedMutations.contains(info)) {
                    continue; // Don't generate any hint for this mutation
                }

                JsonObject hint = new JsonObject();
                hint.addProperty("hint-type", getMeaningfulDifferencesFromDir(mutationDir.toPath()).isEmpty()?"execution":"infection");

                if(entryPoints == null) {
                    // The actual hint in the case of infection, would be the difference between the observed methods and the static methods
                    // but that is debatable. Since the implementation in Java is painful, I leave that to the human readable report generation.
                    entryPoints = reportEntryPoints(info.getClassQualifiedName(), info.getMethodName(), info.getMethodDescription());
                }

                hint.add("entry-points", entryPoints);
                writeHints(mutationDirPath, hint);
            }
        }
    }
}
