package eu.stamp_project.reneri.observations;

import java.util.function.Consumer;

public class Probe {

    public static Observer observer;

    public static Consumer<Observation> logger;

    public static void init(Observer observer, Consumer<Observation> logger) {
        Probe.observer = observer;
        Probe.logger = logger;
    }

    public static void method(String method, int line, Class<?> receiverType, Object receiver, Class<?>[] parameters, Object[] arguments, Class<?> resultType, Object result) {
        logger.accept(observer.observeInvocation(method, line, receiverType, receiver, parameters, arguments, resultType, result, null, true));
    }

    public static void method(String method, int line, Class<?> receiverType, Object receiver, Class<?>[] parameters, Object[] arguments, Throwable exc) {
       logger.accept(observer.observeInvocation(method, line, receiverType, receiver, parameters, arguments, null, null, exc, true));
    }

    public static void trigger(String method, int line, Class<?> receiverType, Object receiver, Class<?>[] parameters, Object[] arguments, Class<?> resultType, Object result) {
        logger.accept(observer.observeInvocation(method, line, receiverType, receiver, parameters, arguments, resultType, result, null, false));
    }

    public static void trigger(String method, int line, Class<?> receiverType, Object receiver, Class<?>[] parameters, Object[] arguments, Throwable exc) {
        logger.accept(observer.observeInvocation(method, line, receiverType, receiver, parameters, arguments, null, null, exc, false));
    }

    public static void trigger() {
        logger.accept(observer.observeTrigger());
    }

}
