package eu.stamp_project.reneri.instrumentation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

//TODO: Consider refactor as a Visitor

public abstract class ExpressionProcessor extends AbstractProcessor<CtExpression<?>> {



    //private CtMethod<?> targetMethod;

    private int expressionCounter = 0;

    private Set<CtTypeReference<?>> typesToAvoid;

    private String baseObservationPoint;

    private CtMethod<?> method;

    public ExpressionProcessor(CtMethod<?> method) {

        this.method = method;
        baseObservationPoint = String.format("%s|%s", method.getDeclaringType().getQualifiedName(), method.getSignature());
        setFactory(method.getFactory());
        setTypesToAvoid();
    }

    protected String getBaseObservationPoint() {
        return baseObservationPoint;
    }

    protected CtMethod<?> getMethod() {
        return method;
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
                !isDubiousGenericityResolution(type) &&
                !type.isGenerics() &&
                !type.isAnonymous() &&
                !typesToAvoid.contains(type);
    }

    private boolean isDubiousGenericityResolution(CtTypeReference reference) {

        /* Generic types are not always correctly resolved. For example:
        sometimes the expression has type SomeType<Object> while it should be SomeType<String>
        therefore, if we get an expression where the type argument is object
        it could be a bad inference so we skip it.
        This leaves out a number of expressions but at least ensures that the instrumented code will compile.
        */
        List<CtTypeReference<?>> arguments = reference.getActualTypeArguments();
        CtTypeReference<?> OBJECT = getFactory().Type().OBJECT;
        return arguments.stream().anyMatch(arg -> arg.equals(OBJECT));

    }

    private boolean isInAssignment(CtExpression<?> node) {
        /*
        An assignment is an expression and so it is the left side.
        Processing the left part leads to compile errors.
        We process the assignment as a whole.
        Could have been done using CtRole.
        */

        return node.getParent() instanceof CtAssignment;
    }

    private boolean isBeingIncrementedOrDecremented(CtExpression<?> node) {
        /*
        If an expression is being incremented or decremented using the corresponding
        unary operators, then the expression is also a left-value and therefore should
        be skipped.
        */
        CtElement parent = node.getParent();
        if(!(parent instanceof CtUnaryOperator)) {
            return false;
        }

        CtUnaryOperator operator = (CtUnaryOperator)parent;

        return EnumSet.of(
                UnaryOperatorKind.POSTDEC,
                UnaryOperatorKind.POSTINC,
                UnaryOperatorKind.PREDEC,
                UnaryOperatorKind.PREINC
        ).contains(operator.getKind());
    }

    private boolean isClassLiteral(CtExpression<?> node) {
        if(node instanceof CtFieldRead) {
            CtFieldRead read  = (CtFieldRead)node;
            return (read.getTarget() instanceof CtTypeAccess && read.getVariable().getSimpleName().equals("class"));
        }
        return false;
    }

    private boolean isStaticFieldExpressionInInnerClass(CtExpression<?> node) {
        /*
        * Static fields in innner classes are required to be compile-time constant expressions
        * therefore we should not transform them with a method call and they are of no interest.
        * */
        CtElement parent = node.getParent();
        if(!(parent instanceof CtField)) {
            return false;
        }
        CtField fieldDeclaration = (CtField)parent;
        return fieldDeclaration.getModifiers().contains(ModifierKind.STATIC) && !fieldDeclaration.getDeclaringType().isTopLevel();
    }

    private boolean isBannedNode(CtExpression<?> node) {
        return Stream.of(
                CtLiteral.class,
                CtSuperAccess.class,
                CtThisAccess.class,
                CtTypeAccess.class,
                CtAnnotation.class,
                CtAnnotationFieldAccess.class,
                CtLambda.class,
                // Not observing unary operators, as their semantic is the same always (no operator overload)
                // also the semantics of post-(increment|decrement) operators can be disrupted
                CtUnaryOperator.class, //TODO: But pre-(inc|dec) could be observed
                // Same goes for binary operators
                CtBinaryOperator.class
        ).anyMatch(type -> type.isInstance(node));
    }

    private boolean canProcessASTNode(CtExpression<?> node) {
        return !(isInAssignment(node) ||
                isClassLiteral(node) ||
                isStaticFieldExpressionInInnerClass(node) ||
                isBeingIncrementedOrDecremented(node) ||
                isBannedNode(node));
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