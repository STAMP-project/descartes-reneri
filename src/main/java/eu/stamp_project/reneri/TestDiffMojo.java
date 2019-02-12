package eu.stamp_project.reneri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.stamp_project.reneri.diff.BagOfValues;
import eu.stamp_project.reneri.diff.DiffOnValues;
import eu.stamp_project.reneri.observations.Observation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static eu.stamp_project.reneri.utils.ExceptionUtils.propagate;
import static eu.stamp_project.reneri.utils.FileUtils.getChildrenDirectories;


@Mojo(name = "testsDiff")
public class TestDiffMojo extends ReneriMojo {

    private final static String OBSERVATIONS_FILENAME = "observations.jsonl";
    private final static String DIFF_FILENAME = "diff.json";


    private BagOfValues originalValues;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        loadOriginalObservations();

        if (noOriginalObservations()) {
            getLog().warn("No original observations found");
            return;
        }

        computeDiffForTests();
    }

    private void loadOriginalObservations() throws MojoExecutionException {
        originalValues = new BagOfValues();
        try {
            getLog().info("Reading original observations");
            getObservationsIn(getPathTo("observations", "tests", "original"))
                    .forEach(originalValues::add);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not find or read the original observed value logs", exc);
        }
    }

    private boolean noOriginalObservations() {
        return originalValues == null || originalValues.isEmpty();
    }

    private void computeDiffForTests() throws MojoExecutionException {

        try {
            Path root = getPathTo("observations", "tests");
            for (File directory : getChildrenDirectories(root)) {
                if ( directory.getName().equals("original")) {
                    continue;
                }
                getLog().info("Computing diff for " + directory.getName());
                computeDiffOnFolder(directory.toPath());
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not find or read the test observation directory", exc);
        }
    }

    private void computeDiffOnFolder(Path testObservationDirectory) throws MojoExecutionException {

        DiffOnValues diffBuilder = new DiffOnValues(originalValues);
        getObservationsIn(testObservationDirectory).forEach(diffBuilder::add);

        if(diffBuilder.hasDiff()) {
            saveDiff(diffBuilder, testObservationDirectory);
        }
    }

    private void saveDiff(DiffOnValues diff, Path directory) throws MojoExecutionException {

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(directory.resolve(DIFF_FILENAME).toFile());
            gson.toJson(diff.getDiff(), writer);
            writer.close();
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not save diff file in " + directory.toString(), exc);
        }

    }

    private Stream<Observation> getObservationsIn(Path directory) throws MojoExecutionException {

        try {
            return getObservationFiles(directory)
                    .flatMap((file) -> propagate(() -> Files.lines(file))) // throws IOException
                    .map((line) -> propagate(() -> Observation.fromString(line))) // throws InvalidObservationException
                    ;
        }
        catch(RuntimeException exc) {
            throw new MojoExecutionException("Unexpected error reading observations from " + directory.toString(), exc.getCause());
        }
    }

    private Stream<Path> getObservationFiles(Path rootFolder) {
        return Arrays.stream(getChildrenDirectories(rootFolder))
                .map(dir -> dir.toPath().resolve(OBSERVATIONS_FILENAME))
                .filter(path -> {
                    File file = path.toFile();
                    return file.exists() && file.canRead();
                });
    }

}
