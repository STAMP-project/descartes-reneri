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

    public boolean matches(MutationIdentifier mutation) {
        return method.equals(
                String.format("%s/%s%s",
                        mutation.getClassName().asInternalName(),
                        mutation.getLocation().getMethodName().name(),
                        mutation.getLocation().getMethodDesc()
                )
        );
    }

    public Set<String> getClosestClassesTo(MutationIdentifier mutation, Set<CtClass<?>> classes) {
        if(!matches(mutation)) {
            return Collections.emptySet();
        }

        Set<String> classNames = classes.stream().map(CtClass::getQualifiedName).collect(Collectors.toSet());

        HashSet<String> result = new HashSet<>();

        for(StackTraceElement[] trace : traces) {
            for(StackTraceElement frame: trace) {
                String declaringClass = frame.getClassName();
                if(classNames.contains(declaringClass)){
                    result.add(declaringClass);
                }
            }
        }
        return result;
    }

}
