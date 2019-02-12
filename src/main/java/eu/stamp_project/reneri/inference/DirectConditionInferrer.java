//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.Observation;
//import eu.stamp_project.reneri.observations.PointObservationCollection;
//
//import java.util.List;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//public class DirectConditionInferrer implements Inferrer {
//
//    private Condition[] conditions;
//
//    public DirectConditionInferrer(TargetedCondition... conditions) {
//        this.conditions = conditions;
//    }
//
//    public DirectConditionInferrer() {
//        this(new AllNull(), new NoneNull(), new NullAndNotNull());
//    }
//
//    @Override
//    public List<Condition> infer(PointObservationCollection observationCollection) {
//        return Stream.of(conditions)
//                .filter(condition -> condition.appliesTo(observationCollection))
//                .collect(Collectors.toList());
//    }
//}
