package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;
import eu.stamp_project.reneri.observations.NullValueObservation;
import org.junit.Test;

import static org.junit.Assert.*;

public class NullAndNotNullTest extends NullValueConditionTest<NullAndNotNull> {

    @Override
    protected NullAndNotNull createConditionInstance() {
        return new NullAndNotNull();
    }

    @Test
    public void testDoesNotMatchAllNullAtomic() throws Exception {
        assertFalse(message("should be false for a collection where all atomic values are null"),
                condition.test(
                        new AtomicValueObservation("1", "java.lang.String", "null"),
                        new AtomicValueObservation("1", "java.lang.String", "null"),
                        new AtomicValueObservation("1", "java.lang.String", "null")
                )
        );
    }

    @Test
    public void testDoesNotMatchAllNonNullAtomic() throws Exception {
        assertFalse(message("should be false for a collection of non-null atomic values"),
                condition.test(
                        new AtomicValueObservation("1", "java.lang.String", "\"1\""),
                        new AtomicValueObservation("1", "java.lang.String", "\"2\""),
                        new AtomicValueObservation("1", "java.lang.String", "\"3\"")
                )
        );
    }

    @Test
    public void testMatchesForNullAndNotNullAtomic() throws Exception {
        assertTrue(message("should be true for a collection having both null and non-null values"),
                condition.test(
                        new AtomicValueObservation("1", "java.lang.String", "\"1\""),
                        new AtomicValueObservation("1", "java.lang.String", "null"),
                        new AtomicValueObservation("1", "java.lang.String", "\"3\"")
                )
        );
    }

    @Test
    public void testDoesNotMatchAllNull() throws Exception {
        assertFalse(message("should be false for a collection where all values are null"),
                condition.test(
                        new NullValueObservation("1", "java.lang.Object", false),
                        new NullValueObservation("1", "java.lang.String", false),
                        new NullValueObservation("1", "java.lang.Integer", false)
                )
        );
    }

    @Test
    public void testDoesNotMatchAllNonNull() throws Exception {
        assertFalse(message("should be false for a collection with no null value"),
                condition.test(
                        new NullValueObservation("1", "java.lang.Object", true),
                        new NullValueObservation("1", "java.lang.String", true),
                        new NullValueObservation("1", "java.lang.Integer", true)
                )
        );
    }

    @Test
    public void testMatchesMixedObservations() throws Exception {
        assertTrue(message("can not match observations if there is at least one null value"),
                condition.test(
                        new NullValueObservation("1", "java.lang.Object", false),
                        new NullValueObservation("1", "java.lang.String", false),
                        new NullValueObservation("1", "java.lang.Integer", true)
                )
        );
    }
}