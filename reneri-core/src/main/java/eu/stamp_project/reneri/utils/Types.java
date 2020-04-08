package eu.stamp_project.reneri.utils;

import java.io.IOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Types {

    //TODO: Check which functionalities from this class can be moved to ClassDesc and related.

    static {

        final String primitiveDesc = "[BCDFIJSZ]";
        final String identifier = "[^\\s\\.;\\[/]+";
        final String classDesc = String.format("L(%s/)*%1$s;", identifier);
        final String arrayDesc = String.format("\\[+(%s|%s)", primitiveDesc, classDesc);

        PRIMITIVE_DESCRIPTOR_PATTERN = Pattern.compile("^" + primitiveDesc + "$");
        CLASS_DESCRIPTOR_PATTERN = Pattern.compile("^" + classDesc + "$");
        ARRAY_DESCRIPTOR_PATTERN = Pattern.compile("^" + arrayDesc + "$");
        REFERENCE_TYPE_DESCRIPTOR_PATTERN = Pattern.compile(String.format("^(\\[*%s|\\[+%s)$", classDesc, primitiveDesc));

        FLOATING_POINT_TYPES = Stream.of(float.class, double.class, Double.class, Float.class)
                .map(Class::descriptorString)
                .collect(Collectors.toSet());

    }

    public static final Set<String> FLOATING_POINT_TYPES;

    private static final Pattern WRAPPER_DESCRIPTOR_PATTERN = Pattern.compile("^Ljava/lang/(Byte|Character|Double|Float|Integer|Long|Short|Boolean);$");


    private static final Pattern PRIMITIVE_DESCRIPTOR_PATTERN;
    private static final Pattern CLASS_DESCRIPTOR_PATTERN;
    private static final Pattern ARRAY_DESCRIPTOR_PATTERN;
    private static final Pattern REFERENCE_TYPE_DESCRIPTOR_PATTERN;


    public static boolean isPrimitiveDescriptor(String descriptor) {
        return PRIMITIVE_DESCRIPTOR_PATTERN.matcher(descriptor).matches();
    }

    public static boolean isWrapperDescriptor(String descriptor) {
        return WRAPPER_DESCRIPTOR_PATTERN.matcher(descriptor).matches();
    }

    public static boolean isClassDescriptor(String descriptor) {
        return CLASS_DESCRIPTOR_PATTERN.matcher(descriptor).matches();
    }

    public static boolean isReferenceTypeDescriptor(String descriptor) {
        return REFERENCE_TYPE_DESCRIPTOR_PATTERN.matcher(descriptor).matches();
    }

    public static boolean isArrayDescriptor(String descriptor) {
        return ARRAY_DESCRIPTOR_PATTERN.matcher(descriptor).matches();
    }

    public static boolean isWrapper(Class<?> type) {
       return isWrapperDescriptor(type.descriptorString());
    }

    public static boolean isCollection(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    }

    public static boolean isFloatingPointTypeDescriptor(String descriptor) {
        return FLOATING_POINT_TYPES.contains(descriptor);
    }


    public static void requireWrapperType(Class<?> type, String message) {
        if (!isWrapper(type))
            throw new InvalidParameterException(String.format( "%s. Got: %s.", message, type.descriptorString()));
    }

    public static Stream<Field> fieldsOf(Class<?> type) {
        Stream<Field> result = Stream.empty();
        while(type != null && type != Void.class && type != void.class && type != Object.class) {
            result = Stream.concat(result, Stream.of(type.getDeclaredFields()));
            type = type.getSuperclass();
        }
        return result;
    }

    public static String descriptor(Method method) {
        return MethodType.methodType(method.getReturnType(), method.getParameterTypes()).descriptorString();
    }

    public static String resourcePath(Class<?> aClass) { return aClass.getName().replace('.', '/') + ".class"; }

    public static byte[] toBytes(Class<?> aClass) throws IOException {
        return aClass.getClassLoader().getResourceAsStream(resourcePath(aClass)).readAllBytes();
    }

}
