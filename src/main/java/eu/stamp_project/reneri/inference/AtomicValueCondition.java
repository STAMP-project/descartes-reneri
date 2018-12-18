package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;

public abstract class AtomicValueCondition implements Condition<AtomicValueObservation> {

    @Override
    public boolean canTarget(AtomicValueObservation observation) {
        return observation != null && !observation.isNull();
    }

}
