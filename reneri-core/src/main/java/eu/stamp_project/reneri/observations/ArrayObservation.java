package eu.stamp_project.reneri.observations;

import java.util.Objects;

import static eu.stamp_project.reneri.utils.Types.isArrayDescriptor;

public final class ArrayObservation extends ValueObservation {

    private int length;

    public ArrayObservation(String type, int length) {
        if(!isArrayDescriptor(type)) {
            throw new IllegalArgumentException("Invalid array description, Got " + type);
        }
        if(length < 0) {
            throw new IllegalArgumentException("Length of the array can not be negative. Got " + length);
        }
        this.type = type;
        this.length = length;
    }

    public int getLength() {
        return length;
    }

}
