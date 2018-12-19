package eu.stamp_project.reneri.inference;

import com.sun.tools.javac.util.List;
import eu.stamp_project.reneri.observations.AtomicValueObservation;

import java.util.HashSet;
import java.util.Set;

public class SetOfValues extends AtomicValueCondition {

    private static Set<Class> ALLOWED_TYPES = new HashSet<>(List.of(
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

    private Set values;

    public SetOfValues(Set values) {
        this.values = values;
    }

    @Override
    public boolean canTarget(AtomicValueObservation observation) {
        return ALLOWED_TYPES.contains(observation.getObservedType());
    }

    @Override
    public boolean holdsFor(AtomicValueObservation observation) {
        return values.contains(observation.getValue());
    }
}
