package eu.stamp_project.reneri.instrumentation;


import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StateObserver {

    private static String[] REPLACEMENT;

    private static Pattern GETTER_NAME = Pattern.compile("^get[A-Z0-9_].*$");

    private static Pattern BOOLEAN_GETTER_NAME = Pattern.compile("^(is|has)[A-Z0-9_].*$");


    static {
        // https://github.com/google/gson/blob/9d44cbc19a73b45971c4ecb33c8d34d673afa210/gson/src/main/java/com/google/gson/stream/JsonWriter.java
        REPLACEMENT = new String[128];
        for (int i = 0; i <= 0x1f; i++) {
            REPLACEMENT[i] = String.format("\\u%04x", (int) i);
        }
        REPLACEMENT['"'] = "\\\"";
        REPLACEMENT['\\'] = "\\\\";
        REPLACEMENT['\t'] = "\\t";
        REPLACEMENT['\b'] = "\\b";
        REPLACEMENT['\n'] = "\\n";
        REPLACEMENT['\r'] = "\\r";
        REPLACEMENT['\f'] = "\\f";
    }

    private static Writer output;

//    private static NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);
//    private static String currentPoint = "";

//    static String typeToObserve;
//
//    private static String SEP = "#";

    private static HashSet<String> wrapperTypes = new HashSet<>(
            Arrays.asList(
                    "java.lang.Byte",
                    "java.lang.Short",
                    "java.lang.Integer",
                    "java.lang.Long",
                    "java.lang.Float",
                    "java.lang.Double",
                    "java.lang.Character",
                    "java.lang.Boolean"
            )
    );

    private static boolean isWrapper(Class<?> type) {
        return wrapperTypes.contains(type.getTypeName());
    }

    private static boolean isAtomic(Class<?> type) {
        return type == String.class || type.isPrimitive() || isWrapper(type);
    }

    protected static void initialize(Writer writer) {
        output = writer;
    }

    protected static void initialize() {
        try {
            String folder = System.getProperty("stamp.reneri.folder");
            if(folder == null)  {
                folder = ".";
            }
            File file = Paths.get(folder, "observations.jsonl").toFile();
            if(file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            if(!file.exists()) {
                file.createNewFile();
            }
            output = new FileWriter(file);
//
//
//            final String fullPath = String.format("%s/%s.jsonl", folder, System.currentTimeMillis());
//            File file = new File(fullPath);
//            if(file.getParentFile() != null) {
//                file.getParentFile().mkdirs();
//            }
//            file.createNewFile();
            ///This might produce deadlocks and anyways we are flushing
            output = new FileWriter(file);
            /*Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run () { try {output.close();} catch(Exception exc){}}
            });*/
        }
        catch (IOException exc){
            throw new RuntimeException("Could not create the observation file", exc);
        }
    }

    private static void write(String text) {
        try {
            if (output == null) initialize();
            output.write(text);
            output.flush(); // This harms execution time
        }
        catch(IOException exc) {
            throw new RuntimeException("Error while writing the observation file", exc); //Stops the test execution
        }
    }


    private static void writeJsonLine(String ... args) {
        if(args.length == 0 ) {
            throw new AssertionError("Must provide content to write a JSON line");
        }
        if(args.length %2 != 0) {
            throw new AssertionError("The number of parameters to write a JSON line must be even");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{\"").append(args[0]).append("\":").append(args[1]);
        for(int index = 2; index < args.length; index+=2) {
            builder.append(",\"").append(args[index]).append("\":").append(args[index+1]);
        }
        builder.append("}\n");
        write(builder.toString());
    }

    private static String quote(String value) {
        return String.format("\"%s\"", escape(value));
    }

    public static void observe(String point, String type, String value) {
        writeJsonLine("point", quote(point), "type", quote(type), "value", value);
    }

    public static void observeNull(String point, Class<?> type, Object value) {
        writeJsonLine("point", quote(point), "type", quote(type.getTypeName()), "null", Boolean.toString(value==null));
    }

    public static void observeThrownException(String point, Throwable exc) {
        writeJsonLine("point", quote(point), "exception", quote(exc.getClass().getTypeName()), "message", quote(exc.getMessage()));
    }

    public static void observeAtomic(String point, Class<?> type, Object value) {
        if(value instanceof String) {
            observe(point, (String)value);
            return;
        }
        else {
            String valueToPrint = (value == null)?"null":value.toString();
            if(value instanceof Character) { // Special case
                valueToPrint = quote(valueToPrint);
            }

            String typeName = type.getTypeName();
            if(value != null && !type.isPrimitive()) {
                typeName = value.getClass().getTypeName();
            }
            observe(point, typeName, valueToPrint);
        }
    }

    public static <T> T observeState(String point, Class type, Object value) {
        // The type is needed for when the value is null at runtime or it is a primitive type
        observeBasicState(point, type, value);
        if (value == null) {
            return null;
        }
        if (!isAtomic(type) && !type.isArray()) {
            observeInternalState(point, value);
//            observeComputedState(point, value);
        }

        @SuppressWarnings("unchecked")
        T result = (T) value;
        return result;
    }

    private static boolean mustSkipField(Field field) {
        //Skipping fields instrumented to compute coverage
        Class<?> type = field.getType();
        return field.getName().equals("$jacocoData") && type.isArray() && type.getComponentType().equals(boolean.class);
    }


    private static void observeInternalState(String point, Object value) {
        for (Field field : getFields(value.getClass())) {
            if(mustSkipField(field)) {
                continue;
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            String fieldPoint = point + "|" + field.getName();
            try {
                Object fieldValue = field.get(value);
                observeBasicState(fieldPoint, field.getType(), fieldValue);
            } catch (Throwable exc) {
                observeThrownException(point, exc);
            }
        }
    }

    private static boolean isAGetter(Method method) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() > 0) {
            return false;
        }
        String name = method.getName();
        return GETTER_NAME.matcher(name).matches() ||
                (method.getReturnType().equals(boolean.class) && BOOLEAN_GETTER_NAME.matcher(name).matches());
    }

    private static void observeComputedState(String point, Object value) {
        for(Method method : value.getClass().getMethods()) {
            if(!isAGetter(method)) {
                continue;
            }
            String pointcut = String.format("%s|#%s", point, method.getName());
            try {
                Object result = method.invoke(value);
                observeBasicState(pointcut, method.getReturnType(), result);
            }
            catch (Throwable exc) {
                observeThrownException(pointcut, exc);
            }
        }
    }

    private static void observeBasicState(String point, Class type, Object value) {
        if (isAtomic(type)) {
            observeAtomic(point, type, value);
            return;
        }
        observeNull(point, type, value);
        if (value == null) {
            return;
        }
        if (type.isArray()) {
            observe(point + "|length", Array.getLength(value));
            return;
        }
        if (value instanceof Collection) {
            String sizePointCut = point + "|#size";
            try {
                observe(sizePointCut, ((Collection) value).size());
            } catch (Throwable exc) { //Just in case :)
                observeThrownException(sizePointCut, exc);
            }
        }
    }

    // Static observers, they will be resolved by the attacher

    public static String observe(String point, String value) {
        observe(point, "java.lang.String", (value == null)? "null": quote(value));
        return value;
    }

    public static int observe(String point, int value) {
        observe(point, "int", Integer.toString(value));
        return value;
    }

    public static Integer observe(String point, Integer value) {
        observe(point, "java.lang.Integer", (value==null)?"null":value.toString());
        return value;
    }

    public static boolean observe(String point, boolean value) {
        observe(point, "boolean", Boolean.toString(value));
        return value;
    }

    public static Boolean observe(String point, Boolean value) {
        observe(point, "java.lang.Boolean", Boolean.toString(value));
        return value;
    }

    public static byte observe(String point, byte value) {
        observe(point, "byte", Byte.toString(value));
        return value;
    }

    public static Byte observe(String point, Byte value) {
        observe(point, "java.lang.Byte", (value==null)?"null":value.toString());
        return value;
    }

    public static short observe(String point, short value) {
        observe(point, "short", Short.toString(value));
        return value;
    }

    public static Short observe(String point, Short value) {
        observe(point, "java.lang.Short", (value==null)?"null":value.toString());
        return value;
    }

    public static long observe(String point, long value) {
        observe(point, "long", Long.toString(value));
        return value;
    }

    public static Long observe(String point, Long value) {
        observe(point, "java.lang.Long", (value==null)?"null":value.toString());
        return value;
    }

    public static double observe(String point, double value) {
        //TODO: Deal with Infinity, -Infinity and Nan
        observe(point, "double", Double.toString(value));
        return value;
    }

    public static float observe(String point, float value) {
        observe(point, "float", Float.toString(value));
        return value;
    }

    public static Float observe(String point, Float value) {
        observe(point, "java.lang.Float", value.toString());
        return value;
    }

    public static Double observe(String point, Double value) {
        //TODO: Deal with Infinity, -Infinity and Nan
        observe(point, "java.lang.Double", (value==null)?"null":value.toString());
        return value;
    }

    public static char observe(String point, char value) {
        observe(point, "char", quote(Character.toString(value)));
        return value;
    }

    public static Character observe(String point, Character value) {
        observe(point, "java.lang.Character", (value==null)?"null":quote(value.toString()));
        return value;
    }

    public static String escape(String value) {
        StringWriter result = new StringWriter(value.length());
        for(int i=0; i< value.length(); i++) {
            char c = value.charAt(i);
            String toWrite = null;
            if(c < REPLACEMENT.length) {
                toWrite = REPLACEMENT[c];
                if(toWrite == null) {
                    toWrite = Character.toString(c);
                }
            }
            else if (c == '\u2028') {
                toWrite = "\\u2028";
            }
            else if(c == '\u2029') {
                toWrite = "\\u2028";
            }
            else {
                toWrite = Character.toString(c);
            }
            result.write(toWrite);
        }
        return result.getBuffer().toString();
    }

    public static class FieldIterator implements Iterator<Field> {

        private Class<?> initialClass;

        private Class<?> currentClass;

        private Field[] currentDeclaredFields;

        private int currentFieldIndex = -1;

        public FieldIterator(Class<?> klass) {
            this.initialClass = klass;

            currentClass = initialClass;
            currentDeclaredFields = currentClass.getDeclaredFields();
            currentFieldIndex = -1;
        }

        @Override
        public boolean hasNext() {

            // Iteration ended
            if(currentDeclaredFields == null) {
                return false;
            }
            // Iterating through the fields of a class
            currentFieldIndex++;
            if(currentFieldIndex < currentDeclaredFields.length) {
                return true;
            }

            do {
                currentClass = currentClass.getSuperclass();
                currentDeclaredFields = (currentClass != null)? currentClass.getDeclaredFields():null;
            }
            while(currentDeclaredFields != null && currentDeclaredFields.length == 0);

            if(currentDeclaredFields == null) {
                return false;
            }

            currentFieldIndex = 0;
            return true;
        }

        @Override
        public Field next() {
            return currentDeclaredFields[currentFieldIndex];
        }
    }

    private static Iterable<Field> getFields(Class<?> klass) {
        return () -> new FieldIterator(klass);
    }


    private static AtomicInteger counter = new AtomicInteger();

    public static void observe(String point, StackTraceElement[] stackTrace) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"point\":")
                .append(quote(point))
                .append(", \"trace\":[");
        for(int index = 1; index < stackTrace.length; index++) {
            if (index != 1) {
                builder.append(',');
            }
            StackTraceElement element = stackTrace[index];
            builder.append("{\"class\":").append(quote(element.getClassName()))
                    .append(", \"method\":").append(quote(element.getMethodName()))
                    .append(",\"line\":").append(element.getLineNumber())
                    .append("}");

        }
        builder.append("]}\n");
        write(builder.toString());
    }

    public static void observeProgramState(
            String containerClassName,
            String methodName,
            String signature,
            Class[] parameterTypes,
            Object[] parameters,
            Class thatType,
            Object that,
            Class resultType,
            Object result) {
        synchronized (counter) {
            String pointcut = counter.getAndIncrement() + "|";

            // There is no evidence that observing the stack trace would produce valuable results,
            // and there are many spurious differences with line numbers lower than 0
            // however, with the stack trace we can identify the exact test method that triggered the execution
            // so we record the stack trace but we don't use it while computing the differences.

            StackTraceElement[] stackTrace = new Exception().getStackTrace();
//            observe(pointcut + "#trace|length", stackTrace.length - 1);
            observe(pointcut + "#trace", stackTrace);

            observeState(pointcut + "#that", thatType, that);
            observeState(pointcut + "#result", resultType, result);
        }
    }

    private static String getMethodCallPointcut(String className, String methodName, String methodDesccription) {
        return String.format("%s|%s|%s|%d", className, methodName, methodDesccription, counter.getAndIncrement());
    }


    public static void observeMethodCall(
            String containerClassName,
            String methodName,
            String methodDescription,
            Class[] parameterTypes,
            Object[] parameters,
            Class thatType,
            Object that,
            Class resultType,
            Object result){
        synchronized (counter) {
            String pointcut = getMethodCallPointcut(containerClassName, methodName, methodDescription);
            // There is no evidence that observing the stack trace would produce valuable results,
            // and there are many spurious differences with line numbers lower than 0
            // however, with the stack trace we can identify the exact test method that triggered the execution
            // so we record the stack trace but we don't use it while computing the differences.
            StackTraceElement[] stackTrace = new Exception().getStackTrace();
//            observe(pointcut + "|#trace|length", stackTrace.length - 1);
            observe(pointcut + "|#trace", stackTrace);

            for(int index = 0; index < parameters.length; index++) {
                observeState(pointcut + "|#" + index, parameterTypes[index], parameters[index]);
            }

            if(that != null) {
                observeState(pointcut + "|#that", thatType, that);
            }

            if(resultType != void.class && resultType != Void.class) {
                observeState(pointcut + "|#result", resultType, result);
            }
        }
    }

    public static void observeMethodCall(
            String containerClassName,
            String methodName,
            String methodDescription,
            Class[] parameterTypes,
            Object[] parameters,
            Class resultType,
            Object result) {

        observeMethodCall(containerClassName, methodName, methodDescription, parameterTypes, parameters, null, null, resultType, result);

    }



}
