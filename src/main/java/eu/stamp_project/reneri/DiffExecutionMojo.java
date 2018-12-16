package eu.stamp_project.reneri;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.stamp_project.mutationtest.descartes.codegeneration.MutationClassAdapter;
import eu.stamp_project.reneri.observation.ObserverClassProcessor;
import eu.stamp_project.reneri.observation.StateObserver;
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
import spoon.reflect.declaration.CtType;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

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
    private File stackTraces;

    public File getStackTraces() {
        return stackTraces;
    }

    public void setStackTraces(File stackTraces) {
        this.stackTraces = stackTraces;
    }

    private MethodTracesEntry[] methodTraces;

    private Set<CtClass<?>> testClasses;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            getLog().info("Cleaning previous results");
            createEmptyDirectory(outputFolder.toPath());

            getLog().info("Instrumenting test classes");

            testClasses = instrumentTestClasses();
            if(testClasses.isEmpty()) {
                getLog().warn("Stopping analysis as no test class was found");
                return;
            }

            getLog().info("Compiling instrumented test classes");
            compileInstrumentedTests();


            getLog().info("Loading stack traces");
            methodTraces = loadMethodTraces();

            getLog().info("Executing test classes to observe original values");
            executeTests(getFolderPath("original"));

            getLog().info("Executing mutation analysis");
            doMutationAnalysis();
        }
        catch (Throwable exc) {
            throw new MojoExecutionException("Process failed", exc);
        }
    }

    private Path getGeneratedTestsPath() {
        return Paths.get(project.getBuild().getTestOutputDirectory()).getParent().resolve("reneri-generated").toAbsolutePath();
    }

    private Set<CtClass<?>> instrumentTestClasses() throws MojoExecutionException {

        Path testSourceOutputPath = getGeneratedTestsPath();
        // It seems that Spoon messes with the generated-test-sources folder
        createEmptyDirectory(testSourceOutputPath);

        MavenLauncher launcher = new MavenLauncher(project.getBasedir().getAbsolutePath(), MavenLauncher.SOURCE_TYPE.ALL_SOURCE);

        // Adding the observer to the path so it can be compiled at the same time as the project
        launcher.addInputResource(new ResourceJavaFile("utils/StateObserver.java"));
        CtModel model = launcher.buildModel();


        Set<CtClass<?>> testClassesFound = TestClassFinder.findTestClasses(model);

        getLog().info("Test classes found: " + testClassesFound.size());

        if(getLog().isDebugEnabled()) {
            for(CtClass type : testClassesFound) {
                getLog().debug(type.getQualifiedName());
            }
        }

        launcher.addProcessor( new ObserverClassProcessor(testClassesFound));
        launcher.process();

        getLog().debug("Saving instrumented classes");



        launcher.setSourceOutputDirectory(testSourceOutputPath.toString());

        CtClass<?> observerClass = (CtClass<?>)model.getRootPackage().getFactory().Type().get(StateObserver.class);

        testClassesFound.add(observerClass);

        launcher.setOutputFilter(testClassesFound::contains);
        launcher.prettyprint();

        testClassesFound.remove(observerClass);

        return testClassesFound;
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

    private List<String> getTestClasspathElements() {
        try {
            return project.getTestClasspathElements();
        }
        catch(DependencyResolutionRequiredException exc) {
            getLog().warn("Could not load all dependencies", exc);
            return Collections.singletonList(project.getBuild().getOutputDirectory());
        }
    }

    private void compileInstrumentedTests() throws MojoExecutionException {

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
                getLog().error(String.format("%s in [%s,%s] %s ", fact.getMessage(Locale.ROOT),  fact.getLineNumber(), fact.getColumnNumber(), fact.getSource()));
            }

            throw new MojoExecutionException("Errors found while compiling the instrumented test classes");
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

    private MethodTracesEntry[] loadMethodTraces() throws MojoExecutionException {
        if(executeAllTests) {
            getLog().debug("All tests are going to be executed against each transformation");
            return  new MethodTracesEntry[0];
        }

        Gson gson = new Gson();
        try(FileReader reader = new FileReader(stackTraces)) {
            return gson.fromJson(reader, MethodTracesEntry[].class);

        }
        catch (Exception exc) {
            throw new MojoExecutionException("An error occurred while loading the stack traces file.", exc);
        }
    }

    private Path getClassFilePath(MutationIdentifier mutation) {
       return  Paths.get(project.getBuild().getOutputDirectory(),  mutation.getClassName().asInternalName() + ".class");
    }

    private void executeTests(Path resultFolder) throws MojoExecutionException {
        executeTests(resultFolder, Collections.emptySet());
    }

    private void executeTests(Path resultFolder, Set<String> classes)  throws MojoExecutionException {


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

    private Set<String> getClosestTestClasses(MutationIdentifier mutation) {
        if(executeAllTests) {
            return Collections.emptySet();
        }

        HashSet<String> result = new HashSet<>();

        for(MethodTracesEntry entry : methodTraces) {
            result.addAll(entry.getClosestClassesTo(mutation, testClasses));
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

    private void executeMutation(Path resultFolderPath, MutationIdentifier mutation) throws IOException, MojoExecutionException {
            // Mutate the source code
            Path pathToClass = getClassFilePath(mutation);
            byte[] originalClass = Files.readAllBytes(pathToClass);
            write(pathToClass, mutate(originalClass, mutation));
            //Select the tests to execute
            Set<String> tests = getClosestTestClasses(mutation);
            //Save the information
            saveMutationInfo(resultFolderPath, mutation, tests);
            // Execute the tests
            executeTests(resultFolderPath, tests);
            //Restore the original code
            write(pathToClass, originalClass);
    }


    private void doMutationAnalysis() throws MojoExecutionException {
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
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        files.add(file.toAbsolutePath().toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException exc) {
            getLog().error("Error while inspecting source files", exc);
        }

        return files;

    }



}
