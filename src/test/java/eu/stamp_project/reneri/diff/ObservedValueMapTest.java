package eu.stamp_project.reneri.diff;

import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.ObservedAtomicValue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObservedValueMapTest {

    private ObservedValueMap map;

    private static final String POINTCUT = "pointcut";

    @Before
    public void setUp() {
        map = new ObservedValueMap();
    }

    @Test
    public void putNewValue() {
        ObservedAtomicValue value = new ObservedAtomicValue("int", "1");
        Observation observation = new Observation(POINTCUT, value);
        map.put(observation);
        assertEquals(value, map.get(POINTCUT));
    }

    @Test
    public void nonExistingValue() {
        assertNull(map.get(POINTCUT));
    }

    @Test
    public void putTwoDifferentValues() {
        map.put(new Observation(POINTCUT, new ObservedAtomicValue("int","1")));
        map.put(new Observation(POINTCUT, new ObservedAtomicValue("int","2")));
        assertNull(map.get(POINTCUT));
    }


    @Test
    public void putTheSameValueTwice() {

        // Observation creation repetitiion is on purpose

        map.put(new Observation(POINTCUT, new ObservedAtomicValue("int","1")));
        map.put(new Observation(POINTCUT, new ObservedAtomicValue("int","1")));

        assertEquals(
                new ObservedAtomicValue("int","1"),
                map.get(POINTCUT)
        );
    }

}