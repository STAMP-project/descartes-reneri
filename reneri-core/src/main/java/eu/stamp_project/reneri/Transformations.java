package eu.stamp_project.reneri;

import eu.stamp_project.mutationtest.descartes.DescartesEngineFactory;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.EngineArguments;
import org.pitest.mutationtest.engine.*;

import java.util.Optional;
import java.util.function.BiFunction;

public class Transformations {

    private Transformations() {}

    public static BiFunction<String, byte[], byte[]> descartes(String methodName, String methodDesc, String declaringClass, String operator) {
        return (name, code) -> {
            DescartesEngineFactory factory = new DescartesEngineFactory();
            MutationEngine engine = factory.createEngine(EngineArguments.arguments());

            ClassByteArraySource source = (className) -> {
                if(className.equals(declaringClass)) {
                    return Optional.of(code);
                }
                return Optional.empty();
            };

            MutationIdentifier identifier = new MutationIdentifier(
                    new Location(ClassName.fromString(declaringClass), MethodName.fromString(methodName), methodDesc),
                    0, operator
            );

            Mutater mutater = engine.createMutator(source);
            Mutant mutant = mutater.getMutation(identifier);

            return mutant.getBytes();
        };

    }
}
