package eu.stamp_project.reneri.instrumentation;

import eu.stamp_project.reneri.TestClassFinder;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.visitor.ProcessingVisitor;

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

    private CtMethod<?> getStaticObserver(CtTypeReference<?> type) {
        return observerClass.getMethod(type, "observe", getFactory().Type().STRING, type);

    }

    private CtTypeReference<?> getTypeForObservationInvocation(CtExpression<?> expression) {

        CtTypeReference<?> type = getActualType(expression).clone();
        /*
        Type inference is done correctly for expressions with implicit types.
        However, for such expressions, for example:

        HashMap<String, String> map = new HashMap<>();

        the reference to the type of the right hand has all parameters as implicit
        this impacts the PrettyPrinter, which is not context sensitive and does not print the types
        even when it knows which types are involved.
        The workaround is to clone the reference and set all parameters as non-implicit.
        */
        for(CtTypeReference<?> typeArguments: type.getActualTypeArguments()) {
            typeArguments.setImplicit(false);
        }
        return type;
    }

    private CtExpression<?> createObservation(String point, CtExpression<?> expression) {
        Factory factory = getFactory();
        CtLiteral<String> observationPoint = factory.createLiteral(point);
        CtTypeReference<?> type = getTypeForObservationInvocation(expression);
        CtMethod<?> staticObserver = getStaticObserver(type);

        // Wrappers and primitive values are handled by static observers
        if(staticObserver != null) {
            return factory.createInvocation(observerClassAccess, staticObserver.getReference(), observationPoint, expression.clone());
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

    @Override
    public void processingDone() {
        attachExceptionObservation();
    }

    private void attachExceptionObservation() {
        CtMethod<?> method = getMethod();
        Optional<CtTypeReference<?>> expectedException = getExpectedException(method);
        if(!expectedException.isPresent()) {
            return;
        }
        observeException(method, expectedException.get());
    }

    private Optional<CtTypeReference<?>> getExpectedException(CtMethod<?> method) {
        Optional<CtAnnotation<?>> annotation = TestClassFinder.getAnnotation(method, "org.junit.Test");
        if(!annotation.isPresent()) {
            return Optional.empty();
        }
        Map<String, CtExpression> values = annotation.get().getValues();
        CtExpression expectedExpression = values.getOrDefault("expected", null);
        if(expectedExpression == null) {
            return Optional.empty();
        }
        if(!(expectedExpression instanceof CtFieldRead)) {
            return Optional.empty();
        }
        CtFieldRead fieldRead = (CtFieldRead)expectedExpression;
        if(!(fieldRead.getVariable().getSimpleName().equals("class") && fieldRead.getTarget() instanceof CtTypeAccess)) {
            return Optional.empty();
        }
        return Optional.of(((CtTypeAccess)fieldRead.getTarget()).getAccessedType());
    }

    private void observeException(CtMethod<?> method, CtTypeReference<?> exceptionType) {
        Factory factory = getFactory();
        CtMethod<?> observeExceptionMethod = getUniqueMethod("observeThrownException").get();
        CtCatchVariable catchVariable = factory.createCatchVariable(exceptionType, "a" + System.currentTimeMillis());
        CtVariableAccess accessToException = factory.createVariableRead(catchVariable.getReference(), false);

        CtBlock catchCode = factory.createBlock();
        catchCode
                .addStatement(factory.createInvocation(
                        observerClassAccess,
                        observeExceptionMethod.getReference(),
                        factory.createLiteral(getBaseObservationPoint() + "|!"),
                        accessToException.clone()))
                .addStatement(factory.createThrow().setThrownExpression(accessToException.clone()));

        CtCatch catchBlock = factory.createCatch();
        catchBlock.setParameter(catchVariable).setBody(catchCode);

        method.setBody(factory.createTry().addCatcher(catchBlock).setBody(method.getBody()));
    }

    /*
    So, the name of the method below is processMethod when process could have been a shorter nicer name.
    The point is that it can not be process. The reason goes back to the way Spoon deals with genericity
    in the AbstractProcessor class. In order to have access at runtime, the constructor of this class
    searches for all methods named, guess how, process that have only one parameter and stores the type
    of this parameter. Then, ProcessingVisitor uses the stored types to know which elements to process.
    An element can be processed if its type can be assigned to all the types stored by the Processor.
    In this case, we had process(CtExpression) and process(CtMethod) and there is no class that could be
    assigned to both, so no element was processed. ARRGGGGGHHHH Spoon!!!
     */
    public static void processMethod(CtMethod<?> method) {
        CtBlock<?> body = method.getBody();
        if (body == null) {
            return;
        }
        ObservationAttacherProcessor processor = new ObservationAttacherProcessor(method);
        ProcessingVisitor visitor = new ProcessingVisitor(method.getFactory());
        visitor.setProcessor(processor);
        body.accept(visitor);
        processor.processingDone();
    }

}
