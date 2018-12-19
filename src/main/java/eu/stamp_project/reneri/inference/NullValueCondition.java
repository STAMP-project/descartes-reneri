package eu.stamp_project.reneri.inference;

import eu.stamp_project.reneri.observations.AtomicValueObservation;
import eu.stamp_project.reneri.observations.ValueObservation;

public abstract class NullValueCondition extends TargetedCondition<ValueObservation> {

    public NullValueCondition() {
        super(ValueObservation.class);
    }

    @Override
    public boolean canTarget(ValueObservation observation) {
        if(observation == null) {
            return false;
        }
        if(observation instanceof AtomicValueObservation) {
            return ((AtomicValueObservation) observation).isNullable();
        }
        return true;
    }

}
