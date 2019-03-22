package eu.stamp_project.reneri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.stamp_project.reneri.diff.BagOfValues;
import eu.stamp_project.reneri.diff.DiffOnValues;
import eu.stamp_project.reneri.diff.ObservedValueMap;
import eu.stamp_project.reneri.observations.Observation;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.stamp_project.reneri.utils.ExceptionUtils.propagate;
import static eu.stamp_project.reneri.utils.FileUtils.getChildrenDirectories;

public abstract class AbstractDiffMojo extends ReneriMojo {


    protected final static String OBSERVATIONS_FILENAME = "observations.jsonl";
    protected final static String DIFF_FILENAME = "diff.json";


    protected ObservedValueMap loadOriginalObservations(Path directory) throws MojoExecutionException {
//
//        BagOfValues result = new BagOfValues();
//        getObservationsIn(directory.resolve("original"))
//                .forEach(result::add);
//        return result;

        ObservedValueMap map = new ObservedValueMap();
        getObservationsIn(directory.resolve("original")).forEach(map::put);
        return map;
    }

    protected void computeDiffOnFolder(Path testObservationDirectory, ObservedValueMap originalValues) throws MojoExecutionException {

        DiffOnValues diffBuilder = new DiffOnValues(originalValues);
        getObservationsIn(testObservationDirectory).forEach(diffBuilder::add);

        if (diffBuilder.hasDiff()) {
            saveDiff(diffBuilder, testObservationDirectory);
        }
    }

    private void saveDiff(DiffOnValues diff, Path directory) throws MojoExecutionException {

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(directory.resolve(DIFF_FILENAME).toFile());
            gson.toJson(diff.getDiff(), writer);
            writer.close();
        } catch (IOException exc) {
            throw new MojoExecutionException("Could not save diff file in " + directory.toString(), exc);
        }

    }

    private Stream<Observation> getObservationsIn(Path directory) throws MojoExecutionException {

        try {
            return getObservationFiles(directory)
                    .flatMap((file) -> propagate(() -> Files.lines(file))) // throws IOException
                    .map((line) -> propagate(() -> Observation.fromString(line))) // throws InvalidObservationException
                    ;
        } catch (RuntimeException exc) {
            throw new MojoExecutionException("Unexpected error reading observations from " + directory.toString(), exc.getCause());
        }
    }

    private Stream<Path> getObservationFiles(Path rootFolder) {
        return Arrays.stream(getChildrenDirectories(rootFolder))
                .map(dir -> dir.toPath().resolve(OBSERVATIONS_FILENAME))
                .filter(path -> {
                    File file = path.toFile();
                    return file.exists() && file.canRead();
                });//.collect(Collectors.toList());
    }

    protected void generateAllDiffReportFor(Path directory) throws MojoExecutionException {

//        BagOfValues originalValues = loadOriginalObservations(directory);

        ObservedValueMap originalValues = loadOriginalObservations(directory);


        if (originalValues.isEmpty()) {
            getLog().warn("Directory " + directory.toString() + " contained no original observations");
            return;
        }

        for (File childDir : getChildrenDirectories(directory)) {
            if (childDir.getName().equals("original")) {
                continue;
            }
            getLog().info("Computing diff for " + childDir.getName());
            computeDiffOnFolder(childDir.toPath(), originalValues);
        }
    }

}
