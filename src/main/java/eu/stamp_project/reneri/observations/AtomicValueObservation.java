//package eu.stamp_project.reneri.observations;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.stream.Stream;
//
//import static eu.stamp_project.utils.TypeHelper.wrap;
//
//public class AtomicValueObservation extends ValueObservation {
//
//    private final static String NULL = "null";
//
//    // TODO: Pass to Descartes
//    private final static HashMap<String, Class<?>> NAME_PRIMITIVE = new HashMap<>();
//
//    static {
//        Stream.of(short.class,
//                byte.class,
//                int.class,
//                long.class,
//                boolean.class,
//                float.class,
//                double.class,
//                char.class)
//                .forEach(type -> NAME_PRIMITIVE.put(type.getTypeName(), type));
//    }
//
//    private static Class<?> classForName(String name) throws ClassNotFoundException {
//        Class<?> type = NAME_PRIMITIVE.getOrDefault(name, null);
//        return (type != null) ? type : Class.forName(name);
//
//    }
//
//    private String literalValue;
//
//    private Class<?> type;
//
//    private Object value;
//
//
//    public AtomicValueObservation(String pointcut, String typeName, String literalValue) throws InvalidObservationException {
//        super(pointcut);
//        try {
//            this.type = classForName(typeName);
//            this.literalValue = literalValue;
//
//            if (literalValue.equals(NULL)) {
//                this.value = null;
//                return;
//            }
//
//            if(this.type == String.class) {
//                this.value = literalValue; //TODO: Unescape the string, it is not needed to detect a change but it is for precise condition inference
//                return;
//            }
//            if(this.type == char.class || this.type == Character.class) {
//                // Characters are special cases
//                this.value = literalValue.charAt(0);
//                return;
//            }
//
//            Class<?> wrapper = (type.isPrimitive())? wrap(type) : type;
//            Method converter = wrapper.getMethod("valueOf", String.class);
//            this.value = converter.invoke(null, literalValue);
//        }
//        catch(ClassNotFoundException exc) {
//            throw new InvalidObservationException("Type " + typeName + "  was not found while building the observation. It is probably not a primitive or wrapper type nor String.", exc);
//
//        }
//        catch (NoSuchMethodException exc) {
//            throw new InvalidObservationException(
//                    String.format("There is no 'valueOf' method in class %s.", typeName), exc);
//
//        }
//        catch(IllegalAccessException exc) {
//            throw new InvalidObservationException(
//                    String.format("Method 'valueOf' of class %s is not accessible.", typeName), exc);
//
//        }
//        catch(InvocationTargetException exc) {
//            throw new InvalidObservationException(
//                    String.format("There was an error converting literal <%s> to type %s.", literalValue, typeName), exc);
//        }
//
//
//    }
//
//    @Override
//    public String getObservedTypeName() {
//        return type.getTypeName();
//    }
//
//    public Class<?> getObservedType() {
//        return type;
//    }
//
//    public String getLiteral() {
//        return literalValue;
//    }
//
//    public Object getValue() {
//        return value;
//    }
//
//    @Override
//    public boolean isNull() {
//        return getValue() == null;
//    }
//
//    public boolean isNullable() { return !type.isPrimitive(); }
//
//
//}
