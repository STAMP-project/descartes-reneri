//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.AtomicValueObservation;
//import eu.stamp_project.reneri.observations.NullValueObservation;
//import eu.stamp_project.reneri.observations.PointObservationCollection;
//import org.junit.Test;
//
//import java.util.Arrays;
//
//import static org.junit.Assert.*;
//
//public class NoneNullTest extends NullValueConditionTest<NoneNull> {
//
//    @Override
//    protected NoneNull createConditionInstance() {
//        return new NoneNull();
//    }
//
//
//    @Test
//    public void testHoldsForNullAtomicValue() throws Exception {
//        assertTrue(message("condition should hold for a single non-null atomic value"),
//                condition.holdsFor(
//                        new AtomicValueObservation("1", "java.lang.String", "\"something\"")
//                )
//        );
//    }
//
//    @Test
//    public void testDoesNotHoldForAtomicValue() throws Exception {
//        assertFalse(message("condition can not hold for a single null atomic value"),
//                condition.holdsFor(
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//    }
//
//    @Test
//    public void testDoesNotMatchtomicValueObservations() throws Exception {
//        assertFalse(message("should be false for a collection having at least one null atomic values"),
//                condition.test(
//                        new AtomicValueObservation("1", "java.lang.String", "\"\""),
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "\"A\"")
//                )
//        );
//    }
//
//    @Test
//    public void testMatchesAtomicValueObservations() throws Exception {
//        assertTrue(message("should be true for a collection of non-null atomic values"),
//                condition.test(
//                        new AtomicValueObservation("1", "java.lang.String", "1"),
//                        new AtomicValueObservation("1", "java.lang.String", "2"),
//                        new AtomicValueObservation("1", "java.lang.String", "3")
//                )
//        );
//    }
//
//    @Test
//    public void testMatchesNonNullObservations() throws Exception {
//        assertTrue(message("must match false null observations"),
//                condition.test(
//                        new NullValueObservation("1", "java.lang.Object", false),
//                        new NullValueObservation("1", "java.lang.String", false),
//                        new NullValueObservation("1", "java.lang.Integer", false)
//                )
//        );
//    }
//
//    @Test
//    public void testDoesNotMatchNullObservations() throws Exception {
//        assertFalse(message("can not match observations if there is at least one null value"),
//                condition.test(
//                        new NullValueObservation("1", "java.lang.Object", false),
//                        new NullValueObservation("1", "java.lang.String", false),
//                        new NullValueObservation("1", "java.lang.Integer", true)
//                )
//        );
//    }
//
//    @Test
//    public void testAppliesForObservationCollection() throws Exception {
//
//        PointObservationCollection collection = new PointObservationCollection("1",
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "\"1\""),
//                        new AtomicValueObservation("1", "java.lang.String", "\"2\"")
//                )
//        );
//
//        collection.addArchive(
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "\"1\""),
//                        new AtomicValueObservation("1", "java.lang.String", "\"2\"")
//                )
//        );
//
//
//        assertTrue(message("should apply if no atomic value observations is null"),
//                condition.appliesTo(collection)
//        );
//    }
//
//    @Test
//    public void testDoesNotApplyForObservationCollection() throws Exception {
//
//        PointObservationCollection collection = new PointObservationCollection("1",
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "\"1\""),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//
//        collection.addArchive(
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "\"something\""),
//                        new AtomicValueObservation("1", "java.lang.String", "\"2\"")
//                )
//        );
//
//        assertFalse(message("can not apply if there is at least one observation is null"),
//                condition.appliesTo(collection)
//        );
//    }
//
//
//
//
//
//}