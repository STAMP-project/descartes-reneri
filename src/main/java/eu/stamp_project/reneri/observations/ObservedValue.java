package eu.stamp_project.reneri.observations;

import java.io.Serializable;

public abstract class ObservedValue implements Serializable {

    protected String typeName;

    public ObservedValue(String typeName) {
        this.typeName = typeName;
    }

    public String getObservedTypeName() {
        return typeName;
    }
    
    public boolean isNull() { return false; };

}
