package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;
import eu.stamp_project.reneri.observations.Observation;

public interface Condition<T extends Observation> {

    boolean canTarget(T observation);

    boolean holds(T observation);

    default boolean appliesTo(T[] observations) {
        for(T obs : observations) {
            if(canTarget(obs) && !holds(obs)) {
                return false;
            }
        }
        return true;
    }
}
