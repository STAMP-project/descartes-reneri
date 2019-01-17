package eu.stamp_project.reneri.instrumentation;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.support.visitor.ProcessingVisitor;

public class PointcutLocator extends AbstractProcessor<CtClass<?>> {

    private final Trie<SourcePosition> pointLocations = new Trie<>();

    public Trie<SourcePosition> getLocations() {
        return pointLocations;
    }

    @Override
    public void process(CtClass<?> element) {
        for(CtMethod<?> method : element.getMethods()) {
            ProcessingVisitor visitor = new ProcessingVisitor(getFactory());
            visitor.setProcessor(new ExpressionProcessor(method) {
                @Override
                protected void processExpression(String point, CtExpression<?> expression) {
                    pointLocations.add(point, expression.getPosition());
                }
            });

            CtBlock<?> body = method.getBody();
            if(body!= null) {
                body.accept(visitor);
            }
        }
    }
}
