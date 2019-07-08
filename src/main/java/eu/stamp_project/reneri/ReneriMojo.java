package eu.stamp_project.reneri;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ReneriMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    protected Build getProjectBuild() { return project.getBuild(); }

    protected String getAbsolutePathToProject() { return project.getBasedir().getAbsolutePath(); }

    @Parameter(property = "ouputFolder", defaultValue = "${project.build.directory}/reneri")
    private File outputFolder;

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    protected Path getPathTo(String directory, String... more) throws IOException {
        Path folderPath = outputFolder.toPath().resolve(Paths.get(directory, more)).toAbsolutePath();
        Files.createDirectories(folderPath);

        return  folderPath;
    }

    //TODO: Need to represent locations as a Java class see comment below
    protected MutationInfo loadMutationFromDir(Gson gson, Path directory) throws MojoExecutionException {
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


}
