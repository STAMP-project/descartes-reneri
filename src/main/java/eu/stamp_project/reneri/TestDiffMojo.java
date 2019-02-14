package eu.stamp_project.reneri;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;


@Mojo(name = "testDiff")
public class TestDiffMojo extends AbstractDiffMojo {


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            generateDiffReportFor(getPathTo("observations", "tests"));
        } catch (IOException exc) {
            throw new MojoExecutionException("Could not read test observation files", exc);

        }
    }
}
