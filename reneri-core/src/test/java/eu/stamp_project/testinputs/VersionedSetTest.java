package eu.stamp_project.testinputs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionedSetTest {

    @Test
    public void testAdd() {
        VersionedSet<Integer> list = new VersionedSet<>();
        list.add(1);
        assertEquals(1, list.size());
    }

    @Test
    public void testEquals() {
        VersionedSet<Integer> one = new VersionedSet<>();
        VersionedSet<Integer> two = new VersionedSet<>();
        assertTrue(one.equals(two)); // We explicitly want to make the call to equals
    }

    @Test
    public void testIntersection() {
        VersionedSet<Integer> one = new VersionedSet<>();
        one.add(1);
        VersionedSet<Integer> two = new VersionedSet<>();
        two.add(2);
        VersionedSet<Integer> result = one.intersect(two);
        assertFalse(result.contains(1));
        assertFalse(result.contains(2));
    }

}