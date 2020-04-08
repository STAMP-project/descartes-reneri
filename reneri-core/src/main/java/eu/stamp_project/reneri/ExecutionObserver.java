package eu.stamp_project.reneri;

import eu.stamp_project.reneri.instrumentation.Instrumenter;
import eu.stamp_project.reneri.logging.TestExecutionObservationListener;
import eu.stamp_project.reneri.observations.MethodInvocationObservation;
import eu.stamp_project.reneri.observations.Observer;
import eu.stamp_project.reneri.observations.Probe;
import eu.stamp_project.reneri.observations.TriggerObservation;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

public class ExecutionObserver {

    private TestExecutionObservationListener listener;

    private BiConsumer<Class<?>, TestExecutionObservationListener> executor;

    private Instrumenter instrumenter = new Instrumenter();

    private BiFunction<String, byte[], byte[]> transformation; // Maybe String, byte[] -> byte[]

    private TriggerCollector triggerCollector;

    private ClassPool basePool;

    private int originalExecutions = 2;

    private int instrumentedExecutions = 1;


    public ExecutionObserver(TestExecutionObservationListener listener,
                             BiConsumer<Class<?>, TestExecutionObservationListener> executor,
                             BiFunction<String, byte[], byte[]> transformation,
                             TriggerCollector triggerCollector) {
        this.listener = listener;
        this.executor = executor;
        this.transformation = transformation;
        this.triggerCollector = triggerCollector;
    }

    //TODO: Handle the exceptions
    public void observeEffects(String methodName, String methodDesc, String declaringClassName, String testClassName) throws NotFoundException, IOException {

        try {
            Set<TriggerObservation> observedTriggers = triggerCollector.collectTriggers(methodName, methodDesc, declaringClassName, testClassName);

            basePool = new ClassPool(true);

            CtClass declaringCtClass = basePool.getCtClass(declaringClassName);
            CtClass testCtClass = basePool.getCtClass(testClassName);

            Set<CtClass> instrumentedCtClasses = instrumentTriggers(observedTriggers);
            instrumentedCtClasses.remove(testCtClass); // It will be handled differently

            execute(declaringCtClass, testCtClass, instrumentedCtClasses, originalExecutions);

            // Applying the transformation
            byte[] mutant = transformation.apply(declaringClassName, declaringCtClass.toBytecode());
            ClassPool transformationPool = new ClassPool(basePool);
            transformationPool.childFirstLookup = true;

            transformationPool.appendClassPath(new ByteArrayClassPath(declaringClassName, mutant));

            declaringCtClass = transformationPool.getCtClass(declaringClassName);

            CtMethod method = declaringCtClass.getMethod(methodName, methodDesc);
            instrumenter.instrument(method);

            execute(declaringCtClass, testCtClass, instrumentedCtClasses, instrumentedExecutions);

            basePool = null;
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("Instrumentation error", exc);
        }
    }

    protected void execute(CtClass declaringCtClass, CtClass invokingCtTest, Set<CtClass> complementaryCtTests, int times) throws IOException {

        try {
            Loader.Simple loader = new Loader.Simple();

            Class<?> declaringClass = loader.invokeDefineClass(declaringCtClass);
            for (CtClass ctClass : complementaryCtTests) {
                loader.invokeDefineClass(ctClass);
            }

            Class<?> testClass = loader.invokeDefineClass(invokingCtTest);

            Probe.init(new Observer(declaringClass), (obs) -> listener.methodInvoked((MethodInvocationObservation) obs));

            for (int i = 0; i < times; i++) {
                executor.accept(testClass, listener);
            }
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("Instrumentation error detected while loading the classes", exc);
        }

    }

    protected Set<CtClass> instrumentTriggers(Set<TriggerObservation> observedTriggers) throws NotFoundException {
        try {
            Set<CtClass> instrumentedTestCtClasses = new HashSet<>();

            for (TriggerObservation trigger : observedTriggers) {
                CtClass tesToInstrument = basePool.getCtClass(trigger.getContainerClass());
                CtMethod[] potentialTriggerContainers = tesToInstrument.getDeclaredMethods(trigger.getContainerMethod());

                for (CtMethod potentialTriggerContainer : potentialTriggerContainers) {
                    new ExprEditor() {

                        public boolean matches(MethodCall call, TriggerObservation trigger) {
                            return call.getMethodName().equals(trigger.getTrigger())
                                    && call.getClassName().equals(trigger.getTriggerClass())
                                    && call.getLineNumber() == trigger.getLineNumber();
                        }

                        @Override
                        public void edit(MethodCall call) {
                            if (matches(call, trigger)) {
                                instrumenter.instrument(call);
                                instrumentedTestCtClasses.add(tesToInstrument);
                            }
                        }
                    }.doit(tesToInstrument, potentialTriggerContainer.getMethodInfo());
                }
            }

            return instrumentedTestCtClasses;
        }catch (CannotCompileException exc) {
            throw new AssertionError("Instrumentation error", exc);
        }
    }

    public int getOriginalExecutions() {
        return originalExecutions;
    }

    public void setOriginalExecutions(int originalExecutions) {
        if(originalExecutions < 1) {
            throw new IllegalArgumentException("There should be at least one original execution");
        }
        this.originalExecutions = originalExecutions;
    }

    public int getInstrumentedExecutions() {
        return instrumentedExecutions;
    }

    public void setInstrumentedExecutions(int instrumentedExecutions) {
        if(instrumentedExecutions < 1) {
            throw new IllegalArgumentException("There should be at least one execution for the instrumented code");
        }
        this.instrumentedExecutions = instrumentedExecutions;
    }
}
