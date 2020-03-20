package eu.stamp_project.reneri.observations;

import static eu.stamp_project.reneri.utils.Types.isClassDescriptor;

public final class FieldObservation extends StaticTypeObservation {

    private String declaringClass, name;

    private ValueObservation observation;

    public FieldObservation(String declaringClass, String name, String type, ValueObservation observation) {
        super(type, observation);
        if(!isClassDescriptor(declaringClass)) {
            throw new IllegalArgumentException("Fields must be declared in classes. Got declaring type " + declaringClass);
        }
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("Field names must not be null or blank");
        }
        //Objects.requireNonNull(observation, "Value of field observation must not be null");
        //TODO: Validate that the type is not null, blank and represents a type
        this.declaringClass = declaringClass;
        this.name = name;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public ValueObservation getObservation() {
        return observation;
    }


}
