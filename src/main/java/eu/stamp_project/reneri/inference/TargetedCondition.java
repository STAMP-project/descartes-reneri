//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.Observation;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//
//
//public abstract class TargetedCondition<T extends Observation> implements Condition {
//
//    protected Class<T> observationType;
//
//    public TargetedCondition(Class<T> observationType) {
//        this.observationType = observationType;
//    }
//
//    /**
//     * Returns a boolean value specifying if the given observation should be ignored while checking the condition
//     * @param observation
//     * @return
//     */
//    public abstract boolean canTarget(T observation);
//
//    /**
//     * Checks whether the condition holds for the given observation
//     * @param observation
//     * @return
//     */
//    public abstract boolean holdsFor(T observation);
//
//    protected Optional<T> toSpecifiedType(Observation observation) {
//        // One can't do observation instanceof T
//        if(observationType.isAssignableFrom(observation.getClass())) {
//            return Optional.of((T)observation);
//        }
//        return Optional.empty();
//    }
//
//    //TODO: Maybe the type filtering could be done outside this code
//    //TODO: Provide more information about the observations that did not meet the condition
//    // For example, which is the observation that does not meet the condition
//
//    /**
//     * Tells whether the condition applies for a given group of observations.
//     * It returns true if there is at least one observation that can be targeted and the condition holds for all
//     * the targeted observations. Therefore, returns false if there is no observation that can be targeted or the
//     * condition is false for at least one targeted observation
//     * @param observations
//     * @return
//     */
//    public boolean test(Observation... observations) {
//        List<T> toTarget = Arrays.stream(observations)
//                .map(this::toSpecifiedType)
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .filter(this::canTarget).collect(Collectors.toList());
//        if(toTarget.isEmpty()) {
//            return false;
//        }
//        return toTarget.stream().allMatch(this::holdsFor);
//    }
//}
