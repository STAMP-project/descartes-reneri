package eu.stamp_project.reneri.observations;

import static java.util.Objects.*;

public class StaticTypeObservation {

    private String staticType;

    private ValueObservation valueObservation;

    public StaticTypeObservation(String staticType, ValueObservation valueObservation) {
        //TODO: Check it is a correct type definition
        this.staticType = staticType;
        this.valueObservation = valueObservation;
    }

    public String getStaticType() {
        return staticType;
    }

    public ValueObservation getValueObservation() {
        return valueObservation;
    }


}
