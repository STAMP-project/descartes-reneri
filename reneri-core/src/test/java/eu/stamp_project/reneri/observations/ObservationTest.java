package eu.stamp_project.reneri.observations;

import org.junit.jupiter.api.Test;

import static eu.stamp_project.reneri.observations.Observations.observeValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ObservationTest {

    @Test
    public void testObserveArray() {

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for(int i = 0; i < stackTrace.length; i++) {
            System.out.println(stackTrace[i]);
        }

//        int[] array = {1, 2, 3, 4, 5};
//        ValueObservation observation = observeValue(array);
//
//        assertThat(observation.getType(), is(array.getClass().descriptorString()));
//        assertThat(observation, is(instanceOf(ArrayObservation.class)));
//
//        ArrayObservation arrayObservation = (ArrayObservation) observation;
//        assertThat(arrayObservation.getLength(), is(equalTo(array.length)));
    }



}