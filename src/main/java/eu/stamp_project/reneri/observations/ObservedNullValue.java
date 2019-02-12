package eu.stamp_project.reneri.observations;

import java.util.Objects;

public class ObservedNullValue extends ObservedValue{

    @Override
    public boolean isNull() {
        return isNull;
    }

    private boolean isNull;

    public ObservedNullValue(String typeName, boolean isNull) {
        super(typeName);
        this.isNull = isNull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservedNullValue that = (ObservedNullValue) o;
        return isNull == that.isNull &&
                Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, isNull);
    }
}
