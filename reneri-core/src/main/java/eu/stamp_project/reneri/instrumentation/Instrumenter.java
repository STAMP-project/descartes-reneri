package eu.stamp_project.reneri.instrumentation;

import eu.stamp_project.reneri.observations.Probe;
import javassist.*;
import javassist.expr.MethodCall;


public class Instrumenter {

    static {

        final String probeClass = Probe.class.getName();
        final String commonArguments = "\"%1$s\",%2$s,$class,$0,$sig,$args,";
        final String exc = "exc);";
        final String result = "$type,$_);";

        CALL_PROBE_TEMPLATE = "try { $_ = $proceed($$); " +
                        probeClass + ".trigger(" + commonArguments + result +
                        " } catch(Throwable exc) { " +
                        probeClass + ".trigger(" + commonArguments + exc + " throw exc; }";

        TRIGGER_PROBE = probeClass + ".trigger();";

        METHOD_PROBE_TEMPLATE = probeClass + ".method(" + commonArguments + result;
        CATCH_PROBE_TEMPLATE = "{ " + probeClass + ".method(" + commonArguments + exc + "; throw exc; }";

    }

    private final static String CALL_PROBE_TEMPLATE;
    private static final String METHOD_PROBE_TEMPLATE;
    private static final String CATCH_PROBE_TEMPLATE;
    private final static String TRIGGER_PROBE;

    public void instrument(CtMethod method) {
        try {

            String methodDesc = method.getDeclaringClass().getName().replace('.', '/') + "#" +
                            method.getName() + method.getSignature();
            int line = method.getMethodInfo().getLineNumber(0);
            method.insertAfter(String.format(METHOD_PROBE_TEMPLATE, methodDesc, line));
            CtClass throwableClass = ClassPool.getDefault().get("java.lang.Throwable");
            method.addCatch(String.format(CATCH_PROBE_TEMPLATE,methodDesc, line), throwableClass, "exc");
        }
        catch (CannotCompileException | NotFoundException  exc) {
            throw new AssertionError("Unexpected error in the code of the probe or standard classes", exc);
        }
    }

    public void instrumentForTrigger(CtMethod method) {
        try {
            method.insertBefore(TRIGGER_PROBE);
        }
        catch (CannotCompileException exc) {
            throw new AssertionError("Unexpected error in the code of the trigger probe", exc);
        }
    }

    public void instrument(MethodCall call) {
        try {
            call.replace(
                    String.format(CALL_PROBE_TEMPLATE,
                            call.getClassName().replace('.', '/') + "#" + call.getMethodName() + call.getSignature(),
                            call.getLineNumber()));
        } catch (CannotCompileException exc) {
            throw new AssertionError("Unexpected error in the code of the method call probe", exc);
        }
    }

}
