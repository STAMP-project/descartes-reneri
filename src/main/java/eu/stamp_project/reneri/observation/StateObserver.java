package eu.stamp_project.reneri.observation;

import java.io.*;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

public class StateObserver {


    private static Writer output;
    private static NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);

    private static String currentPoint = "";

//    //static String typeToObserve;
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
        return type.isPrimitive() || isWrapper(type);
    }

    private static void initialize() {
        try {

            String folder = System.getProperty("stamp.reneri.folder");

            if(folder == null)  {
                folder = ".";
            }

            final String fullPath = String.format("%s/%s.jsonl", folder, System.currentTimeMillis());
            File file = new File(fullPath);
            if(file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            output = new FileWriter(file);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run () { try {output.close();} catch(Exception exc){}}
            });
        }
        catch (IOException exc){
            throw new RuntimeException("Could not create the observation file", exc);
        }
    }

//    public static void setCurrentPoint(String point) {
//        currentPoint = point;
//    }
//
//    public static void enter(String point) {
//        currentPoint += SEP + point;
//    }
//
//    public static void exit() {
//        int index = currentPoint.lastIndexOf(SEP);
//        if(index >= 0)
//            currentPoint = currentPoint.substring(0, index);
//    }


    private static void write(String text) {
        try {
            if (output == null) initialize();
            output.write(text + "\n");
            output.flush(); // This harms execution time
        }
        catch(IOException exc) {
            throw new RuntimeException("Error while writing the observation file", exc); //Stops the test execution
        }
    }

    private static void observe(String point, String type, String value) {
        write(String.format("{\"point\":\"%s\",\"type\":\"%s\",\"value\":%s}", point, type, value));
    }

    private static void observeNull(String point, Class<?> type, Object value) {
        write(String.format("{\"point\":\"%s\",\"type:\":\"%s\",\"null\":%s}", point, type.getTypeName(), value == null));
    }

    private static void observeThrownException(String point, Throwable exc) {
        write(String.format("{\"point\": \"%s\", \"exception\": \"%s\", \"message\": \"%s\"}", point, exc.getClass().getTypeName(), exc.getMessage()));
    }

    private static void observeAtomic(String point, Class<?> type, Object value) {
        if(value instanceof String) {
            observe_java_lang_String(point, (String)value);
        }
        else {
            String valueToPrint = (value == null)?"null":value.toString();
            if(value instanceof Character) { // Special case
                valueToPrint = String.format("\"%s\"", value.toString());
            }
            observe(point, type.getTypeName(), valueToPrint);
        }
    }

    public static <T> T observeState(String point, Class type, Object value) {
        // The type is needed for when the value is null at runtime
        if(isAtomic(type)) {
            observeAtomic(point, type, value);
            @SuppressWarnings("unchecked")
            T result = (T)value;
            return result;
        }

        observeNull(point, type, value);
        if(value == null) {
            return null;
        }

        for(Field field : value.getClass().getFields()) {
            Class<?> fieldType = field.getType();
            String fieldPoint = point + "|" + field.getName();

            try {
                Object fieldValue = field.get(value);
                if (isAtomic(type)) {
                    observeAtomic(fieldPoint, fieldType, fieldValue);
                } else {
                    observeNull(fieldPoint, fieldType, fieldValue);
                }
            }
            catch(Throwable exc) {
                observeThrownException(point, exc);
            }
        }

        @SuppressWarnings("unchecked")
        T result = (T)value;
        return result;
    }


    public static boolean observe_boolean(String point, boolean b) {
        observe(point, "boolean", Boolean.toString(b));
        return b;
    }

    public static Boolean observe_java_lang_Boolean(String point, Boolean b) {
        observe(point, "java.lang.Integer", (b==null)?"null":b.toString());
        return b;
    }

    public static byte observe_byte(String point, byte b) {
        observe(point, "byte", Byte.toString(b));
        return b;
    }

    public static Byte observe_java_lang_Byte(String point, Byte b) {
        observe(point, "java.lang.Byte", (b==null)?"null":b.toString());
        return b;
    }

    public static short observe_short(String point, short s) {
        observe(point, "short", Short.toString(s));
        return s;
    }

    public static Short observe_java_lang_Short(String point, Short s) {
        observe(point, "java.lang.Short", (s==null)?"null":s.toString());
        return s;
    }

    public static int observe_int(String point, int i) {
        observe(point, "int", Integer.toString(i));
        return i;
    }

    public static Integer observe_java_lang_Integer(String point, Integer i) {
        observe(point, "java.lang.Integer", (i==null)?"null":i.toString());
        return i;
    }

    public static long observe_long(String point, long l) {
        observe(point, "long", Long.toString(l));
        return l;
    }

    public static Long observe_java_lang_Long(String point, Long l) {
        observe(point, "java.lang.Long", (l==null)?"null":l.toString());
        return l;
    }

    public static String observe_java_lang_String(String point, String s) {
        observe(point, "java.lang.String", (s == null)? "null":String.format("\"%s\"", escape(s)));
        return s;
    }

    public static double observe_double(String point, double d) {
        observe(point, "double", format.format(d));
        return d;
    }

    public static Double observe_java_lang_Double(String point, Double d) {
        observe(point, "java.lang.Double", (d==null)?"null":format.format(d));
        return d;
    }

    public static char observe_char(String point, char c) {
        observe(point, "char", String.format("\"%s\"", Character.toString(c)));
        return c;
    }

    public static Character observe_java_lang_Character(String point, Character c) {
        observe(point, "char", (c==null)?"null":String.format("\"%s\"", c.toString()));
        return c;
    }


    public static String escape(String value) {
        StringWriter result = new StringWriter(value.length());
        for(int i=0; i< value.length(); i++) {
            char c = value.charAt(i);
            String toWrite = null;
            switch (c) {
                case '\u2028':
                case '\u2029':
                    toWrite = "\\u2028";
                    break;
                case '"':
                    toWrite = "\\\"";
                    break;
                case '\\':
                    toWrite = "\\\\";
                    break;
                case '\t':
                    toWrite = "\\t";
                    break;
                case '\b':
                    toWrite = "\\b";
                    break;
                case '\n':
                    toWrite = "\\n";
                    break;
                case '\r':
                    toWrite = "\\r";
                    break;
                case '\f':
                    toWrite = "\\f";
                    break;
                default:
                    toWrite = Character.toString(c);
                    break;
            }
            result.write(toWrite);
        }
        return result.getBuffer().toString();
    }


}
