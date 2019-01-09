package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.PointObservationCollection;

import java.util.List;

//TODO: Maybe functional interface?
public interface Inferrer {
    List<Condition> infer(PointObservationCollection observationCollection);
}
