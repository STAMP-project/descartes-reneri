package eu.stamp_project.reneri.amplification;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import eu.stamp_project.reneri.codeanalysis.EntryPointFinder;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static javassist.Modifier.*;



@Mojo(name = "generation-targets", requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerationTargetMojo extends TargetMojo {

    private static final String CLASS = ".class";

    private EntryPointFinder finder;

    private HashMap<Method, GenerationTarget> targets = new HashMap<>();

    private  Set<String> delcaredClasses;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        findClassesInProject();
        createEntryPointFinder();
        inspectTransformations();
        reportTargets();
    }

    @Override
    protected void readMutation(JsonReader reader) throws IOException {
        String status = null;
        String mutator = null;
        Method method = null;

        reader.beginObject();

        while(JsonToken.NAME.equals(reader.peek())) {
            String property = reader.nextName();
            switch (property) {
                case "status":
                    status = reader.nextString();
                    break;
                case "mutator":
                    mutator = reader.nextString();
                    break;
                case "method":
                    method = getGson().fromJson(reader, Method.class);
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if(status == null || mutator == null || method == null) {
            throw new IOException("Mutation definition is missing some fields");
        }

        if(status.equals("SURVIVED")) {
            handleMethod(method, mutator);
        }
    }

    private void handleMethod(Method method, String mutator) {
        //TODO: Check computeifabsent

        if(targets.containsKey(method)) {
            targets.get(method).getMutators().add(mutator);
        }
        else {
            targets.put(method, new GenerationTarget(method, getEntryPoints(method), mutator));
        }
    }

    private Set<Method> getEntryPoints(Method method) {

        try {
            Set<CtMethod> jaMethods = finder.findEntryPointsFor(method.getClassQualifiedName(), method.getName(), method.getDescription());

            Set<Method> result = new HashSet<>();

            for (CtMethod entryPoint : jaMethods) {
                CtClass declaringClass = entryPoint.getDeclaringClass();

                int classModifiers = declaringClass.getModifiers();
                int epModifiers = entryPoint.getModifiers();

                if (!(isAbstract(classModifiers) || isInterface(classModifiers)) || isStatic(epModifiers)) {
                    result.add(new Method(entryPoint));
                } else {
                    String epDescriptor = entryPoint.getMethodInfo().getDescriptor();
                    result.addAll(
                            findInheritors(entryPoint)
                                    .stream()
                                    .map(inheritor -> new Method(
                                            inheritor.getSimpleName(),
                                            entryPoint.getName(),
                                            inheritor.getPackageName(),
                                            epDescriptor))
                                    .collect(Collectors.toList()));
                }
            }
            return result;
        }
        catch (NotFoundException exc) {
            getLog().warn("No entry points found for " + method.toString(), exc);
        }
        return null;
    }

    private Set<CtClass> findInheritors(CtMethod method) {

        HashSet<CtClass> inheritors = new HashSet<>();

        CtClass declaringClass = method.getDeclaringClass();
        ClassPool pool = declaringClass.getClassPool();

        String methodName = method.getName();
        String methodDescriptor = method.getMethodInfo().getDescriptor();

        for(String className : delcaredClasses) {
            CtClass candidateClass;
            try {
                candidateClass = pool.getCtClass(className);
            }
            catch (NotFoundException exc) {
                throw new AssertionError(String.format("Javassist can not find the class %s found in the project", className), exc);
            }
            int modifiers = candidateClass.getModifiers();
            if(isAbstract(modifiers) || isInterface(modifiers) || !candidateClass.subclassOf(declaringClass)) {
                continue;
            }
            try {
                CtMethod candidateMethod = candidateClass.getMethod(methodName, methodDescriptor);
                if (candidateMethod.equals(method)) { // It is the same method declaration
                    inheritors.add(candidateClass);
                }
            }
            catch (NotFoundException exc) {
                throw new AssertionError(String.format("Javassist can not find the inherited method %s in the derived class %s.", method.getLongName(), candidateClass.getName()), exc);
            }
        }
        return inheritors;
    }

    private void findClassesInProject() throws MojoExecutionException, MojoFailureException {
        try {
            delcaredClasses = getClassesInProject();
            if(delcaredClasses.isEmpty()) {
                throw new MojoFailureException("Could not find any class in the project");
            }
        }
        catch (IOException exc) {
            throw new MojoExecutionException("A problem occurred while searching for classes in the project", exc);
        }
    }

    private Set<String> getClassesInProject() throws IOException {

        Path root = Paths.get(getProject().getBuild().getOutputDirectory());

        Set<String> result = new HashSet<>();

        Files.walkFileTree(root, new FileVisitor<Path>()  {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                getLog().debug("Inspecting " + file.toString());
                String path = root.relativize(file).toString();
                getLog().debug("Relative path " + path);
                getLog().debug("Inspecting " + path);
                if(path.endsWith(CLASS)) {
                    String clasNameFromPath = path.substring(0, path.length() - CLASS.length()).replace('/', '.');
                    getLog().debug("Found class " + clasNameFromPath);
                    result.add(clasNameFromPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
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
        return result;
    }

    private void createEntryPointFinder() throws MojoExecutionException {
        try {
            ClassPool classPool = new ClassPool(true);
            for (String element : getProject().getCompileClasspathElements()) {
                classPool.appendClassPath(element);
            }
            finder = new EntryPointFinder(classPool);

        }
        catch (DependencyResolutionRequiredException | NotFoundException exc) {
            throw new MojoExecutionException("Could not load classpath elements", exc);
        }
    }

    private void reportTargets() throws MojoExecutionException {

        try(FileWriter writer = new FileWriter(getOutputFile("generation-targets.detailed.json"))) {
            getGson().toJson(targets.values().toArray(), writer);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not write detailed report", exc);
        }

        try(FileWriter writer = new FileWriter(getOutputFile("generation-targets.json"))) {
            getGson().toJson(
                    targets.values()
                            .stream()
                            .map( target -> {
                                Set<Method> entryPoints = target.getEntryPoints();
                                return entryPoints != null ? entryPoints : Collections.emptySet();
                            })
                            .flatMap(Set::stream)
                            .toArray(),
                    writer);
        }
        catch (IOException exc) {
            throw new MojoExecutionException("Could not write report", exc);
        }
    }

}
