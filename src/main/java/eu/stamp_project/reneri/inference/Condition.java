package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.PointObservationCollection;

import java.util.function.Predicate;

public interface Condition extends Predicate<Observation[]> {

    default boolean appliesTo(PointObservationCollection pointObservations) {
        return pointObservations.stream().allMatch(this);
    }

}
