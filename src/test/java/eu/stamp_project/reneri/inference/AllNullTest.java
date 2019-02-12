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
//public class AllNullTest extends NullValueConditionTest<AllNull> {
//
//    @Override
//    protected AllNull createConditionInstance() {
//        return new AllNull();
//    }
//
//    @Test
//    public void testHoldsForNullAtomicValue() throws Exception {
//        assertTrue("AllNull condition should hold for a single null atomic value",
//                condition.holdsFor(
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//    }
//
//    @Test
//    public void testDoesNotHoldForAtomicValue() throws Exception {
//        assertFalse("AllNull condition can not hold for a single non-null atomic value",
//                condition.holdsFor(
//                        new AtomicValueObservation("1", "java.lang.String", "\"something\"")
//                )
//        );
//    }
//
//    @Test
//    public void testMatchesAtomicValueObservations() throws Exception {
//        assertTrue("AllNull should be true for a collection of null atomic values",
//                condition.test(
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//    }
//
//    @Test
//    public void testDoesNotMatchAtomicValueObservations() throws Exception {
//        assertFalse("AllNull can not match atomic value observations if there is a non-null value",
//                condition.test(
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "\"\""),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//    }
//
//    @Test
//    public void testMatchesNullObservations() throws Exception {
//        assertTrue("AllNull must match true null observations",
//                condition.test(
//                        new NullValueObservation("1", "java.lang.Object", true),
//                        new NullValueObservation("1", "java.lang.String", true),
//                        new NullValueObservation("1", "java.lang.Integer", true)
//                )
//        );
//    }
//
//    @Test
//    public void testDoesNotMatchNullObservations() throws Exception {
//        assertFalse("AllNull can not match observations if there is at least one non-null value",
//                condition.test(
//                        new NullValueObservation("1", "java.lang.Object", true),
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
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//
//        collection.addArchive(
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//
//
//        assertTrue("AllNull should apply if all atomic value observations are null",
//                condition.appliesTo(collection)
//        );
//    }
//
//    @Test
//    public void testDoesNotApplyForObservationCollection() throws Exception {
//
//        PointObservationCollection collection = new PointObservationCollection("1",
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "null"),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//
//        collection.addArchive(
//                Arrays.asList(
//                        new AtomicValueObservation("1", "java.lang.String", "\"something\""),
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//
//        assertFalse("AllNull can not apply if there is at least one observation is not null",
//                condition.appliesTo(collection)
//        );
//    }
//
//
//
//}