package eu.stamp_project.reneri.codeanalysis;

import eu.stamp_project.reneri.datastructures.DirectedGraph;
import javassist.*;
import javassist.bytecode.*;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class EntryPointFinder {

    private ClassPool pool;

    public EntryPointFinder(ClassPool pool) {
        Objects.requireNonNull(pool, "Provided ClassPool can not be null");
        this.pool = pool;
    }

    public Set<CtMethod> findAccessors(String fieldName, String observedClass) /*throws NotFoundException*/ {

        try {
            CtClass targetClass = pool.getCtClass(observedClass);
            CtField field = targetClass.getField(fieldName); // TODO: There could be cases in which a base class declares a field with the same name

            if (Modifier.isPublic(field.getModifiers())) {
                return null; // Special value saying that the field could be directly accessed
            }

            // The field is private:
            CtClass declaringClass = field.getDeclaringClass();
            Set<CtMethod> accessors = Arrays.stream(declaringClass.getDeclaredMethods())
                    .filter((m) -> usesField(m, field))
                    .collect(Collectors.toSet());

            if (accessors.isEmpty()) {
                return accessors;
            }
            Set<CtMethod> nonPrivateAccessors = selectNonPrivate(accessors);
            if (!nonPrivateAccessors.isEmpty()) {
                return nonPrivateAccessors;
            }
            return findEntryPointsFor(accessors, declaringClass);
        }
        catch (NotFoundException exc) {
            // There might be cases in which the field does not belong to the static type
            // For example, the static type is an interface
            // for these cases we can not find the accessors
            return Collections.emptySet();
        }
    }

    private boolean usesField(CtMethod method, CtField field) {

        String declaringClassName = field.getDeclaringClass().getName();
        String fieldName = field.getName();

        MethodInfo info = method.getMethodInfo();
        ConstPool constPool = info.getConstPool();
        CodeAttribute codeAttribute = info.getCodeAttribute();
        if(codeAttribute == null) {
            // We don't know actually
            return false;
        }
        CodeIterator it = codeAttribute.iterator();
        try {
            while (it.hasNext()) {
                int index = it.next();
                int opcode = it.byteAt(index);
                // 180 : getfield
                // 178 : getstatic
                if (opcode == 180 || opcode == 178) {
                    int argument = it.s16bitAt(index + 1);
                    if (constPool.getFieldrefClassName(argument).equals(declaringClassName) &&
                            constPool.getFieldrefName(argument).equals(fieldName)) {
                        return true;
                    }
                }
            }
            return false;
        }
        catch (BadBytecode exc) {
            return false;
        }
    }

    private Set<CtMethod> findEntryPointsFor(Set<CtMethod> methodsToReach, CtClass declaringClass) {
        DirectedGraph<CtMethod> callGraph = buildCallGraph(declaringClass);
        return selectNonPrivate(methodsToReach
                .stream()
                .map(callGraph::getClousure).collect(Collector.of(
                        HashSet<CtMethod>::new,
                        HashSet<CtMethod>::addAll,
                        (a, b) -> {
                            a.addAll(b);
                            return a;
                        })
                ));
    }

    private Set<CtMethod> selectNonPrivate(Set<CtMethod> methods) {
        return methods.stream()
                .filter((m) -> !Modifier.isPrivate(m.getModifiers()))
                .collect(Collectors.toSet());
    }

    private DirectedGraph<CtMethod> buildCallGraph(CtClass declaringClass) {

        DirectedGraph<CtMethod> callGraph = new DirectedGraph<>();

        for(CtMethod method : declaringClass.getDeclaredMethods()) {
            try {
                MethodInfo info = method.getMethodInfo();
                ConstPool constPool = info.getConstPool();
                CodeAttribute codeAttribute = info.getCodeAttribute();
                if(codeAttribute == null) {
                    // Can't inspect methods called by this one
                    continue;
                }
                CodeIterator it = codeAttribute.iterator();
                while (it.hasNext()) {
                    int index = it.next();
                    int opcode = it.byteAt(index);
                    if (opcode >= 182 && opcode <= 186) { // invoke*
                        int argument = it.s16bitAt(index + 1);

                        if (!declaringClass.getName().equals(constPool.getMethodrefClassName(argument))) {
                            continue;
                        }
                        try {
                            CtMethod invokedMethod = declaringClass.getMethod(
                                    constPool.getMethodrefName(argument),
                                    constPool.getMethodrefType(argument)
                            );
                            callGraph.addEdge(invokedMethod, method); // Inverted call graph
                        } catch (NotFoundException exc) {
                            // Skip this method call
                        }
                    }
                }
            } catch (BadBytecode exc) {
                // Skip the method
            }
        }

        return callGraph;
    }


    private Set<CtMethod> getMethodsInvoking(CtClass container, Set<CtMethod> targetMethods) {

        HashSet<CtMethod> result = new HashSet<>();
        for (CtMethod method : container.getDeclaredMethods()) {
            try {
                MethodInfo info = method.getMethodInfo();
                ConstPool constPool = info.getConstPool();
                CodeAttribute codeAttribute = info.getCodeAttribute();
                CodeIterator it = codeAttribute.iterator();
                while (it.hasNext()) {
                    int index = it.next();
                    int opcode = it.byteAt(index);
                    if (opcode < 182 || opcode > 186) {
                        continue;
                    }
                    int argument = it.s16bitAt(index + 1);

                    CtClass invokedClass = pool.getCtClass(constPool.getMethodrefClassName(argument));
                    CtMethod invokedMethod = invokedClass.getMethod(constPool.getMethodrefName(argument), constPool.getMethodrefType(argument));

                    if(targetMethods.contains(invokedMethod)) {
                        result.add(method);
                    }
                }

            } catch (BadBytecode | NotFoundException exc) {
                // Skip the method
            }
        }
        return result;
    }

    public Set<CtMethod> findEntryPointsFor(String className, String methodName, String methodDescription) throws NotFoundException {

        CtClass targetClass = pool.getCtClass(className);
        CtMethod methodToReach = targetClass.getMethod(methodName, methodDescription);

        if(!Modifier.isPrivate(methodToReach.getModifiers())) {
            return Collections.singleton(methodToReach);
        }

        CtClass declaringClass = methodToReach.getDeclaringClass();
        Set<CtMethod> entryPoints = findEntryPointsFor(Collections.singleton(methodToReach), declaringClass);

        if(entryPoints.isEmpty()) {
            return entryPoints;
        }

        while(Modifier.isPrivate(declaringClass.getModifiers())) {
            declaringClass = declaringClass.getDeclaringClass();
            entryPoints = findEntryPointsFor(getMethodsInvoking(declaringClass, entryPoints), declaringClass);
        }

        return entryPoints;

    }
}
