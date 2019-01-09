package eu.stamp_project.reneri.observations;

import eu.stamp_project.reneri.instrumentation.StateObserver;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ObservationTest {

    //TODO: Test escaped values

    @Test
    public void testCreateAtomicValueObservation() throws InvalidObservationException {

        Observation observation = Observation.fromLine("{\"point\": \"0\", \"type\": \"int\", \"value\": 3}\n");

        assertTrue("Observation is expected to be an atomic value observation", observation instanceof AtomicValueObservation);
        assertEquals("0", observation.getPointcut());

        AtomicValueObservation valueObservation = (AtomicValueObservation)observation;
        assertEquals(int.class, valueObservation.getObservedType());
        assertEquals(3, valueObservation.getValue());

    }

    @Test
    public void testCreateNullValueObservation() throws InvalidObservationException {
        Observation observation = Observation.fromLine("{\"point\": \"0\", \"type\": \"java.util.List\", \"null\": true}\n");

        assertTrue("Observation is expected to be a null value observation", observation instanceof NullValueObservation);
        assertEquals("0", observation.getPointcut());

        NullValueObservation nullValueObservation = (NullValueObservation)observation;
        assertEquals("Observed value must be of type java.util.List", "java.util.List", nullValueObservation.getObservedTypeName());
        assertTrue("Observed value must be null", nullValueObservation.isNull());
    }

    @Test
    public void testCreateExceptionObservation() throws  InvalidObservationException {
        Observation observation = Observation.fromLine("{\"point\": \"0\", \"exception\": \"Exception\", \"message\": \"message\"}\n");

        assertTrue("Observation is expected to be an exception observation", observation instanceof ExceptionObservation);
        assertEquals("0", observation.getPointcut());

        ExceptionObservation exceptionObservation = (ExceptionObservation) observation;

        assertEquals("Exception", exceptionObservation.getExceptionTypeName());
        assertEquals("message", exceptionObservation.getExceptionMessage());
    }

    @Test
    public void testAtomicObservationWithNullValue() throws InvalidObservationException {

        Observation observation = Observation.fromLine(
                object(
                        property("point", string("0")),
                        property("type", string("java.lang.String")),
                        property("value", "null"))
        );

        assertTrue("Observation is expected to be an atomic value observation", observation instanceof AtomicValueObservation);
        assertEquals("0", observation.getPointcut());

        AtomicValueObservation valueObservation = (AtomicValueObservation)observation;
        assertEquals(String.class, valueObservation.getObservedType());
        assertNull(valueObservation.getValue());
    }

    @Test
    public void testCharAtomicValue() throws InvalidObservationException {
        Observation observation = Observation.fromLine(
                object(
                        property("point", string("0")),
                        property("type", string("char")),
                        property("value", string("\n")))
        );
        assertTrue("Observation is expected to be an atomic value observation", observation instanceof AtomicValueObservation);
        AtomicValueObservation valueObservation = (AtomicValueObservation)observation;
        assertEquals(char.class, valueObservation.getObservedType());
        assertEquals('\n', valueObservation.getValue());
    }

    private static String object(String...properties) {
        return "{" + Stream.of(properties).collect(Collectors.joining(", ")) + "}\n";
    }

    private static String property(String name, String value) {
        return String.format("%s: %s", string(name), value);
    }

    private static String string(String str) {
        return String.format("\"%s\"", StateObserver.escape(str));
    }

}