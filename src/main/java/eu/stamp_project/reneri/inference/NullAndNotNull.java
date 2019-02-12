//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.Observation;
//import eu.stamp_project.reneri.observations.ValueObservation;
//
//import java.util.Arrays;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//public class NullAndNotNull extends NullValueCondition {
//
//    @Override
//    public boolean holdsFor(ValueObservation observation) {
//        return false;
//    }
//
//    @Override
//    public boolean test(Observation... observations) {
//        return Arrays.stream(observations)
//                .map(this::toSpecifiedType)
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .filter(this::canTarget)
//                .map(ValueObservation::isNull)
//                .collect(Collectors.toSet()).size() == 2;
//    }
//}
