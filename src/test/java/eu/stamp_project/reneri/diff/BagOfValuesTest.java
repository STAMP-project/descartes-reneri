package eu.stamp_project.reneri.diff;

import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.ObservedAtomicValue;
import eu.stamp_project.reneri.observations.ObservedValue;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class BagOfValuesTest {

    private BagOfValues bag;

    @Before
    public void setUp() { bag = new BagOfValues(); }

    @Test
    public void testAdd() {
        bag.add(new Observation("point", new ObservedAtomicValue("int", "3")));
        assertFalse(bag.isEmpty());
        assertTrue(bag.containsPoint("point"));
        // Repeating the observation creation on purpose
        assertTrue(bag.containsObservation(new Observation("point", new ObservedAtomicValue("int", "3"))));
        assertEquals("Bag of values should contain only one element", 1, bag.getPointCount());
    }

    @Test
    public void testDoubleAdd() {

        // Creating on purpose two different objects
        bag.add(new Observation("point", new ObservedAtomicValue("int", "3")));
        bag.add(new Observation("point", new ObservedAtomicValue("int", "3")));

        assertEquals("Bag of values should contain only one element", 1, bag.getPointCount());

        Set<ObservedValue> values = bag.getObservedValues("point");
        assertThat(values, hasSize(1));
        // Creating another object on purpose
        assertThat(values, contains(new ObservedAtomicValue("int", "3")));
    }

    @Test
    public void testNotPresent() {
        bag.add(new Observation("point", new ObservedAtomicValue("int", "3")));
        assertFalse("Bag should not contain the observation",
                bag.containsObservation(
                        new Observation(
                                "point",
                                new ObservedAtomicValue("int", "4")
                        )
                )
        );
    }

}