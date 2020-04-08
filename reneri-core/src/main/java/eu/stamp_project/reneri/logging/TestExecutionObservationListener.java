package eu.stamp_project.reneri.logging;
import eu.stamp_project.reneri.observations.MethodInvocationObservation;

public interface TestExecutionObservationListener {

    default void executionStarted() {}

    default void testCaseStarted(String testIdentifier){}

    default void methodInvoked(MethodInvocationObservation observation){}

    default void testCaseSucceed(){}

    default void testCaseFailed(Throwable error){}

    default void executionFinished(){}

}
