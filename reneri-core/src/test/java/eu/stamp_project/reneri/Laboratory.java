package eu.stamp_project.reneri;

import eu.stamp_project.reneri.logging.TestExecutionObservationListener;
import eu.stamp_project.reneri.observations.MethodInvocationObservation;
import eu.stamp_project.testinputs.VersionedSet;
import eu.stamp_project.testinputs.VersionedSetTest;

public class Laboratory {

    public static void main(String[] args) throws Exception {
        String methodName = "incrementVersion";
        String methodDesc = "()V";
        String declaringClassName = VersionedSet.class.getName();
        String invokingTestClassName = VersionedSetTest.class.getName();
        String operator = "void";

        ExecutionObserver executionObserver = new ExecutionObserver(
                new TestExecutionObservationListener() {

                    @Override
                    public void executionStarted() {
                        System.out.println("Execution started");
                    }

                    @Override
                    public void testCaseStarted(String testIdentifier) {
                        System.out.println(" " + testIdentifier);
                    }

                    @Override
                    public void methodInvoked(MethodInvocationObservation observation) {
                        System.out.println("  " + observation.method);
                    }

                    @Override
                    public void testCaseSucceed() {
                        System.out.println(" Test case succeeded");
                    }

                    @Override
                    public void testCaseFailed(Throwable error) {
                        System.out.println(" Test case failed " + error);
                    }

                    @Override
                    public void executionFinished() {
                        System.out.println("Execution finished");

                    }
                },
                Jupiter::executeWithListener,
                Transformations.descartes(methodName, methodDesc, declaringClassName, operator),
                new DynamicTriggerCollector(Jupiter::execute)
        );
        executionObserver.observeEffects(methodName, methodDesc, declaringClassName, invokingTestClassName);

    }

}
