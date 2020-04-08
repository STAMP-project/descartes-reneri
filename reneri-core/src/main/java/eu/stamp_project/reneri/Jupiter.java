package eu.stamp_project.reneri;

import eu.stamp_project.reneri.logging.TestExecutionObservationListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class Jupiter {

    //TODO: Define the parameters for the execution like, the tests to invoke
    public static void execute(Class<?> testClass) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }

    //TODO: Define the parameters for the execution, like the tests to invoke
    public static void executeWithListener(Class<?> testClass, TestExecutionObservationListener listener) {

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, new TestExecutionListener() {
            @Override
            public void testPlanExecutionStarted(TestPlan testPlan) { listener.executionStarted(); }

            @Override
            public void testPlanExecutionFinished(TestPlan testPlan) { listener.executionFinished(); }

            @Override
            public void executionStarted(TestIdentifier testIdentifier) {
                if (testIdentifier.isTest())
                    listener.testCaseStarted(testIdentifier.getUniqueId());
            }

            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                if(!testIdentifier.isTest()) return;
                if(testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                    listener.testCaseSucceed();
                }
                else {
                    listener.testCaseFailed(testExecutionResult.getThrowable().orElse( new Exception("Unspecified error")));
                }
            }
        });

    }

}
