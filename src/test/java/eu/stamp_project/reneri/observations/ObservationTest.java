package eu.stamp_project.reneri.observations;

import eu.stamp_project.reneri.instrumentation.StateObserver;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

public class ObservationTest {

    //TODO: Test escaped values

    @Test
    public void testCreateAtomicValueObservation() throws InvalidObservationException {

        Observation observation = Observation.fromString(
                object(
                        stringProperty("point", "0"),
                        stringProperty("type", "int"),
                        property("value", "3")
                )
        );

        ObservedValue value = observation.getValue();
        assertThat(value, instanceOf(ObservedAtomicValue.class));
        assertFalse("Value should not be null", value.isNull());
        ObservedAtomicValue atomicValue = (ObservedAtomicValue) value;
        assertEquals("Wrong literal value","3", atomicValue.getLiteralValue());
    }

    @Test
    public void testCreateNullValueObservation() throws InvalidObservationException {
        Observation observation = Observation.fromString(
                object(
                        stringProperty("point", "0"),
                        stringProperty("type", "java.util.List"),
                        property("null", "true")
                )
        );
        ObservedValue value = observation.getValue();
        assertThat(value, instanceOf(ObservedNullValue.class));
        assertTrue("Observed value must be null",  value.isNull());
    }

    @Test
    public void testCreateExceptionObservation() throws  InvalidObservationException {
        Observation observation = Observation.fromString(
                object(
                        stringProperty("point", "0"),
                        stringProperty("exception", "Exception"),
                        stringProperty("message", "message")
                )
        );

        ObservedValue value = observation.getValue();
        assertThat(value, instanceOf(ObservedException.class));
        assertFalse("Observe exceptions are never null", value.isNull());
    }

    @Test
    public void testAtomicObservationWithNullValue() throws InvalidObservationException {

        Observation observation = Observation.fromString(
                object(
                        property("point", string("0")),
                        property("type", string("java.lang.String")),
                        property("value", "null"))
        );

        ObservedValue value = observation.getValue();
        assertThat(value, instanceOf(ObservedAtomicValue.class));
        assertTrue("Should be a null atomic value", value.isNull());
    }

    @Test
    public void testStackTraceObservation() throws InvalidObservationException {
        Observation observation = Observation.fromString("{\"point\":\"trace\", \"trace\":[{\"class\":\"joptsimple.util.EnumConverter\", \"method\":\"message\",\"line\":107},{\"class\":\"joptsimple.util.EnumConverter\", \"method\":\"convert\",\"line\":63},{\"class\":\"joptsimple.util.EnumConverterTest\", \"method\":\"rejectsNonEnumeratedValues\",\"line\":61},{\"class\":\"sun.reflect.NativeMethodAccessorImpl\", \"method\":\"invoke0\",\"line\":-2}]}");
        ObservedValue value = observation.getValue();
        assertThat(value, instanceOf(ObservedStackTrace.class));
    }


    @Test public void testEqualStackTraces() throws InvalidObservationException {
        // The only difference is a -1 for the line of the first method
        Observation one   = Observation.fromString("{\"point\":\"trace\", \"trace\":[{\"class\":\"joptsimple.util.EnumConverter\", \"method\":\"message\",\"line\":107},{\"class\":\"joptsimple.util.EnumConverter\", \"method\":\"convert\",\"line\":63},{\"class\":\"joptsimple.util.EnumConverterTest\", \"method\":\"rejectsNonEnumeratedValues\",\"line\":61},{\"class\":\"sun.reflect.NativeMethodAccessorImpl\", \"method\":\"invoke0\",\"line\":-2}]}");
        Observation other = Observation.fromString("{\"point\":\"trace\", \"trace\":[{\"class\":\"joptsimple.util.EnumConverter\", \"method\":\"message\",\"line\":-1},{\"class\":\"joptsimple.util.EnumConverter\", \"method\":\"convert\",\"line\":63},{\"class\":\"joptsimple.util.EnumConverterTest\", \"method\":\"rejectsNonEnumeratedValues\",\"line\":61},{\"class\":\"sun.reflect.NativeMethodAccessorImpl\", \"method\":\"invoke0\",\"line\":-2}]}");
        assertEquals(one.getValue(), other.getValue());
    }


//
//    @Test
//    public void testCharAtomicValue() throws InvalidObservationException {
//        Observation observation = Observation.fromLine(
//                object(
//                        property("point", string("0")),
//                        property("type", string("char")),
//                        property("value", string("\n")))
//        );
//        assertTrue("Observation is expected to be an atomic value observation", observation instanceof AtomicValueObservation);
//        AtomicValueObservation valueObservation = (AtomicValueObservation)observation;
//        assertEquals(char.class, valueObservation.getObservedType());
//        assertEquals('\n', valueObservation.getValue());
//    }
//
    private static String object(String...properties) {
        return "{" + Stream.of(properties).collect(Collectors.joining(", ")) + "}\n";
    }

    private static String property(String name, String value) {
        return String.format("%s: %s", string(name), value);
    }

    private static String stringProperty(String name, String value) {
        return property(name, string(value));
    }

    private static String string(String str) {
        return String.format("\"%s\"", StateObserver.escape(str));
    }

}