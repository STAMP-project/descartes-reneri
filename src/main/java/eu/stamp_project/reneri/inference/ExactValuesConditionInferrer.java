package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;
import eu.stamp_project.reneri.observations.PointObservationCollection;

import java.util.*;
import java.util.stream.Collectors;

public class ExactValuesConditionInferrer implements Inferrer {

    public final static Set<Class> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            short.class,
            byte.class,
            int.class,
            long.class,
            Short.class,
            Byte.class,
            Integer.class,
            Long.class,
            char.class,
            Character.class,
            String.class //TODO: ?
    ));


    @Override
    public List<Condition> infer(PointObservationCollection observationCollection) {

        List<AtomicValueObservation> observations = observationCollection.stream()
                .flatMap(Arrays::stream)
                .filter(obs -> obs instanceof AtomicValueObservation)
                .map(obs -> (AtomicValueObservation)obs)
                .filter(obs -> ALLOWED_TYPES.contains(obs.getObservedType()))
                .collect(Collectors.toList())
                ;

        Set values = observations.stream()
                .map(AtomicValueObservation::getValue)
                .collect(Collectors.toSet())
                ;

        Set<Class<?>> types = observations.stream()
                .map(AtomicValueObservation::getObservedType)
                .collect(Collectors.toSet())
                ;

        if(values.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new SetOfValues(values, types));
    }


}
