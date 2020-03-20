package eu.stamp_project.reneri.observations;


public abstract class ValueObservation extends Observation {

    protected String type;

    // Each extending class has its own requirements for the type
    // So we let them assign the value directly

    public  String getType() {
        return type;
    };

}
