package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;

public class FixedValue extends AtomicValueCondition {

    private Object value;

    public FixedValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean holdsFor(AtomicValueObservation observation) {
        return value.equals(observation.getValue());
    }

    public Object getValue() { return value; }
}
