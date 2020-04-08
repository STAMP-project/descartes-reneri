package eu.stamp_project.reneri;

import eu.stamp_project.reneri.instrumentation.Instrumenter;
import eu.stamp_project.reneri.observations.Observer;
import eu.stamp_project.reneri.observations.Probe;
import eu.stamp_project.reneri.observations.TriggerObservation;
import javassist.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class DynamicTriggerCollector implements TriggerCollector {

    private Consumer<Class<?>> executor;

    public DynamicTriggerCollector(Consumer<Class<?>> executor) {
        this.executor = executor;
    }

    @Override
    public Set<TriggerObservation> collectTriggers(String methodName, String methodDesc, String declaringClassName, String testClassName) throws NotFoundException, IOException {
        try {
            ClassPool pool = new ClassPool(true);

            CtClass declaringClass = pool.getCtClass(declaringClassName);
            CtMethod method = declaringClass.getMethod(methodName, methodDesc);

            new Instrumenter().instrumentForTrigger(method);

            Loader.Simple loader = new Loader.Simple();
            loader.invokeDefineClass(declaringClass);
            Class<?> testClass = loader.invokeDefineClass(pool.getCtClass(testClassName));

            // Probe to collect triggers
            HashSet<TriggerObservation> triggers = new HashSet<>();
            Probe.init(
                    new Observer(testClass),
                    (observation) -> triggers.add((TriggerObservation) observation)
            );

            executor.accept(testClass);
            return triggers;
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("Instrumentation error", exc);
        }
    }
}
