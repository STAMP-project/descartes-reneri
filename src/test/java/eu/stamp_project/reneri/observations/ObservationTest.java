package eu.stamp_project.reneri.observations;

import org.junit.Test;

import static org.junit.Assert.*;

public class ObservationTest {

    //TODO: Test escape values

    @Test
    public void testCreateAtomicValueObservation() throws InvalidObservationException {

        Observation observation = Observation.fromLine("{\"point\": \"0\", \"type\": \"int\", \"NullValueCondition\": 3}\n");

        assertTrue("Observation is expected to be an atomic NullValueCondition observation", observation instanceof AtomicValueObservation);
        assertEquals("0", observation.getPointcut());

        AtomicValueObservation valueObservation = (AtomicValueObservation)observation;
        assertEquals(int.class, valueObservation.getObservedType());
        assertEquals(3, valueObservation.getValue());

    }

    @Test
    public void testCreateNullValueObservation() throws InvalidObservationException {
        Observation observation = Observation.fromLine("{\"point\": \"0\", \"type\": \"java.util.List\", \"null\": true}\n");

        assertTrue("Observation is expected to be a null NullValueCondition observation", observation instanceof NullValueObservation);
        assertEquals("0", observation.getPointcut());

        NullValueObservation nullValueObservation = (NullValueObservation)observation;
        assertEquals("Observed NullValueCondition must be of type java.util.List", "java.util.List", nullValueObservation.getObservedTypeName());
        assertTrue("Observed NullValueCondition must be null", nullValueObservation.isNull());
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

}