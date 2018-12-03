package eu.stamp_project.reneri;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.stamp_project.mutationtest.descartes.codegeneration.MutationClassAdapter;
import eu.stamp_project.reneri.observation.ObserverClassProcessor;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassWriter;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mojo(name = "diff", requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class DiffExecutionMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "testCommandLineComplement")
    private String testCommandLineComplement;

    @Parameter(property = "targetTestClasses", defaultValue = "target-tests.txt")
    private File targetClasses;

    @Parameter(property = "ouputFolder", defaultValue = "${project.build.directory}/reneri")
    private File outputFolder;

    @Parameter(property = "transformations", defaultValue = "${project.build.directory/mutations.json}")
    private File transformations;

    @Parameter(property = "testTimes", defaultValue = "10")
    private int testTimes; //Number of times the tests should be executed

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            getLog().info("Cleaning previous results");
            createEmptyDirectory(outputFolder.toPath());

            getLog().info("Instrumenting test classes");
            instrumentTestClasses();

            getLog().info("Compiling instrumented test classes");
            compileInstrumentedTests();

            getLog().info("Executing test classes to observe original values");
            executeTests(getFolderPath("original"));

            getLog().info("Executing mutation analysis");
            doMutationAnalysis();
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Process failed", exc);
        }
    }

    public Path getGeneratedTestsPath() {
        Path testOutputPath = Paths.get(project.getBuild().getTestOutputDirectory());
        return testOutputPath.getParent().resolve("generated-test-sources");
    }

    protected void instrumentTestClasses() throws MojoExecutionException {

        Path testSourceOutputPath = getGeneratedTestsPath();
        createEmptyDirectory(testSourceOutputPath);
        // It seems that Spoon messes with the generated-test-sources folder

        MavenLauncher launcher = new MavenLauncher(project.getBasedir().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);
        launcher.addTemplateResource(new ResourceJavaFile("templates/ObserverTemplate.java"));
        CtModel model = launcher.buildModel();
        Set<CtClass<?>> testClasses = TestClassFinder.findTestClasses(model); // TODO: Fix the types

        getLog().info("Test classes found: " + testClasses.size());

        if(getLog().isDebugEnabled()) {
            for(CtClass type : testClasses) {
                getLog().debug(type.getQualifiedName());
            }
        }

        launcher.addProcessor( new ObserverClassProcessor(testClasses));
        launcher.process();
        launcher.setSourceOutputDirectory(testSourceOutputPath.toString());
        launcher.setOutputFilter(testClasses::contains);
        launcher.prettyprint();
    }

    private void createEmptyDirectory(Path root) throws MojoExecutionException {
        try {

            if(Files.exists(root)) {
                getLog().debug("Folder " + root.toString() + " already exists. Deleting its content.");

                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                });
            }
            Files.createDirectories(root);

        }
        catch(IOException exc) {
            throw new MojoExecutionException("Could not clear directory " + root.toString(), exc);
        }
    }

    protected List<String> getTestClasspathElements() {
        try {
            return project.getTestClasspathElements();
        }
        catch(DependencyResolutionRequiredException exc) {
            getLog().warn("Could not load all dependencies", exc);
            return Collections.singletonList(project.getBuild().getOutputDirectory());
        }
    }

    public void compileInstrumentedTests() throws MojoExecutionException {

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromStrings(findSourceFiles(getGeneratedTestsPath()));

        String classpath  = getTestClasspathElements().stream().collect(Collectors.joining(System.getProperty("path.separator")));

        getLog().debug("Classpath to compile the instrumented tests");
        getLog().debug(classpath);

        String compiledTestsFolder = project.getBuild().getTestOutputDirectory();
        Path compiledTestsPath = Paths.get(compiledTestsFolder);

        createEmptyDirectory(compiledTestsPath);

        List<String> options = Arrays.asList("-cp",  classpath, "-d", compiledTestsFolder);

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, sources);

        if(!task.call()) {
            getLog().error("Error while compiling instrumented test classes");
            for(Diagnostic fact : diagnostics.getDiagnostics()) {
                getLog().error(fact.getMessage(Locale.ROOT));
            }

            throw new MojoExecutionException("Errors found while compiling the instrumented test classes");
        }
    }

    protected  List<MutationIdentifier> loadUndetectedMutations() throws IOException {
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

    private Path getClassFilePath(MutationIdentifier mutation) {
       return  Paths.get(project.getBuild().getOutputDirectory(),  mutation.getClassName().asInternalName() + ".class");
    }

    protected  void executeTestCommand(Path resultFolder) throws MojoExecutionException {
        Process testProcess = null;
        try {

            ProcessBuilder builder = new ProcessBuilder("mvn", "surefire:test", String.format("-Dstamp.reneri.folder=\"%s\"", resultFolder.toAbsolutePath().toString()));
            builder.directory(project.getBasedir());
            testProcess = builder.start();
            testProcess.waitFor();
            if (testProcess.exitValue() != 0) {
                getLog().warn("Test process exited with code " + testProcess.exitValue());
            }

        }
        catch (IOException exc) {
            throw new MojoExecutionException("Failed to execute the test process", exc);
        }
        catch (InterruptedException exc) {
            throw new MojoExecutionException("Test process was interrupted", exc);
        }
        finally {
            if(testProcess != null && testProcess.isAlive())
                testProcess.destroyForcibly();
        }
    }


    protected void executeTests(Path folder)  throws MojoExecutionException {
        for(int i = 0; i < testTimes; i++) {
            executeTestCommand(folder);
        }
    }

    private Path getFolderPath(String name) throws IOException {
        Path folderPath = outputFolder.toPath().resolve(name).toAbsolutePath();
        Files.createDirectories(folderPath);
        return folderPath;
    }

    private void write(Path path, byte[] content) throws IOException {
        try(FileOutputStream output = new FileOutputStream(path.toFile())) {
            output.write(content);
        }
    }

    private byte[] mutate(byte[] originalClass, MutationIdentifier mutation) {
        ClassReader classReader = new ClassReader(originalClass);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        MutationClassAdapter adapter = new MutationClassAdapter(mutation, classWriter);
        classReader.accept(adapter, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private void saveMutationInfo(Path folder, MutationIdentifier mutation) throws IOException {
        Location location = mutation.getLocation();
        JsonObject obj = new JsonObject();
        obj.addProperty("mutator", mutation.getMutator());
        obj.addProperty("class", location.getClassName().getNameWithoutPackage().toString());
        obj.addProperty("package", location.getClassName().getPackage().toString());
        obj.addProperty("method", location.getMethodName().toString());
        obj.addProperty("description", location.getMethodDesc());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try(FileWriter writer = new FileWriter(folder.resolve("mutation.json").toFile())) {
            gson.toJson(obj, writer);
        }
    }

    protected void executeMutation(Path resultFolderPath, MutationIdentifier mutation) throws IOException, MojoExecutionException {
            Path pathToClass = getClassFilePath(mutation);
            byte[] originalClass = Files.readAllBytes(pathToClass);
            write(pathToClass, mutate(originalClass, mutation));
            saveMutationInfo(resultFolderPath, mutation);
            executeTests(resultFolderPath);
            write(pathToClass, originalClass);
    }


    protected void doMutationAnalysis() throws MojoExecutionException {
        try {
            List<MutationIdentifier> mutations = loadUndetectedMutations();

            getLog().debug(String.format("Found %d undetected mutations", mutations.size()));

            for (int index = 0; index < mutations.size(); index++) {

                getLog().debug("Executing mutation " + index);
                try {
                    executeMutation(getFolderPath(Integer.toString(index)), mutations.get(index));
                }
                catch (Exception exc) {
                    getLog().error("Error executing mutation " + index, exc);
                }
            }
        }
        catch(IOException exc) {
            throw  new MojoExecutionException("Could not read mutation file", exc);
        }
    }


    private List<String> findSourceFiles(Path rootFolder) {
        List<String> files = new ArrayList<>();

        try {
            Files.walkFileTree(rootFolder, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                    if(dir.endsWith("eu/stamp_project/reneri")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        files.add(file.toAbsolutePath().toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException exc) {
            getLog().error("Error while inspecting source files", exc);
        }

        return files;

    }


    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public String getTestCommandLineComplement() {
        return testCommandLineComplement;
    }

    public void setTestCommandLineComplement(String testCommandLineComplement) {
        this.testCommandLineComplement = testCommandLineComplement;
    }

    public File getTargetClasses() {
        return targetClasses;
    }

    public void setTargetClasses(File targetClasses) {
        this.targetClasses = targetClasses;
    }

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public File getTransformations() {
        return transformations;
    }

    public void setTransformations(File transformations) {
        this.transformations = transformations;
    }

    public int getTestTimes() {
        return testTimes;
    }

    public void setTestTimes(int testTimes) {
        this.testTimes = testTimes;
    }
}
