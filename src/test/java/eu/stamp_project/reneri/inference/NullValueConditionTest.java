//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.AtomicValueObservation;
//import eu.stamp_project.reneri.observations.NullValueObservation;
//import org.junit.Before;
//import org.junit.Test;
//
//import static org.junit.Assert.*;
//
//public abstract class NullValueConditionTest<T extends NullValueCondition> {
//
//    protected T condition;
//
//    @Before
//    public void setUp() {
//        condition = createConditionInstance();
//    }
//
//    protected abstract T createConditionInstance();
//
//
//    protected String getClassName() {
//        return condition.getClass().getSimpleName();
//    }
//
//    protected String message(String content) {
//        return String.format("%s %s", getClassName(), content);
//    }
//
//    @Test
//    public void testCanTargetAtomicValueObservation() throws Exception {
//        assertTrue(message("condition should be able to target nullable atomic value observations"),
//                condition.canTarget(
//                        new AtomicValueObservation("1", "java.lang.String", "null")
//                )
//        );
//    }
//
//    @Test
//    public void testCanNotTargetNonNullableTypes() throws Exception {
//        assertFalse(message("condition can not target primitive values"),
//                condition.canTarget(
//                        new AtomicValueObservation("1", "int", "3")
//                )
//        );
//    }
//
//    @Test
//    public void testCanTargetNullValueObservations() throws Exception {
//        assertTrue(message("condition should be able to target null value observations"),
//                condition.canTarget(
//                        new NullValueObservation("1", "java.lang.Object", true)
//                )
//        );
//    }
//
//}