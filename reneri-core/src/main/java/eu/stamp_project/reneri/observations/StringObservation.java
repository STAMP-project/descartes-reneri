package eu.stamp_project.reneri.observations;

import java.util.Objects;


public final class StringObservation extends ValueObservation {

    public static final int PREFIX_LENGTH = 100;

    private String value;

    private int length;

    public StringObservation(String value) {
        Objects.requireNonNull(value, "String observations can not be null");
        this.length = value.length();
        this.value = length > PREFIX_LENGTH? value.substring(0, PREFIX_LENGTH) : value;
        this.type = String.class.descriptorString();
    }

    public String getValue() {
        return value;
    }

    public int getLength() {
        return length;
    }
}
