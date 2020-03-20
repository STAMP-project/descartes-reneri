package eu.stamp_project.reneri.observations;

import java.util.Objects;

import static eu.stamp_project.reneri.utils.Types.isClassDescriptor;

public final class ExceptionObservation extends ValueObservation {

    private String message;

    public ExceptionObservation(String type, String message) {
        if(!isClassDescriptor(type)) {
            throw new IllegalArgumentException("Invalid type descriptor for exception. Got " + type);
        }
        if(message == null || message.isBlank()) {
            throw new IllegalArgumentException("Exception message can not be null or blank");
        }

        this.type = type;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
