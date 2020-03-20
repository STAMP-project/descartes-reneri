package eu.stamp_project.reneri.observations;

import eu.stamp_project.reneri.logging.Logger;
import eu.stamp_project.reneri.observations.Observer;

public class Probe {

    public static Observer observer;

    public static Logger logger;

    public static void observeInvocation(
            String method, int line,
            Class<?> receiverType, Object receiver,
            Class<?> resultType, Object result,
            Class<?>[] parameters, Object[] arguments,
            Throwable exc) {
        logger.logObservation(observer.observeInvocation(method, line, receiverType, receiver, resultType, result, parameters, arguments, exc));
    }

    public static void observeInvocationAndTrigger(
            String method, int line,
            Class<?> receiverType, Object receiver,
            Class<?> resultType, Object result,
            Class<?>[] parameters, Object[] arguments,
            Throwable exc) {
        logger.logObservation(observer.observeInvocationAndTrigger(method, line, receiverType, receiver, resultType, result, parameters, arguments, exc));
    }

    public static void observeTrigger() {
        logger.logObservation(observer.observeTrigger());
    }

}
