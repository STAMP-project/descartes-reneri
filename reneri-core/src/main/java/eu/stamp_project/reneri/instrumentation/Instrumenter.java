package eu.stamp_project.reneri.instrumentation;

import javassist.*;
import javassist.expr.MethodCall;


public class Instrumenter {

    static {
        final String method =  "eu.stamp_project.reneri.observations.Probe.observeInvocation(\"%1$s\",%2$s,$class,$0,$type,$_,$sig,$args,";
        CALL_PROBE_TEMPLATE = "try { " + method + "null); } catch (Throwable exc) { " + method + "exc); }";
    }

    private final static String CALL_PROBE_TEMPLATE;

    private static final String METHOD_PROBE_TEMPLATE = "eu.stamp_project.reneri.Probe.observeInvocationAndTrigger(\"%s\",%s,$class,$0,$type,$_,$sig,$args,%s)";;

    public void instrument(CtMethod method) {
        try {
            String methodDesc = method.getDeclaringClass().getName().replace('.', '/') + "#" +
                            method.getName() + method.getSignature();
            int line = method.getMethodInfo().getLineNumber(0);
            method.insertAfter(String.format(METHOD_PROBE_TEMPLATE, methodDesc, line, "null"));
            CtClass throwableClass = ClassPool.getDefault().get("java.lang.Throwable");
            method.addCatch(String.format(METHOD_PROBE_TEMPLATE,methodDesc, line, "exc"), throwableClass, "exc");
        }
        catch (CannotCompileException | NotFoundException  exc) {
            throw new AssertionError("Unexpected error in the code of the probe or standard classes", exc);
        }
    }

    public void instrumentForTrigger(CtMethod method) {
        try {
            method.insertBefore("eu.stamp_project.reneri.Probe.observeTrigger();");
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
