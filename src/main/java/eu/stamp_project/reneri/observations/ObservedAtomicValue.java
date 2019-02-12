package eu.stamp_project.reneri.observations;

import java.util.Objects;

public class ObservedAtomicValue extends ObservedValue {

    private String literalValue;

    public ObservedAtomicValue(String typeName, String literalValue) {
        super(typeName);
        this.literalValue = literalValue;
    }

    public String getLiteralValue() {
        return literalValue;
    }

    @Override
    public boolean isNull() {
        return literalValue.equals("null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservedAtomicValue that = (ObservedAtomicValue) o;
        return Objects.equals(typeName, that.typeName) &&
                Objects.equals(literalValue, that.literalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, literalValue);
    }
}
