package eu.stamp_project.reneri.inference;


import eu.stamp_project.reneri.observations.PointObservationCollection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ComposedInferrer implements Inferrer {

    public Inferrer[] inferrers;

    public ComposedInferrer(Inferrer... inferrers) {
        this.inferrers = inferrers;
    }

    @Override
    public List<Condition> infer(PointObservationCollection observationCollection) {
        return Stream.of(inferrers).flatMap(inferrer -> inferrer.infer(observationCollection).stream()).collect(Collectors.toList());
    }
}
