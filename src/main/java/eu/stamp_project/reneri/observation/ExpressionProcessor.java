package eu.stamp_project.reneri.observation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.*;
import spoon.support.SpoonClassNotFoundException;
import spoon.template.Substitution;
import spoon.template.Template;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ExpressionProcessor extends AbstractProcessor<CtExpression<?>> {


    private Template<?> template = new ObserverTemplate();
    private CtType<?> templateClass;

    private CtType<?> observerClass;
    private CtMethod<?> targetMethod;
    private int expressionCounter = 0;

    private Set<CtTypeReference<?>> typesToAvoid;

    //private Set<CtTypeReference<?>> observedTypes = new HashSet<>();

    public ExpressionProcessor(CtType<?> observerClass, CtMethod<?> method) {
        this.observerClass = observerClass;
        this.targetMethod = method;

        setFactory(observerClass.getFactory());

        // Fill types to avoid
        TypeFactory factory = new TypeFactory(getFactory());
        typesToAvoid = new HashSet<>();
        typesToAvoid.add(factory.VOID);
        typesToAvoid.add(factory.VOID_PRIMITIVE);
        typesToAvoid.add(factory.NULL_TYPE);

        templateClass = getFactory().Class().get(template.getClass());
    }

    @Override
    public boolean isToBeProcessed(CtExpression<?> element) {
        //Exclude simple expressions
        if(Stream.of(CtLiteral.class, CtSuperAccess.class, CtThisAccess.class, CtTypeAccess.class, CtAnnotation.class)
                .anyMatch(type -> type.isInstance(element)))
            return false;

        CtTypeReference type = element.getType(); //TODO: Anonymous types
        return type != null && !typesToAvoid.contains(type);
    }

    @Override
    public void process(CtExpression<?> element) {

        CtTypeReference type = element.getType();

        // TODO: Turns out that Spoon is not getting generic types very well.
        // The type I'm getting is SomeType<Object> while it should be SomeType<String>

        if(type.getActualTypeArguments().size() >= 1) return;

        Factory factory = getFactory();

        CtExpression<?> replacement = getFactory().createInvocation(
                factory.createTypeAccess(observerClass.getReference()),
                getMethodFor(type).getReference(),
                factory.createLiteral(Integer.toString(expressionCounter++)),
                element.clone());
        element.replace(replacement);

    }

    @Override
    public void processingDone() {
        addTopEnter();
        //purgeNotObserved(); //Can't purge as each targetMethod is processed in different times. Should copy only when used.
    }

    protected void addTopEnter() {
        Factory factory = getFactory();
        CtTypeAccess observer = factory.createTypeAccess(observerClass.getReference());
        CtExecutableReference topenter = observerClass.getMethod("topenter", factory.Type().STRING).getReference();
        targetMethod.getBody().addStatement(0, factory.createInvocation(observer, topenter, factory.createLiteral(targetMethod.getSimpleName())));
    }

//    protected void purgeNotObserved() {
//        Set<String> methodsOfInterest = observedTypes.stream().map(this::getMethodNameFor).collect(Collectors.toSet());
//        observerClass.getMethods().stream()
//                .filter(method -> method.getSimpleName().startsWith("observe_") && !methodsOfInterest.contains(method.getSimpleName()))
//                .forEach(CtMethod::delete);
//    }

    protected CtMethod<?> getUniqueMethodFrom(CtType<?> type, String name) {
        List<CtMethod<?>> candidates = type.getMethodsByName(name);
        if(candidates.size() > 1) {
            throw new AssertionError(String.format("Method %s is expected to be unique in type %s", name, type.getQualifiedName()));
        }
        if(candidates.size() == 1) {
            return candidates.get(0);
        }
        return null;
    }

    protected CtMethod<?> getMethodFor(CtTypeReference<?> valueType) {

        //observedTypes.add(valueType);

        final String methodName = getMethodNameFor(valueType);
        CtMethod<?> method = getUniqueMethodFrom(observerClass, methodName);
        if(method != null) {
            // The method has been already added to the observer class
            return method;
        }

        method = getUniqueMethodFrom(templateClass, methodName);
        if(method != null) {
            // The method is being added from the template class
            return Substitution.insertMethod(observerClass, template, method);
        }


        // Create a new method for the given type
        return createMethodFor(valueType);
    }

    protected CtMethod<?> createMethodFor(CtTypeReference<?> valueType) {

        Factory factory = getFactory();
        CtMethod method = factory.createMethod();
        method.addModifier(ModifierKind.PUBLIC).addModifier(ModifierKind.STATIC);
        method.setSimpleName(getMethodNameFor(valueType));
        method.setType(valueType);

        CtTypeReference<?> STRING = factory.Type().STRING;
        CtTypeReference<?> OBJECT = factory.Type().OBJECT;

        CtParameterReference<?> pointParam = factory.createParameter(method, STRING, "point").getReference();
        CtVariableRead<?> point = (CtVariableRead<?>)factory.createVariableRead(pointParam, false); //o_O
        CtParameterReference<?> valueParam = factory.createParameter(method, valueType, "value").getReference();
        CtVariableRead<?> value = (CtVariableRead<?>)factory.createVariableRead(valueParam, false); // o_O
        CtTypeAccess<?> observer = factory.createTypeAccess(observerClass.getReference());
        CtExecutableReference<?> observeNull = observerClass.getMethod("observeNull", STRING, OBJECT).getReference();

        CtBlock<?> body = factory.createBlock();
        // observeNull(<point>, value)
        body.addStatement(factory.createInvocation(observer, observeNull, point, value));

        final AtomicInteger pointCounter = new AtomicInteger();
        CtIf ifNotNull = factory.createIf()
                //if(value != null)
                .setCondition(factory.createBinaryOperator(value, factory.createLiteral(null), BinaryOperatorKind.NE))
                .setThenStatement(factory.createBlock().setStatements(
                        getObservationPoints(value).map(watch -> {
                            CtTypeReference<?> watchType = watch.getType();
                            CtExecutableReference<?> observe = isOriginalType(watchType)?getMethodFor(watchType).getReference():observeNull;
                            // observe_<type>(<pointCounter>, <watch>)
                            // or
                            // observeNull(<pointCounter>, <watch>)
                            return factory.createInvocation(observer, observe, factory.createLiteral(Integer.toString(pointCounter.getAndIncrement())), watch);
                        }).collect(Collectors.toList()))
                        //enter(<point>) //At the begining of the then instruction
                        .addStatement(0, factory.createInvocation(observer, observerClass.getMethod("enter", STRING).getReference(), point))
                        //exit()
                        .addStatement(factory.createInvocation(observer, observerClass.getMethod("exit").getReference())));

        if(pointCounter.get() > 0) {
            //If there are actual observation points
            body.addStatement(ifNotNull);
        }

        // return value
        body.addStatement(((CtReturn)factory.createReturn()).setReturnedExpression(value));
        method.setBody(body);
        observerClass.addMethod(method);
        return method;
    }

    protected boolean isOriginalType(CtTypeReference<?> valueType) {
        return valueType.getQualifiedName().equals("java.lang.String") || valueType.isPrimitive() || valueType.unbox().isPrimitive();
    }

    protected String getMethodNameFor(CtTypeReference<?> type) {
        return "observe_" + type.getQualifiedName()
                .replace('.', '_')
                .replace("[]", "__array__");
    }

    private boolean isArray(CtTypeReference<?> type) {
        return type.getSimpleName().contains("[]"); // Hacky way to know whether the type is an array
    }

    protected Stream<CtExpression<?>> getObservationPoints(CtVariableRead<?> value) {
        Factory factory = getFactory();
        return Stream.concat(
                getObservableFields(value.getType()).map(
                        (CtFieldReference<?> field) -> {
                            CtFieldRead<?> fieldRead = factory.createFieldRead();
                            fieldRead.setTarget(value);
                            fieldRead.setVariable((CtFieldReference)field);
                            return fieldRead;
                        }),
                getObservableGetters(value.getType()).map(getter ->
                        factory.createInvocation(value, getter.getReference())
                )
        );
    }

    protected Stream<CtFieldReference<?>> getObservableFields(CtTypeReference<?> valueType) {
        Predicate<CtFieldReference<?>> condition = field -> field.getModifiers().contains(ModifierKind.PUBLIC);

        if(inSamePackage(valueType))
            condition = field -> {
                Set<ModifierKind> modifiers = field.getModifiers();
                return modifiers.contains(ModifierKind.PUBLIC) || modifiers.contains(ModifierKind.PROTECTED);
            };
        return valueType.getDeclaredFields().stream().filter(condition);
    }

    //TODO: Observing these methods may cause a test failure, deal with it
    protected Stream<CtMethod<?>> getObservableGetters(CtTypeReference<?> valueType) {
        try {
            CtType<?> actualType = valueType.getTypeDeclaration();
            if (actualType == null) //Can't observe the methods
                return Stream.empty();
            Predicate<CtMethod<?>> condition = method -> isGetter(method)  && method.hasModifier(ModifierKind.PUBLIC);
            if (inSamePackage(valueType))
                condition = method -> isGetter(method) &&
                        (method.hasModifier(ModifierKind.PUBLIC) || method.hasModifier(ModifierKind.PROTECTED));
            return valueType.getTypeDeclaration().getMethods().stream().filter(condition);
        } catch(SpoonClassNotFoundException  exc) {
            //Sometimes this happens even when the class has been loaded
            return Stream.empty();
        }
    }

    protected boolean inSamePackage(CtTypeReference<?> valueType) {
        CtPackageReference valuePackage = valueType.getPackage();
        if(valuePackage == null)
            return false;
        CtPackage observerPackage = observerClass.getPackage();
        if(observerPackage == null)
            return false;
        return valuePackage.getQualifiedName().equals(observerPackage.getQualifiedName());
    }

    protected boolean isGetter(CtMethod<?> method) {
        // Getters by convention
        return method.getParameters().size() == 0 && (method.getSimpleName().startsWith("get") || method.getSimpleName().startsWith("is"));
    }

}