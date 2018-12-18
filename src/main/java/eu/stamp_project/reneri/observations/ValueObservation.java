package eu.stamp_project.reneri.observations;

public abstract class ValueObservation extends Observation {

    public ValueObservation(String pointcut) {
        super(pointcut);
    }

    public abstract String getObservedTypeName();

    public abstract boolean isNull();
}
