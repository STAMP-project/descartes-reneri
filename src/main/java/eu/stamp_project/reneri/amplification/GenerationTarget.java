package eu.stamp_project.reneri.amplification;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GenerationTarget {

    private Method method;

    private HashSet<String> mutators = new HashSet<>();

    private HashSet<Method> entryPoints = new HashSet<>();

    public GenerationTarget(Method method, Set<Method> entryPoints, String... mutators) {
        this.method = method;
        this.mutators.addAll(Arrays.asList(mutators));
        this.entryPoints.addAll(entryPoints);
    }

    public Method getMethod() {
        return method;
    }

    public HashSet<String> getMutators() {
        return mutators;
    }

    public HashSet<Method> getEntryPoints() {
        return entryPoints;
    }


}
