package eu.stamp_project.reneri;

import eu.stamp_project.reneri.utils.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;

@Mojo(name = "methodDiff")
public class MethodDiffMojo extends AbstractDiffMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            for (File methodDirectory : FileUtils.getChildrenDirectories(getPathTo("observations", "methods"))) {
                generateDiffReportFor(methodDirectory.toPath());
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not read method observations", exc);
        }
    }
}
