package eu.stamp_project.reneri.observations;

import java.util.Objects;

import static eu.stamp_project.reneri.utils.Types.*;
import static eu.stamp_project.utils.TypeHelper.unwrap;

public final class AtomicValueObservation extends ValueObservation {

    private final static double EPSILON = 0.00001;

    private Object value;

    public AtomicValueObservation(String type, Object value) {
        validate(type, value);

        this.type = type;
        this.value = value;
    }

    private void validate(String type, Object value) {
        Objects.requireNonNull(value, "Atomic value observations can not be null");
        Class<?> dynamicType = value.getClass();
        requireWrapperType(dynamicType, "Only wrapper or primitive type values should be used for atomic observations");
        String dynamicTypeDescriptor = dynamicType.descriptorString();
        if(!type.equals(dynamicTypeDescriptor) && !type.equals(unwrap(dynamicType).descriptorString())) {
            throw new IllegalArgumentException(String.format("Value %s of type %s does not match given descriptor %s", value, dynamicTypeDescriptor, type));
        }
    }

    public Object getValue() {
        return value;
    }


}
