package eu.stamp_project.reneri.instrumentation;

import spoon.SpoonException;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.support.visitor.ProcessingVisitor;

import java.util.Optional;
import java.util.Set;

import static eu.stamp_project.reneri.TestClassFinder.getAnnotation;

public class ObserverClassProcessor extends AbstractProcessor<CtClass<?>> {

    private final static String OWN_PACKAGE = ObservationAttacherProcessor.class.getPackage().getName();

    private Set<CtClass<?>> typesToTransform;

    public ObserverClassProcessor() {
        this.typesToTransform = null; // Means all types shall be transformed
    }

    public ObserverClassProcessor(Set<CtClass<?>> typesToTransform) {
        this.typesToTransform = typesToTransform;
    }

    @Override
    public boolean isToBeProcessed(CtClass<?> element) {

        if(element.getQualifiedName().startsWith(OWN_PACKAGE)) {
            // Preventing self analysis
            return false;
        }

        try {
            return (typesToTransform == null) || typesToTransform.contains(element);
        }
        catch(SpoonException exc) {
            // TODO: Log this as a warning
            return false; // Some classes throw an exception when inspecting their methods.
        }
    }

    @Override
    public void process(CtClass<?> element) {
        //TODO: Use the log mechanism instead of printing to the standard ouput
        System.out.println("Processing " + element.getQualifiedName());
        for (CtMethod<?> method : element.getMethods()) {
            turnOffTimeout(method);
            ObservationAttacherProcessor.processMethod(method);
        }
    }

    private void turnOffTimeout(CtMethod<?> method) {
        // We observe only functional differences so we turn off timeout
        Optional<CtAnnotation<?>> annotation = getAnnotation(method, "org.junit.Test");
        if(!annotation.isPresent()) {
            return; // Not marked as a test
        }
        CtAnnotation<?> testAnnotation = annotation.get();
        testAnnotation.addValue("timeout", getFactory().createLiteral(0L));
    }

    protected void insertObservationPoints(CtMethod<?> method) {
        CtBlock<?> body = method.getBody();
        if (body == null) {
            return;
        }
        ObservationAttacherProcessor processor = new ObservationAttacherProcessor(method);
        ProcessingVisitor visitor = new ProcessingVisitor(getFactory());
        visitor.setProcessor(processor);
        body.accept(visitor);
        processor.processingDone();
    }

}
