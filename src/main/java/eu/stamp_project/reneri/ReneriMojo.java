package eu.stamp_project.reneri;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
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


}
