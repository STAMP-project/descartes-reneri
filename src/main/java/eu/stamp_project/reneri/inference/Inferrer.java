package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.PointObservationCollection;

import java.util.List;


public interface Inferrer {
    List<Condition> infer(PointObservationCollection observationCollection);
}
