package eu.stamp_project.reneri.observations;

import java.util.Objects;

public class ObservedException extends ObservedValue {

    private String exceptionMessage;

    public ObservedException(String exceptionTypeName, String exceptionMessage) {
        super(exceptionTypeName);
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservedException that = (ObservedException) o;
        return Objects.equals(typeName, that.typeName) &&
                Objects.equals(exceptionMessage, that.exceptionMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, exceptionMessage);
    }

}
