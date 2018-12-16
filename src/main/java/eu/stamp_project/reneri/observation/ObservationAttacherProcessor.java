package eu.stamp_project.reneri.observation;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;


public class ObservationAttacherProcessor extends ExpressionProcessor {

    private CtType<StateObserver> observerClass;

    private CtTypeAccess<StateObserver> observerClassAccess;

    private CtMethod<?> defaultObserverMethod;

    public ObservationAttacherProcessor(CtMethod<?> method) {
        super(method);

        setObserverClass();
        setDefaultObserverMethod();
    }

    private void setObserverClass() {
        Factory factory = getFactory();
        observerClass = factory.Type().get(StateObserver.class.getName());
        observerClassAccess = factory.createTypeAccess(observerClass.getReference());
    }

    private void setDefaultObserverMethod() {
        Optional<CtMethod<?>> observer = getUniqueMethod("observeState");
        if(!observer.isPresent()) {
            throw new AssertionError("StateObserver class does not contain a default observer method");
        }

        defaultObserverMethod = observer.get();
    }

    private Optional<CtMethod<?>> getUniqueMethod(String name) {
        List<CtMethod<?>> methods = observerClass.getMethodsByName(name);
        if(methods.size() > 1) {
            throw new AssertionError("There is more than one method named " + name + " in the StateObserver class. Name: " + name);
        }
        if(methods.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(methods.get(0));
    }


    private String typeToMethodName(CtTypeReference<?> type) {
        return "observe_" + type.getQualifiedName().replace('.', '_');
    }

    private Optional<CtMethod<?>> getStaticObserver(CtTypeReference<?> type) {
        return getUniqueMethod(typeToMethodName(type));
    }

    private CtExpression<?> createObservation(String point, CtExpression<?> expression) {
        Factory factory = getFactory();
        CtLiteral<String> observationPoint = factory.createLiteral(point);
        CtTypeReference<?> type = getActualType(expression);
        Optional<CtMethod<?>> staticObserver = getStaticObserver(type);

        if(staticObserver.isPresent()) {
            return factory.createInvocation(observerClassAccess, staticObserver.get().getReference(), observationPoint, expression.clone());
        }

        CtInvocation<?> result = factory.createInvocation(
                observerClassAccess,
                defaultObserverMethod.getReference(),
                observationPoint,
                factory.createClassAccess(type),
                expression.clone()
        );

        result.addActualTypeArgument(type);
        return result;
    }

    @Override
    protected void processExpression(String point, CtExpression<?> expression) {
       expression.replace(createObservation(point, expression));
    }
}
