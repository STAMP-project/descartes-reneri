package eu.stamp_project.reneri.observation;

import java.io.*;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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
            observe_java_lang_String(point, (String)value);
        }
        else {
            String valueToPrint = (value == null)?"null":value.toString();
            if(value instanceof Character) { // Special case
                valueToPrint = quote(valueToPrint);
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

        for(Field field :  getFields(value.getClass())) {
            if(!field.isAccessible()) {
                field.setAccessible(true);
            }
            Class<?> fieldType = field.getType();
            String fieldPoint = point + "|" + field.getName();

            try {
                Object fieldValue = field.get(value);
                if (isAtomic(fieldType)) {
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
        observe(point, "java.lang.String", (s == null)? "null": quote(s));
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
        observe(point, "char", quote(Character.toString(c)));
        return c;
    }

    public static Character observe_java_lang_Character(String point, Character c) {
        observe(point, "char", (c==null)?"null":quote(c.toString()));
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

    protected static class FieldIterator implements Iterator<Field> {

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


}
