package eu.stamp_project.reneri.instrumentation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
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

        /* TODO: Turns out that Spoon is not getting generic types very well.
        The type I'm getting is SomeType<Object> while it should be SomeType<String>
        therefore, if we get an expression where the type argument is object
        it could be a bad inference so we skip them.
        This leaves out a number of expressions but at least ensures that the instrumented code will compile.
        */
        List<CtTypeReference<?>> arguments = reference.getActualTypeArguments();
        CtTypeReference<?> OBJECT = getFactory().Type().OBJECT;
        return arguments.stream().anyMatch(arg -> arg.equals(OBJECT));

    }

    private boolean canProcessASTNode(CtExpression<?> node) {

        CtElement parent = node.getParent();

        // Assignments
        if(parent instanceof CtAssignment || parent instanceof  CtUnaryOperator) {
            /*
            An assignment is an expression and so it is the left side.
            Processing the left part leads to compile errors.
            We process the assignment as a whole.
            Could have been done using CtRole.
            */

            // There is no need to observe a subexpression of unary operators.
            return false;
        }

        // Unary operators
        if(node instanceof CtUnaryOperator) {
            UnaryOperatorKind operator = ((CtUnaryOperator)node).getKind();
            // Don't observe post-(increment|decrement) operators, as their semantic can be disrupted
            return !(operator.equals(UnaryOperatorKind.POSTDEC) || operator.equals(UnaryOperatorKind.POSTINC));
        }

        // Class literals
        if(node instanceof CtFieldRead) {
            CtFieldRead read  = (CtFieldRead)node;
            return !(read.getTarget() instanceof CtTypeAccess && read.getVariable().getSimpleName().equals("class"));
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
        /*
        If the expression has been casted, then return the outermost type used in the cast sequence
         */
        List<CtTypeReference<?>> casts = expression.getTypeCasts();
        return (casts.isEmpty())?expression.getType() : casts.get(0);
    }


}