package eu.stamp_project.reneri;

import eu.stamp_project.reneri.inference.ComposedInferrer;
import eu.stamp_project.reneri.inference.Condition;
import eu.stamp_project.reneri.inference.DirectConditionInferrer;
import eu.stamp_project.reneri.inference.Inferrer;
import eu.stamp_project.reneri.observations.InvalidObservationFileException;
import eu.stamp_project.reneri.observations.ObservationCollection;
import eu.stamp_project.reneri.observations.PointObservationCollection;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Set;

//TODO: Refactor with the other Mojo
//TODO: Check that the files are in position
//TODO: Locate the pointcuts in the source code
@Mojo(name = "infer")
public class InferenceMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    @Parameter(property = "ouputFolder", defaultValue = "${project.build.directory}/reneri")
    private File outputFolder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            ObservationCollection originalObservations = new ObservationCollection(outputFolder.toPath().resolve("original"));
            for (File file : outputFolder.listFiles()) {
                if (!file.isDirectory() || file.getName().equals("original")) {
                    continue;
                }
                getLog().info("Checking folder: " + file.getName());
                infereAndMatch(originalObservations, new ObservationCollection(file.toPath()));
            }
        }
        catch (InvalidObservationFileException exc) {
            throw new MojoExecutionException("Invalid observation file " + exc.getFileName(), exc);
        }
    }

    protected void infereAndMatch(ObservationCollection original, ObservationCollection mutated) {
        Inferrer inferrer = getInferrer();
        for(String point :  mutated.getObservedPoints()) {
            if(!original.has(point)) {
                getLog().info(String.format("Pointcut %s was observed in %s but not in the original execution", point, mutated.getFolderPath()));
                continue;
            }
            //TODO: Possible optimization, keep a cached list of conditions already inferred
            PointObservationCollection originalPoint = original.get(point);
            PointObservationCollection mutatedPoint = mutated.get(point);

            for(Condition condition : inferrer.infer(originalPoint)) {
                if(!condition.appliesTo(mutatedPoint)) {
                    getLog().info(String.format("Condition %s does not apply to mutated point %s", condition.getClass().getTypeName(), point));
                }
            }
        }
    }

    public Inferrer getInferrer() {
        return new DirectConditionInferrer();
    }
}
