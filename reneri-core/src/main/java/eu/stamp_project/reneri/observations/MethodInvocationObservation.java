package eu.stamp_project.reneri.observations;

public final class MethodInvocationObservation extends Observation {

    // So far, there is no point in adding getters and setters,
    // when they do no other thing than accessing the field and assigning the value
    // remains to be seen if it would be required for serialization

    public StaticTypeObservation receiver, result;

    public ExceptionObservation exception;

    public StaticTypeObservation[] arguments;

    public TriggerObservation trigger;

    public final String method;

    public final int line;

    public MethodInvocationObservation(String method, int line) {
        this.method = method;
        this.line = line;
    }
}
