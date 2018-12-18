package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;

import java.util.Set;

public class SetOfValues extends AtomicValueCondition {

    private Set values;

    public SetOfValues(Set values) {
        this.values = values;
    }

    @Override
    public boolean holds(AtomicValueObservation observation) {
        return values.contains(observation.getValue());
    }
}
