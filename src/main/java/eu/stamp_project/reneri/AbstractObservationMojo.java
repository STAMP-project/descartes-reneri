package eu.stamp_project.reneri;

import com.google.gson.Gson;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.pitest.mutationtest.engine.MutationIdentifier;
import spoon.MavenLauncher;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;

public abstract class AbstractObservationMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    private MavenPluginManager mavenPluginManager;

    @Component
    private PluginVersionResolver versionResolver;

    @Parameter(property = "ouputFolder", defaultValue = "${project.build.directory}/reneri")
    private File outputFolder;

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Parameter(property = "testTimes", defaultValue = "10")
    private int testTimes; //Number of times the tests should be executed

    public int getTestTimes() {
        return testTimes;
    }

    public void setTestTimes(int testTimes) {
        this.testTimes = testTimes;
    }


    @Parameter(property = "stackTraces", defaultValue = "${project.build.directory}/stack-traces.json")
    private File stackTracesFile;

    public File getStackTracesFile() {
        return stackTracesFile;
    }

    public void setStackTracesFile(File stackTraces) {
        this.stackTracesFile = stackTraces;
    }

    @Parameter(property = "excludedTests")
    private Set<String> excludedTests;

    public Set<String> getExcludedTests() {
        if(excludedTests == null){
            return Collections.emptySet();
        }
        return excludedTests;
    }

    public void setExcludedTests(Set<String> excludedTests) {
        if(excludedTests == null) {
            excludedTests = Collections.emptySet();
        }
        this.excludedTests = excludedTests;
    }

    private MavenPluginResolver pluginResolver;

    protected MavenPluginResolver getPluginResolver() {
        if(pluginResolver == null) {
            pluginResolver = new MavenPluginResolver(project, mavenSession, versionResolver);
        }
        return pluginResolver;
    }

    private MethodTracesEntry[] methodStackTraces;

    protected MethodTracesEntry[] getMethodStackTraces() throws MojoExecutionException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(stackTracesFile)) {
            methodStackTraces = gson.fromJson(reader, MethodTracesEntry[].class);
            return methodStackTraces;
        } catch (Exception exc) {
            throw new MojoExecutionException("An error occurred while loading the stack traces file.", exc);
        }
    }

    protected void setUserPropertyInSession(String property, String value) {
        mavenSession.getUserProperties().setProperty(property, value);
    }

    protected void executePlugin(String groupId, String artifactId, String version, Xpp3Dom configuration, String goalToExecute) throws MojoExecutionException {
        try {
            Plugin plugin = getPluginResolver().resolve(groupId, artifactId, version, configuration);
            executeMojo(plugin, goal(goalToExecute),
                    (Xpp3Dom) plugin.getConfiguration(),
                    executionEnvironment(project, mavenSession, pluginManager));
        } catch (PluginVersionResolutionException exc) {
            throw new MojoExecutionException(String.format("Could not find %s:%s:%s", groupId, artifactId, version), exc);
        }
    }

    protected Path getPathTo(String directory, String... more) throws IOException {
        Path folderPath = outputFolder.toPath().resolve(Paths.get(directory, more)).toAbsolutePath();
        Files.createDirectories(folderPath);

        return  folderPath;
    }

    private Set<CtClass<?>> testClasses;

    private InheritanceGraph graph;

    protected void setTestClasses(Set<CtClass<?>> testClasses) {
        this.testClasses = testClasses;
        graph = (testClasses == null)? null : new InheritanceGraph(testClasses);
    }

    protected Set<CtClass<?>> getTestClasses() {
        return testClasses;
    }

    protected Set<String> getInheritanceClousure(Set<String> classes) {
        return graph.getInheritanceClousure(classes);
    }

    protected Set<String> getInvolvedTestsFor(MutationIdentifier mutation) throws MojoExecutionException {
        HashSet<String> result = new HashSet<>();
        for(MethodTracesEntry entry : getMethodStackTraces()) {
            Set<String> closestTests = entry.getClosestClassesTo(mutation, testClasses);
            result.addAll(getInheritanceClousure(closestTests)); // Include derived classes
        }
        return result;
    }

    protected Set<String> getTestsToExecute(Collection<MutationIdentifier> mutations) throws MojoExecutionException {
        HashSet<String> result = new HashSet<>();
        for(MutationIdentifier id : mutations) {
            result.addAll(getInvolvedTestsFor(id));
        }
        result.removeAll(getExcludedTests());
        return result;
    }

    protected Set<String> getTestsToExecute(MutationIdentifier mutation) throws MojoExecutionException {
        return getTestsToExecute(Collections.singleton(mutation));
    }

    protected Path getClassFilePath(MutationIdentifier mutation) {
        return  Paths.get(project.getBuild().getOutputDirectory(),  mutation.getClassName().asInternalName() + ".class");
    }

    protected MavenLauncher getLauncherForProject() {
        return new MavenLauncher(project.getBasedir().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
    }

}
