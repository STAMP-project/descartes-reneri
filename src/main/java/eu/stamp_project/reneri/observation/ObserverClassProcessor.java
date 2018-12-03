package eu.stamp_project.reneri.observation;

import org.apache.commons.lang3.builder.ToStringExclude;
import spoon.SpoonException;
import spoon.processing.AbstractProcessor;
import spoon.processing.Property;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.support.visitor.ProcessingVisitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static eu.stamp_project.reneri.TestClassFinder.getAnnotation;

public class ObserverClassProcessor extends AbstractProcessor<CtClass<?>> {

    @Property
    String reportPath = "";

    @Property
    long timeoutFactor = 2;

    @Property
    Set<CtClass<?>> typesToTransform;

    private ObserverTemplate template = new ObserverTemplate();

    private Set<String> processedMethods = new HashSet<>();


    Predicate<CtClass<?>> isCandidate = (type) -> true;

    public ObserverClassProcessor() {
        this.typesToTransform = null; // Means all types shall be transformed
    }

    public ObserverClassProcessor(Set<CtClass<?>> typesToTransform) {
        this.typesToTransform = typesToTransform;
    }


    @Override
    public boolean isToBeProcessed(CtClass<?> element) {

        System.out.println("Checking " + element.getQualifiedName());

        try {
            return (typesToTransform == null) || typesToTransform.contains(element);
        }
        catch(SpoonException exc) {
            // TODO: Log this as a warning
            return false; // Some classes throw an exception when inspecting their methods.
        }
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    @Override
    public void process(CtClass<?> element) {
        System.out.println("Processing " + element.getQualifiedName());
        CtClass<?> observerClass = getObserverClass(element);
        for (CtMethod<?> method : element.getMethods()) {
            increaseTimeoutIfNeeded(method);
            insertObservationPoints(observerClass, method);
        }
    }

    protected void increaseTimeoutIfNeeded(CtMethod<?> method) {
        Optional<CtAnnotation<?>> annotation = getAnnotation(method, "org.junit.Test");
        if(!annotation.isPresent()) {
            return; // Not marked as a test
        }
        CtAnnotation<?> testAnnotation = annotation.get();
        CtExpression value = testAnnotation.getValue("timeout");


        if(value == null) {
            return; // Does not specify a timeout
        }
        value = getFactory().createBinaryOperator(getFactory().createLiteral(timeoutFactor), value, BinaryOperatorKind.MUL);
        testAnnotation.addValue("timeout", value);
    }

    protected void insertObservationPoints(CtClass<?> observerClass, CtMethod<?> method) {
        ExpressionProcessor processor = new ExpressionProcessor(observerClass, method);
        processor.setFactory(getFactory());
        ProcessingVisitor visitor = new ProcessingVisitor(getFactory());
        visitor.setProcessor(processor);
        method.getBody().accept(visitor);
        processor.processingDone();
    }

    protected CtClass<?> getObserverClass(CtClass<?> container) {
        final String observerName = "___stamp__dynamic__observer___";
        CtClass<?> observerClass = container.getNestedType(observerName);
        if(observerClass != null)
            return observerClass;
        observerClass = getFactory().createClass(container, observerName);
        observerClass.setModifiers(Collections.singleton(ModifierKind.STATIC));
        //template.baseFilePath = reportPath;
        template.apply(observerClass);
        return observerClass;
    }

}
