package eu.stamp_project.reneri.inference;


import eu.stamp_project.reneri.observations.AtomicValueObservation;

import java.util.Set;

//TODO: Maybe separate values and types
public class SetOfValues extends AtomicValueCondition {

    private Set<Class<?>> types;
    private Set values;

    public SetOfValues(Set values, Set<Class<?>> types) {
        this.values = values;
        this.types = types;
    }

    @Override
    public boolean canTarget(AtomicValueObservation observation) {
        return types.contains(observation.getObservedType());
    }

    //TODO: Do something to provide the cause of the mismatch
    @Override
    public boolean holdsFor(AtomicValueObservation observation) {
        return values.contains(observation.getValue()) && types.contains(observation.getObservedType());
    }
}
