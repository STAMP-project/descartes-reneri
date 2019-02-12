//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.AtomicValueObservation;
//import eu.stamp_project.reneri.observations.NullValueObservation;
//import eu.stamp_project.reneri.observations.Observation;
//import org.junit.Test;
//
//import static org.junit.Assert.*;
//
//public class ConditionTest {
//
//
//    @Test
//    public void testMultipleConditions() throws Exception {
//
//
//        TargetedCondition mustBeThree = new FixedValue(3, int.class);
//
//        TargetedCondition mustBeNull = new AllNull();
//
//        Observation three = new AtomicValueObservation("0", "int", "3");
//
//        Observation isNull = new NullValueObservation("1", "java.lang.Object", true);
//
//        assertFalse(mustBeThree.toSpecifiedType(isNull).isPresent());
//
//        assertFalse(mustBeNull.holdsFor(three));
//        assertTrue(mustBeThree.holdsFor(three));
//        assertFalse(mustBeThree.test(new Observation[]{isNull}));
//
//    }
//
//}