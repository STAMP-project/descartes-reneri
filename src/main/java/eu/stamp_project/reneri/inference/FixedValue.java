package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;


//TODO: We probably don't need this one
public class FixedValue extends AtomicValueCondition {

    private Object value;
    private Class<?> type;

    public FixedValue(Object value, Class<?> type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public boolean canTarget(AtomicValueObservation observation) {
        return super.canTarget(observation) && observation.getObservedType().equals(type);
    }

    @Override
    public boolean holdsFor(AtomicValueObservation observation) {
        return value.equals(observation.getValue());
    }

    public Object getValue() { return value; }
}
