package eu.stamp_project.reneri.observations;


import static eu.stamp_project.reneri.utils.Types.*;

public class ObjectObservation extends ValueObservation {

    private FieldObservation[] fields;

    public ObjectObservation(String type, FieldObservation... fields) {
        if(!isClassDescriptor(type)) {
            throw new IllegalArgumentException("Type for object observation must be a class. Got " + type);
        }

        this.type = type;
        this.fields = fields;
    }

    public FieldObservation[] getFields() {
        return fields;
    }

    public void setFields(FieldObservation... fields) {
        this.fields = fields;
    }

    public boolean hasFields() {
        return fields != null && fields.length > 0;
    }


}
