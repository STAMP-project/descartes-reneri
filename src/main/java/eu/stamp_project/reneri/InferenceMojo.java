//package eu.stamp_project.reneri;
//
//import com.google.gson.*;
//import eu.stamp_project.reneri.inference.*;
//import eu.stamp_project.reneri.instrumentation.PointcutLocator;
//import eu.stamp_project.reneri.datastructures.Trie;
//import eu.stamp_project.reneri.observations.InvalidObservationFileException;
//import eu.stamp_project.reneri.observations.ObservationCollection;
//import eu.stamp_project.reneri.observations.PointObservationCollection;
//import org.apache.maven.plugin.AbstractMojo;
//import org.apache.maven.plugin.MojoExecutionException;
//import org.apache.maven.plugin.MojoFailureException;
//import org.apache.maven.plugins.annotations.Mojo;
//import org.apache.maven.plugins.annotations.Parameter;
//import org.apache.maven.project.MavenProject;
//import spoon.MavenLauncher;
//import spoon.reflect.cu.SourcePosition;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//
////TODO: Refactor with the other Mojo
////TODO: Check that the files are in position
//@Mojo(name = "infer")
//public class InferenceMojo extends AbstractMojo {
//
//    @Parameter(defaultValue = "${project}")
//    private MavenProject project;
//
//    public MavenProject getProject() {
//        return project;
//    }
//
//    public void setProject(MavenProject project) {
//        this.project = project;
//    }
//
//    @Parameter(property = "ouputFolder", defaultValue = "${project.build.directory}/reneri")
//    private File outputFolder;
//
//
//    private Trie<SourcePosition> pointCutLocations;
//
//    protected void locatePointCuts() {
//        getLog().info("Locating pointcuts in the test code");
//        MavenLauncher launcher = new MavenLauncher(project.getBasedir().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.TEST_SOURCE);
//        PointcutLocator processor = new PointcutLocator();
//        launcher.buildModel();
//        launcher.addProcessor(processor);
//        launcher.process();
//        pointCutLocations = processor.getLocations();
//    }
//
//    @Override
//    public void execute() throws MojoExecutionException, MojoFailureException {
//        try {
//
//            locatePointCuts();
//
//
//            Path observationPath = outputFolder.toPath().resolve(Paths.get("observations", "tests"));
//            ObservationCollection originalObservations = new ObservationCollection(observationPath.resolve("original"));
//
//            Gson gson = new GsonBuilder()
//                    // TODO: Serializers out of their classes
//                    .registerTypeAdapter(ConditionMismatch.class, ConditionMismatch.getSerializer())
//                    .registerTypeAdapter(Condition.class, new ConditionSerializer())
//                    .setPrettyPrinting()
//                    .create();
//
//            for (File file : observationPath.toFile().listFiles()) {
//                if (!file.isDirectory() || file.getName().equals("original")) {
//                    continue;
//                }
//                getLog().info("Checking folder: " + file.getName());
//                List<ConditionMismatch> mismatches = inferAndMatch(originalObservations, new ObservationCollection(file.toPath()));
//
//                try {
//                    //TODO: Parameter for the file name?
//                    FileWriter writer = new FileWriter(file.toPath().resolve("hints.json").toFile());
//                    gson.toJson(mismatches, writer);
//                    writer.close();
//                }
//                catch (IOException exc) {
//                    throw new MojoExecutionException("Could not create file to write the hints", exc);
//                }
//
//            }
//        }
//        catch (InvalidObservationFileException exc) {
//            throw new MojoExecutionException("Invalid observation file " + exc.getFileName(), exc);
//        }
//    }
//
//    protected List<ConditionMismatch> inferAndMatch(ObservationCollection original, ObservationCollection mutated) {
//
//        List<ConditionMismatch> mismatches = new ArrayList<>();
//
//        Inferrer inferrer = getInferrer();
//        for(String point :  mutated.getObservedPoints()) {
//            if(!original.has(point)) {
//                getLog().info(String.format("Pointcut %s was observed in %s but not in the original execution", point, mutated.getFolderPath()));
//                continue;
//            }
//            //TODO: Possible optimization, keep a cached list of conditions already inferred
//            PointObservationCollection originalPoint = original.get(point);
//            PointObservationCollection mutatedPoint = mutated.get(point);
//
//            for(Condition condition : inferrer.infer(originalPoint)) {
//                if(!condition.appliesTo(mutatedPoint)) {
//                    mismatches.add(new ConditionMismatch(point, condition, pointCutLocations.getClosestMatch(point)));
//                    getLog().info(String.format("Condition %s does not apply to mutated point %s", condition.getClass().getTypeName(), point));
//                }
//            }
//        }
//        return mismatches;
//    }
//
//    public Inferrer getInferrer() {
//        return new CompoundInferrer(
//                new DirectConditionInferrer(),
//                new ExactValuesConditionInferrer());
//    }
//}
