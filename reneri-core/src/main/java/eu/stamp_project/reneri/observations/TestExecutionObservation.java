package eu.stamp_project.reneri.observations;

import java.util.*;

public class TestExecutionObservation implements Iterable<MethodInvocationObservation> {

    private String identifier;
    private Throwable error;
    private List<MethodInvocationObservation> observedMethodInvocations;

    public TestExecutionObservation(String identifier) {
        this.identifier = identifier;
        observedMethodInvocations = new ArrayList<>();
    }

    public TestExecutionObservation(String identifier, MethodInvocationObservation... observedMethodInvocations) {
        this(identifier);
        this.observedMethodInvocations = Arrays.asList(observedMethodInvocations);
    }

    public void addInvocation(MethodInvocationObservation observedInvocation) {
        Objects.requireNonNull(observedInvocation, "Method invocation observation can not be null");
        observedMethodInvocations.add(observedInvocation);
    }

    public String getIdentifier() { return identifier; }

    public List<MethodInvocationObservation> getObservedInvocations() {
        return observedMethodInvocations;
    }

    public void resetError() { this.error = null; }

    public void setError(Throwable error) {
        Objects.requireNonNull(error,"Test execution error can not be null");
        this.error = error;
    }

    public Throwable getError() { return error; }

    public boolean testFailed() { return error != null; }

    @Override
    public Iterator<MethodInvocationObservation> iterator() {
        return observedMethodInvocations.iterator();
    }

}
