package eu.stamp_project.reneri.observations;

import eu.stamp_project.testinputs.VersionedSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.stamp_project.reneri.observations.Observer.observe;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ValueObservationTest {

    @Test
    public void testObserveNull() {
        assertNull(observe(null));
    }

    @Test
    public void testObservePrimitive() {
        int value = 3;
        ValueObservation observation = observe(3);
        assertThat(observation.getType(), is(Integer.class.descriptorString()));
        assertThat(observation, is(instanceOf(AtomicValueObservation.class)));

        AtomicValueObservation atomicValueObservation = (AtomicValueObservation) observation;
        assertThat(value, is(equalTo(atomicValueObservation.getValue())));
    }

    @Test
    public void testObserveString() {
        String value = "This is a string";
        ValueObservation observation = observe(value);

        assertThat(observation.getType(), is(String.class.descriptorString()));
        assertThat(observation, is(instanceOf(StringObservation.class)));

        StringObservation stringObservation = (StringObservation) observation;
        assertThat(stringObservation.getValue(), is(value));
        assertThat(stringObservation.getLength(), is(value.length()));
    }

    @Test
    public void testObserveLongString() {
        String value = "a".repeat(2 * StringObservation.PREFIX_LENGTH);
        StringObservation observation = (StringObservation) observe(value);
        assertThat(value.substring(0, StringObservation.PREFIX_LENGTH), is(observation.getValue()));
        assertThat(observation.getLength(), is(value.length()));
    }

    @Test
    public void testObserveArray() {
        int[] array = {1, 2, 3, 4, 5};
        ValueObservation observation = observe(array);

        assertThat(observation.getType(), is(array.getClass().descriptorString()));
        assertThat(observation, is(instanceOf(ArrayObservation.class)));

        ArrayObservation arrayObservation = (ArrayObservation) observation;
        assertThat(arrayObservation.getLength(), is(equalTo(array.length)));
    }

    @Test
    public void testObserveObject() {
        // VersionedSet is not a collection as per Collection or Map implementation
        VersionedSet<Integer> set = new VersionedSet<>();

        ValueObservation observation = observe(set);

        assertThat(observation.getType(), is(VersionedSet.class.descriptorString()));
        assertThat(observation, is(instanceOf(ObjectObservation.class)));

        ObjectObservation objectObservation = (ObjectObservation)observation;

        assertThat(objectObservation.getFields(), arrayWithSize(2)); // Two fields, version and elements
        //TODO: Validate the fields,

    }

    @Test
    public void testObserveCollection() {
        List<Integer>  list = List.of(1, 2, 3, 4, 5);
        ValueObservation observation = observe(list);

        assertThat(observation.getType(), is(list.getClass().descriptorString()));
        assertThat(observation, is(instanceOf(CollectionObservation.class)));

        CollectionObservation collectionObservation = (CollectionObservation)observation;
        assertThat(collectionObservation.getSize(), is(list.size()));
    }



}