package eu.stamp_project.reneri;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.stamp_project.mutationtest.descartes.codegeneration.MutationClassAdapter;
import eu.stamp_project.reneri.instrumentation.ObserverClassProcessor;
import eu.stamp_project.reneri.instrumentation.StateObserver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassWriter;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "diff", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class DiffExecutionMojo extends AbstractMojo {

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

    @Parameter(property = "transformations", defaultValue = "${project.build.directory/mutations.json}")
    private File transformations;

    public File getTransformations() {
        return transformations;
    }

    public void setTransformations(File transformations) {
        this.transformations = transformations;
    }

    @Parameter(property = "testTimes", defaultValue = "10")
    private int testTimes; //Number of times the tests should be executed

    public int getTestTimes() {
        return testTimes;
    }

    public void setTestTimes(int testTimes) {
        this.testTimes = testTimes;
    }

    @Parameter(property = "executeAllTests", defaultValue = "false")
    private boolean executeAllTests;

    public boolean getExecuteAllTests() {
        return executeAllTests;
    }

    public void setExecuteAllTests(boolean executeAllTests) {
        this.executeAllTests = executeAllTests;
    }

    @Parameter(property = "stackTraces", defaultValue = "${project.build.directory}/stack-traces.json")
    private File stackTracesFile;

    public File getStackTracesFile() {
        return stackTracesFile;
    }

    public void setStackTracesFile(File stackTraces) {
        this.stackTracesFile = stackTraces;
    }


    private Set<CtClass<?>> instrumentedTestClasses;

    private InheritanceGraph graph;

    private MethodTracesEntry[] methodStackTraces;

    private MavenPluginResolver pluginResolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            setUp();

            cleanEnvironment();

            instrumentTestClasses();

            if(noClassWasInstrumented()) {
                getLog().info("Stopping analysis as no test class was found or instrumented");
                return;
            }

            compileInstrumentedTestClasses();

            loadStackTraces();

            observeOriginalValues();

            observeTransformations();
        }
        catch (Throwable exc) {
            throw new MojoExecutionException("Process failed", exc);
        }
    }

    private void cleanEnvironment() throws MojoExecutionException {
        getLog().info("Cleaning previous results");
        try {
            FileUtils.createEmptyDirectory(outputFolder.toPath());
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Clould not clean output folder", exc);
        }
    }

    private void setUp() {
        pluginResolver = new MavenPluginResolver(project, mavenSession, versionResolver);
    }

    private boolean noClassWasInstrumented() {
        return instrumentedTestClasses == null || instrumentedTestClasses.isEmpty();
    }

    private void instrumentTestClasses() throws MojoExecutionException {

        getLog().info("Instrumenting test classes");

        Path testSourceOutputPath = getGeneratedTestsPath();
        try {
            FileUtils.createEmptyDirectory(testSourceOutputPath);
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Could not create folder for instrumented tests", exc);
        }

        instrumentedTestClasses = instrumentTestClasses(testSourceOutputPath.toString());

        try {
            FileUtils.copyDirectory(Paths.get(project.getBuild().getTestSourceDirectory()), testSourceOutputPath);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("An error occurred while copying original test sources", exc);
        }
    }


    private Path getGeneratedTestsPath() {
        return Paths.get(project.getBuild().getTestOutputDirectory()).getParent().resolve("reneri-generated").toAbsolutePath();
    }

    private Set<CtClass<?>> instrumentTestClasses(String folder) {
        MavenLauncher launcher = new MavenLauncher(project.getBasedir().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);

        // Adding the observer to the path so it can be compiled at the same time as the project
        launcher.addInputResource(new ResourceJavaFile("utils/StateObserver.java"));
        CtModel model = launcher.buildModel();

        Set<CtClass<?>> testClassesFound = TestClassFinder.findTestClasses(model);

        graph = new InheritanceGraph(testClassesFound);

        getLog().debug("Test classes found: " + testClassesFound.size());

        if(getLog().isDebugEnabled()) {
            for(CtClass type : testClassesFound) {
                getLog().debug(type.getQualifiedName());
            }
        }

        launcher.addProcessor( new ObserverClassProcessor(testClassesFound));
        launcher.process();

        getLog().debug("Saving instrumented classes");

        launcher.setSourceOutputDirectory(folder);

        CtClass<?> observerClass = (CtClass<?>)model.getRootPackage().getFactory().Type().get(StateObserver.class);

        testClassesFound.add(observerClass);

        launcher.setOutputFilter(testClassesFound::contains);
        launcher.prettyprint();

        testClassesFound.remove(observerClass);

        return testClassesFound;
    }

    private List<String> getTestClasspathElements() {
        try {
            return project.getTestClasspathElements();
        }
        catch(DependencyResolutionRequiredException exc) {
            getLog().warn("Could not load all dependencies", exc);
            return Collections.singletonList(project.getBuild().getOutputDirectory());
        }
    }

    private void compileInstrumentedTestClasses() throws MojoExecutionException {

        getLog().info("Compiling instrumented test classes");


        try {
            Plugin compiler = pluginResolver.resolve(
                    "org.apache.maven.plugins",
                    "maven-compiler-plugin",
                    "3.8.0",
                    configuration(
                            element(name("compileSourceRoots"),
                                    element("value", getGeneratedTestsPath().toString())),
                            element("testSource", "1.8"),
                            element("testTarget", "1.8")
                    )
            );
            executeMojo(
                    compiler,
                    goal("testCompile"),
                    (Xpp3Dom) compiler.getConfiguration(),
                    executionEnvironment(project, mavenSession, pluginManager)
            );
        }
        catch(PluginVersionResolutionException exc) {
            throw new MojoExecutionException("Could not resolve org.apache.maven.plugins:maven-compiler-plugin:3.8.0", exc);
        }
    }

    private void loadStackTraces() throws MojoExecutionException {

        if(executeAllTests) {
            getLog().debug("All tests are going to be executed against each transformation");
            methodStackTraces = new MethodTracesEntry[0];
            return;
        }

        Gson gson = new Gson();
        try(FileReader reader = new FileReader(stackTracesFile)) {
            methodStackTraces = gson.fromJson(reader, MethodTracesEntry[].class);

        }
        catch (Exception exc) {
            throw new MojoExecutionException("An error occurred while loading the stack traces file.", exc);
        }

    }

    private void observeOriginalValues() throws MojoExecutionException {
        getLog().info("Executing test classes to observe original values");

        try {
            //TODO: Transformations could be loaded before and determine the tests that should be executed
            executeTests(getFolderPath("original"));
        }
        catch(IOException exc) {
            throw new MojoExecutionException("Could not create directory to store original observations", exc);
        }
    }

    private Path getFolderPath(String name) throws IOException {
        Path folderPath = outputFolder.toPath().resolve(name).toAbsolutePath();
        Files.createDirectories(folderPath);
        return folderPath;
    }

    private void executeTests(Path resultFolder) throws MojoExecutionException {
        executeTests(resultFolder, Collections.emptySet());
    }

    private void executeTests(Path resultFolder, Set<String> classes)  throws MojoExecutionException {

        for(int iteration = 0; iteration < testTimes; iteration++) {
            getLog().info("Executing tests");
            String testsToRun = classes.stream().collect(Collectors.joining(", "));
            getLog().debug("Test classes to execute: " + testsToRun);

            try {
                Path executionOutputFolder = resultFolder.resolve(Long.toString(System.currentTimeMillis()));

                mavenSession.getUserProperties().setProperty("stamp.reneri.folder", executionOutputFolder.toAbsolutePath().toString());

                Plugin surefire = pluginResolver.resolve(
                        "org.apache.maven.plugins",
                        "maven-surefire-plugin",
                        "2.22.1",
                        configuration(
                                element("reportsDirectory", executionOutputFolder.resolve("surefire-reports").toAbsolutePath().toString()),
                                element("redirectTestOutputToFile", "true"),
                                element("test", testsToRun)
                        )
                );
                executeMojo(surefire, goal("test"),
                        (Xpp3Dom) surefire.getConfiguration(),
                        executionEnvironment(project, mavenSession, pluginManager));
            } catch (MojoExecutionException exc) {
                getLog().warn("Issues while executing the tests ", exc);
            } catch (PluginVersionResolutionException exc) {
                throw new MojoExecutionException("Could not find org.apache.maven.plugins:maven-surefire-plugin:2.19", exc);
            }
        }

        /*
        List<String> command = new ArrayList<>(Arrays.asList("mvn", "surefire:test", String.format("\"-Dstamp.reneri.folder=%s\"", resultFolder.toAbsolutePath().toString())));

        if(!classes.isEmpty()) {
            command.add(String.format("\"-Dtest=%s", classes.stream().collect(Collectors.joining(", "))));
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(project.getBasedir());
        for (int i = 0; i < testTimes; i++) {
            Process testProcess = null;
            try {
                testProcess = builder.start();
                testProcess.waitFor();
                int exitValue = testProcess.exitValue();
                if (exitValue != 0) {
                    getLog().warn("Test process exited with code " + exitValue);
                }
            }
            catch (IOException exc) {
                throw new MojoExecutionException("Failed to execute the test process", exc);
            }
            catch (InterruptedException exc) {
                throw new MojoExecutionException("Test process was interrupted", exc);
            }
            finally {
                if (testProcess != null && testProcess.isAlive()) {
                    testProcess.destroyForcibly();
                }
            }
        }*/

    }

    private void observeTransformations() throws MojoExecutionException {
        getLog().info("Observing transformation effects");

        try {
            List<MutationIdentifier> mutations = loadUndetectedMutations();

            getLog().debug(String.format("Found %d undetected transformations", mutations.size()));

            for (int index = 0; index < mutations.size(); index++) {

                getLog().debug("Executing transformation " + index);
                try {
                    executeMutation(getFolderPath(Integer.toString(index)), mutations.get(index));
                }
                catch (Exception exc) {
                    getLog().error("Error executing transformation " + index, exc);
                }
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not read mutation file", exc);
        }
    }

    private  List<MutationIdentifier> loadUndetectedMutations() throws IOException {
        Gson gson = new Gson();
        FileReader fileReader = new FileReader(transformations);
        JsonObject document = gson.fromJson(fileReader, JsonObject.class);
        JsonArray mutations = document.getAsJsonArray("mutations");
        List<MutationIdentifier> result = new ArrayList<>(mutations.size()/2);

        mutations.forEach(mutationElement -> {
            JsonObject mutation = mutationElement.getAsJsonObject();
            if(!mutation.getAsJsonPrimitive("status").getAsString().equals("SURVIVED")) {
                return;
            }
            JsonObject method = mutation.getAsJsonObject("method");
            ClassName className = ClassName.fromString(method.getAsJsonPrimitive("package").getAsString() + "." + method.getAsJsonPrimitive("class").getAsString());
            MethodName methodName = MethodName.fromString(method.getAsJsonPrimitive("name").getAsString());
            Location location = new Location(className, methodName, method.getAsJsonPrimitive("description").getAsString());
            MutationIdentifier mID = new MutationIdentifier(location, 0, mutation.getAsJsonPrimitive("mutator").getAsString());
            result.add(mID);
        });

        return result;
    }

    private void executeMutation(Path resultFolderPath, MutationIdentifier mutation) throws IOException, MojoExecutionException {
        // Mutate the source code
        Path pathToClass = getClassFilePath(mutation);
        byte[] originalClass = Files.readAllBytes(pathToClass);
        FileUtils.write(pathToClass, mutate(originalClass, mutation));
        //Select the tests to execute
        Set<String> tests = getTestClassesToExecute(mutation);
        //Save the information
        saveMutationInfo(resultFolderPath, mutation, tests);
        // Execute the tests
        executeTests(resultFolderPath, tests);
        //Restore the original code
        FileUtils.write(pathToClass, originalClass);
    }

    private Path getClassFilePath(MutationIdentifier mutation) {
        return  Paths.get(project.getBuild().getOutputDirectory(),  mutation.getClassName().asInternalName() + ".class");
    }

    private byte[] mutate(byte[] originalClass, MutationIdentifier mutation) {
        ClassReader classReader = new ClassReader(originalClass);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        MutationClassAdapter adapter = new MutationClassAdapter(mutation, classWriter);
        classReader.accept(adapter, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private Set<String> getTestClassesToExecute(MutationIdentifier mutation) {
        if(executeAllTests) {
            return Collections.emptySet();
        }

        HashSet<String> result = new HashSet<>();

        for(MethodTracesEntry entry : methodStackTraces) {
            Set<String> closestTests = entry.getClosestClassesTo(mutation, instrumentedTestClasses);
            result.addAll(graph.getInheritanceClousure(closestTests)); // Include derived classes
        }
        return result;
    }

    private void saveMutationInfo(Path folder, MutationIdentifier mutation, Set<String> tests) throws IOException {
        Location location = mutation.getLocation();
        JsonObject obj = new JsonObject();
        obj.addProperty("mutator", mutation.getMutator());
        obj.addProperty("class", location.getClassName().getNameWithoutPackage().toString());
        obj.addProperty("package", location.getClassName().getPackage().toString());
        obj.addProperty("method", location.getMethodName().toString());
        obj.addProperty("description", location.getMethodDesc());

        if(!tests.isEmpty()) {
            JsonArray testArray = new JsonArray();
            tests.forEach(testArray::add);
            obj.add("tests", testArray);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try(FileWriter writer = new FileWriter(folder.resolve("mutation.json").toFile())) {
            gson.toJson(obj, writer);
        }
    }

//
//    private void compileInstrumentedTests() throws MojoExecutionException {
//
//        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
//
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
//        Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromStrings(findSourceFiles(getGeneratedTestsPath()));
//
//        String classpath  = getTestClasspathElements().stream().collect(Collectors.joining(System.getProperty("path.separator")));
//
//        getLog().debug("Classpath to compile the instrumented tests");
//        getLog().debug(classpath);
//
//        String compiledTestsFolder = project.getBuild().getTestOutputDirectory();
//        Path compiledTestsPath = Paths.get(compiledTestsFolder);
//
//        try {
//            FileUtils.createEmptyDirectory(compiledTestsPath);
//        }
//        catch (IOException exc) {
//            throw new MojoExecutionException("Could not create folder for compiled tests", exc);
//        }
//
//        List<String> options = Arrays.asList("-cp",  classpath, "-d", compiledTestsFolder);
//
//        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, sources);
//
//        if(!task.call()) {
//            getLog().error("Error while compiling instrumented test classes");
//            for(Diagnostic fact : diagnostics.getDiagnostics()) {
//                getLog().error(String.format("%s in [%s,%s] %s ", fact.getMessage(Locale.ROOT),  fact.getLineNumber(), fact.getColumnNumber(), fact.getSource()));
//            }
//
//            throw new MojoExecutionException("Errors found while compiling the instrumented test classes");
//        }
//
//    }
//
//    private List<String> findSourceFiles(Path rootFolder) {
//        List<String> files = new ArrayList<>();
//
//        try {
//            Files.walkFileTree(rootFolder, new FileVisitor<Path>() {
//                @Override
//                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
//                    if (file.toString().endsWith(".java")) {
//                        files.add(file.toAbsolutePath().toString());
//                    }
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult visitFileFailed(Path file, IOException exc) {
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        }
//        catch (IOException exc) {
//            getLog().error("Error while inspecting source files", exc);
//        }
//
//        return files;
//
//    }
}
