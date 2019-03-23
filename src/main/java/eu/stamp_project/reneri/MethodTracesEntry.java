package eu.stamp_project.reneri;

import org.pitest.mutationtest.engine.MutationIdentifier;
import spoon.reflect.declaration.CtClass;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodTracesEntry {

    public String method;

    public StackTraceElement[][] traces;

    public boolean matches(MutationInfo mutation) {
        return method.equals(mutation.getMethodInternalFullName());
    }

    public Set<String> getClosestClassesTo(MutationInfo mutation, Set<String> classes) {
        if(!matches(mutation)) {
            return Collections.emptySet();
        }

        //Set<String> classNames = classes.stream().map(CtClass::getQualifiedName).collect(Collectors.toSet());

        HashSet<String> result = new HashSet<>();

        for(StackTraceElement[] trace : traces) {
            for(StackTraceElement frame: trace) {
                String declaringClass = frame.getClassName();
                if(classes.contains(declaringClass)){
                    result.add(declaringClass);
                }
            }
        }
        return result;
    }

}
