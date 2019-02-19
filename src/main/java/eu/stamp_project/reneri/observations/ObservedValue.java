package eu.stamp_project.reneri.observations;

import java.util.Optional;

public abstract class ObservedValue {

    protected String typeName;

    public ObservedValue(String typeName) {
        this.typeName = typeName;
    }

    public String getObservedTypeName() {
        return typeName;
    }
    
    public boolean isNull() { return false; };

}
