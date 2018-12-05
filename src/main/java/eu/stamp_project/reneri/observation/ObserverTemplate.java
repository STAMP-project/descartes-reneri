package eu.stamp_project.reneri.observation;

import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.template.AbstractTemplate;
import spoon.template.Local;
import spoon.template.Substitution;

import java.io.*;
import java.text.NumberFormat;
import java.util.Locale;

public class ObserverTemplate extends AbstractTemplate<CtClass<?>> {

    @Local
    @Override
    public CtClass<?> apply(CtType<?> targetType) {

        Substitution.insertAllFields(targetType, this);

        Factory factory = targetType.getFactory();
        CtClass<?> template = factory.Class().get(getClass());

        setInitialPoint(targetType);
        //setPathPrefix(targetType, baseFilePath);

        template.getMethods().stream()
                .filter(method -> !(isObserver(method) || isLocal(method)))
                .forEach(method -> Substitution.insertMethod(targetType, this, method));

        return (CtClass<?>)targetType;
    }

    @Local
    private boolean isLocal(CtMethod<?> method) {
        return method.getAnnotation(Local.class) != null;
    }

    @Local
    private boolean isObserver(CtMethod<?> method) {
        return method.getSimpleName().startsWith("observe_");
    }

    @Local
    private void assign(CtType<?> type, String field, String literalValue) {
        CtLiteral literal = type.getFactory().createLiteral(literalValue);
        ((CtField)type.getField(field)).setAssignment(literal);
    }

//    @Local
//    private void setPathPrefix(CtType<?> type, String path) {
//        String fullPath = Paths.get(baseFilePath, type.getQualifiedName()).toString();
//        assign(type, "pathPrefix", fullPath);
//    }


    @Local
    private void setInitialPoint(CtType<?> observerType) {
        String declaringTypeFullName = observerType.getDeclaringType().getQualifiedName();
        assign(observerType, "currentPoint", declaringTypeFullName);
        assign(observerType, "typeToObserve", declaringTypeFullName);
    }

//    @Local
//    public String baseFilePath;

    static Writer output;
    static NumberFormat format = NumberFormat.getNumberInstance(Locale.ROOT);

    static String currentPoint = "";
    static String typeToObserve;

    static String SEP = "#";

    public static void initialize() {
        try {

            String folder = System.getProperty("stamp.reneri.folder");

            if(folder == null)  {
                folder = ".";
            }

            final String fullPath = String.format("%s/%s-%s.jsonl", folder, typeToObserve, System.currentTimeMillis());
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
            throw new RuntimeException("Could not create the observation file for: " + typeToObserve, exc);
        }
    }

    public static void enter(String point) {
        currentPoint += SEP + point;
    }

    public static void topenter(String point) {
        int index = currentPoint.indexOf(SEP);
        if(index >=0)
            currentPoint = currentPoint.substring(0, index);
        enter(point);
    }

    public static void exit() {
        int index = currentPoint.lastIndexOf(SEP);
        if(index >= 0)
            currentPoint = currentPoint.substring(0, index);
    }

    public static void write(String text) {
        try {
            if (output == null) initialize();
            output.write(text);
            output.flush(); // This harms execution time
        }
        catch(IOException exc) {
            throw new RuntimeException("Error while writing the observation file", exc); //Stops the test execution
        }
    }

    public static void observe(String point, String type, String value) {
        write(String.format("{\"point\":\"%s\",\"type\":\"%s\",\"value\":%s}\n", currentPoint + SEP + point, type, value));
    }

    public static void observeNull(String point, Object value) {
        write(String.format("{\"point\":\"%s\",\"null\":%s}\n", currentPoint + SEP + point, value == null));
    }


    public static boolean observe_boolean(String point, boolean b) {
        observe(point, "boolean", Boolean.toString(b));
        return b;
    }

    public static Boolean observe_java_lang_Boolean(String point, Boolean b) {
        observe(point, "java.lang.Boolean", (b==null)?"null":b.toString());
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

    public static float observe_float(String point, float f) {
        observe(point, "float", Float.toString(f)); //TODO: No locale issue???
        return f;
    }

    public static Float observe_java_lang_Float(String point, Float f) {
        observe(point, "java.lang.Float", (f==null)?"null":f.toString());
        return f;
    }

    public static String observe_java_lang_String(String point, String s) {
        observe(point, "java.lang.String", (s == null)? "null":String.format("\"%s\"", escape(s)));
        return s;
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

}
