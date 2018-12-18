package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.ValueObservation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NullAndNotNull extends NullCondition {

    @Override
    public boolean holds(ValueObservation observation) {
        return false;
    }

    @Override
    public boolean appliesTo(ValueObservation[] observations) {
        return Arrays.stream(observations)
                .filter(this::canTarget)
                .map(ValueObservation::isNull)
                .collect(Collectors.toSet()).size() == 2;
    }
}
