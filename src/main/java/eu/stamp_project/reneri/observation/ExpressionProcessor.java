package eu.stamp_project.reneri.observation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;



public abstract class ExpressionProcessor extends AbstractProcessor<CtExpression<?>> {

    //private CtMethod<?> targetMethod;

    private int expressionCounter = 0;

    private Set<CtTypeReference<?>> typesToAvoid;

    private String baseObservationPoint;

    public ExpressionProcessor(CtMethod<?> method) {
        setBaseObservationPoint(method);
        setFactory(method.getFactory());
        setTypesToAvoid();
    }

    private void setBaseObservationPoint(CtMethod<?> method) {
        baseObservationPoint = String.format("%s|%s", method.getDeclaringType().getQualifiedName(), method.getSignature());
    }

    private void setTypesToAvoid() {
        TypeFactory factory = getFactory().Type();

        typesToAvoid = new HashSet<>();
        typesToAvoid.add(factory.VOID);
        typesToAvoid.add(factory.VOID_PRIMITIVE);
        typesToAvoid.add(factory.NULL_TYPE);
    }

    @Override
    public boolean isToBeProcessed(CtExpression<?> element) {
        return canProcessASTNode(element) && canProcessType(getActualType(element));
    }

    private boolean canProcessType(CtTypeReference type) {
        return type != null &&
                !type.getQualifiedName().equals("?") && // Yep, unresolved type references are named as ? and I haven't found a better way to query this
                !isDubiousGenericitResolution(type) &&
                !type.isGenerics() &&
                !type.isAnonymous() &&
                !typesToAvoid.contains(type);
    }

    private boolean isDubiousGenericitResolution(CtTypeReference reference) {

        // TODO: Turns out that Spoon is not getting generic types very well.
        // The type I'm getting is SomeType<Object> while it should be SomeType<String>
        // therefore, if we get an expression where the type argument is object
        // it could be a bad inference so we skip them.
        // This leaves out a number of expressions but at least ensures that the instrumented code will compile.
        List<CtTypeReference<?>> arguments = reference.getActualTypeArguments();
        CtTypeReference<?> OBJECT = getFactory().Type().OBJECT;
        return arguments.stream().anyMatch(arg -> arg.equals(OBJECT));

    }

    private boolean canProcessASTNode(CtExpression<?> node) {
        if(node.getParent() instanceof CtAssignment) {
            // An assignment is an expression
            // and so it is the left side
            // processing the left part leads to compile errors.
            // We process the assignment as a whole.
            return false;
        }
        return !Stream.of(
                CtLiteral.class,
                CtSuperAccess.class,
                CtThisAccess.class,
                CtTypeAccess.class,
                CtAnnotation.class
        ).anyMatch(type -> type.isInstance(node));
    }

    private String getPointID() {
        return String.format("%s|%d", baseObservationPoint, expressionCounter++);
    }

    @Override
    public void process(CtExpression<?> element) {
        processExpression(getPointID(), element);
    }

    protected abstract void processExpression(String point, CtExpression<?> expression);


    protected CtTypeReference<?> getActualType(CtExpression<?> expression) {

        //ARRGHH SPOON!!!
        List<CtTypeReference<?>> casts = expression.getTypeCasts();

        if(casts.isEmpty()) {
            return expression.getType();
        }

        return casts.get(0);
    }


}