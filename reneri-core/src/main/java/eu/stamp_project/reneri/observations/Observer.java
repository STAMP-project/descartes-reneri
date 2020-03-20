package eu.stamp_project.reneri.observations;

import java.lang.reflect.Array;
import java.util.*;

import static eu.stamp_project.reneri.utils.Types.*;

public class Observer {

    private final static String RENERI_OBSERVATION_PACKAGE = Observer.class.getPackageName();

    private HashSet<String> triggerSentinels = new HashSet<>();

    public void setTriggerSentinels(Class<?> sentinel) {
        triggerSentinels.clear();
        LinkedList<Class<?>> queue = new LinkedList<>();
        queue.add(sentinel);
        while(!queue.isEmpty()) {
            Class<?> current  = queue.removeFirst();
            String className = current.getName();
            if(triggerSentinels.contains(className)) continue;
            triggerSentinels.add(className);
            Class<?> superClass = current.getSuperclass();
            if(superClass != null && superClass != Object.class) {
                queue.add(superClass);
            }
            queue.addAll(List.of(current.getInterfaces()));
        }
    }

    public TriggerObservation observeTrigger() {

        if(triggerSentinels.isEmpty()) return null; // Quick exit

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int start = 0;
        while(stackTrace[start].getClassName().startsWith(RENERI_OBSERVATION_PACKAGE)){ start++; };
        for(int i = start; i  < stackTrace.length; i++) {
            if(!triggerSentinels.contains(stackTrace[i].getClassName())) continue;
            StackTraceElement container = stackTrace[i];
            StackTraceElement trigger = stackTrace[i - 1];
            return new TriggerObservation(
                    container.getClassName(),
                    container.getMethodName(),
                    trigger.getClassName(),
                    trigger.getMethodName(),
                    container.getLineNumber(),
                    i - start);
        }
        return null;
    }

    public ValueObservation shallowObserve(Object value) {
        Class<?> type = value.getClass();
        String typeDescriptor = type.descriptorString();
        if(isWrapper(type)) {
            return new AtomicValueObservation(typeDescriptor, value);
        }
        if(type == String.class) {
            return new StringObservation((String)value);
        }
        if(type.isArray()) {
            return new ArrayObservation(type.descriptorString(), Array.getLength(value));
        }
        if(isCollection(type)) {
            new CollectionObservation(typeDescriptor,  getCollectionSize(value));
        }
        return new ObjectObservation(typeDescriptor);
    }

    protected int getCollectionSize(Object value) {
        if(value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        else if(value instanceof Map) {
            return ((Map<?,?>) value).size();
        }
        throw new AssertionError("Trying to get the size of a non-collection type instance.");
    }

    public ExceptionObservation observeException(Throwable exc) {
        Objects.requireNonNull(exc, "Exception to observe can not be null");
        return new ExceptionObservation(exc.getClass().descriptorString(), exc.getMessage());
    }

    protected FieldObservation[] observeFieldsFrom(Object value) {
        return fieldsOf(value.getClass())
                .filter(field -> !(field.isSynthetic() || field.getName().contains("$")))
                .map(field -> {
                    if(!field.canAccess(value)){
                        field.setAccessible(true);
                    }
                    ValueObservation fieldValueObservation;
                    try {
                        fieldValueObservation = shallowObserve(field.get(value));
                    }
                    catch (IllegalAccessException exc) {
                        fieldValueObservation = observeException(exc);
                    }
                    return new FieldObservation(
                            field.getDeclaringClass().descriptorString(),
                            field.getName(),
                            field.getType().descriptorString(),
                            fieldValueObservation);
                })
                .toArray(FieldObservation[]::new);
    }

    public ValueObservation observeValue(Object value) {
        if(value == null) {
            return null;
        }
        ValueObservation observation = shallowObserve(value);
        if(observation instanceof ObjectObservation) {
            ((ObjectObservation) observation).setFields(observeFieldsFrom(value));
        }
        return observation;
    }

    public StaticTypeObservation observe(Class<?> type, Object value) {
        return new StaticTypeObservation(type.descriptorString(), observeValue(value));
    }

    public MethodInvocationObservation observeInvocationAndTrigger(String method, int line,
                                                                   Class<?> receiverType, Object receiver,
                                                                   Class<?> resultType, Object result,
                                                                   Class<?>[] parameters, Object[] arguments,
                                                                   Throwable exc) {
        MethodInvocationObservation observation = observeInvocation(method, line, receiverType, receiver, resultType, result, parameters, arguments, exc);
        observation.trigger = observeTrigger();
        return observation;
    }

    public MethodInvocationObservation observeInvocation(String method, int line,
                                                         Class<?> receiverType, Object receiver,
                                                         Class<?> resultType, Object result,
                                                         Class<?>[] parameters, Object[] arguments,
                                                         Throwable exc) {

        MethodInvocationObservation observation = new MethodInvocationObservation(method, line);
        if(receiver != null) {
            // Method is not static
            observation.receiver = observe(receiverType, receiver);
        }
        if(resultType != Void.class && resultType != void.class) {
            // Method returns a value
            observation.result = observe(resultType, result);
        }
        if (parameters != null) {
            StaticTypeObservation[] argumentObservations = new StaticTypeObservation[parameters.length];
            for (int index = 0; index < arguments.length; index++) {
                if (parameters[index].isPrimitive()) continue;
                argumentObservations[index] = observe(parameters[index], arguments[index]);
            }
            observation.arguments = argumentObservations;
        }
        if(exc != null) {
            observation.exception = observeException(exc);
        }
        return observation;
    }

}
