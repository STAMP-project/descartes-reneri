package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.Observation;

import java.util.function.Predicate;

public interface Condition extends Predicate<Observation[]> {

}
