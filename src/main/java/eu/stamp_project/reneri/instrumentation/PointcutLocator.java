package eu.stamp_project.reneri.instrumentation;

import eu.stamp_project.reneri.datastructures.Trie;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.support.visitor.ProcessingVisitor;

import java.util.HashMap;
import java.util.Map;

public class PointcutLocator extends AbstractProcessor<CtClass<?>> {

    private final Map<String, SourcePosition> pointLocations = new HashMap<>();

    private final Map<String, String> pointTypes = new HashMap<>();

    public Map<String, SourcePosition> getLocations() {
        return pointLocations;
    }

    public Map<String, String> getTypes() { return pointTypes; }

    @Override
    public void process(CtClass<?> element) {
        for(CtMethod<?> method : element.getMethods()) {
            ProcessingVisitor visitor = new ProcessingVisitor(getFactory());
            visitor.setProcessor(new ExpressionProcessor(method) {
                @Override
                protected void processExpression(String point, CtExpression<?> expression) {
                    pointLocations.put(point, expression.getPosition());
                    pointTypes.put(point, getActualType(expression).getQualifiedName());
                }
            });

            CtBlock<?> body = method.getBody();
            if(body!= null) {
                body.accept(visitor);
            }
        }
    }
}
