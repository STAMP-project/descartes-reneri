package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetOfTypes extends AtomicValueCondition {

    private Set<Class<?>> types;

    public SetOfTypes(Collection<Class<?>> types) {
        this.types = new HashSet<>(types);
    }

    @Override
    public boolean holdsFor(AtomicValueObservation observation) {
        return types.contains(observation.getObservedType());
    }
}
