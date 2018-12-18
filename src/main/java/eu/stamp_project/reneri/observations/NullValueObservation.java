package eu.stamp_project.reneri.observations;

public class NullValueObservation extends ValueObservation {

    private String pointcut;

    private String typeName;

    private boolean isNull;

    public NullValueObservation(String pointcut, String typeName, boolean isNull) {
        super(pointcut);

        this.pointcut = pointcut;
        this.typeName = typeName;
        this.isNull = isNull;
    }

    @Override
    public String getObservedTypeName() {
        return typeName;
    }

    @Override
    public boolean isNull() {
        return isNull;
    }

}
