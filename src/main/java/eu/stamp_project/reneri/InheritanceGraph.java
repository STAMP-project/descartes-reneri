package eu.stamp_project.reneri;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;


public class InheritanceGraph {

    private HashMap<String, Set<String>> adjacencyList ;


    public InheritanceGraph(Collection<CtClass<?>> classes) {
        buildAdjancencyList(classes);
    }

    protected void buildAdjancencyList(Collection<CtClass<?>> classes) {

        adjacencyList = new HashMap<>();

        for(CtClass<?> aClass :  classes) {
            adjacencyList.putIfAbsent(aClass.getQualifiedName(), new HashSet<>());
        }

        for(CtClass<?> aClass : classes) {
            CtTypeReference<?> superClass = aClass.getSuperclass();
            if(superClass == null) {
                continue;
            }

            String superClassName = superClass.getQualifiedName();
            Set<String> directDescendants  = adjacencyList.getOrDefault(superClassName, null);
            if(directDescendants != null) {
                directDescendants.add(aClass.getQualifiedName());
            }
        }
    }

    public Set<String> getInheritanceClousure(String className) {
        return getInheritanceClousure(Collections.singleton(className));
    }

    public Set<String> getInheritanceClousure(Set<String> classes) {
        Set<String> result = new HashSet<>();

        Queue<String> queue = new LinkedList<>();
        queue.addAll(classes);

        while(!queue.isEmpty()) {
            String current = queue.remove();
            if(result.contains(current)) {
                continue;
            }
            result.add(current);
            queue.addAll(adjacencyList.getOrDefault(current, Collections.emptySet()));
        }

        return result;
    }
}
