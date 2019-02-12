//package eu.stamp_project.reneri.inference;
//
//import eu.stamp_project.reneri.observations.AtomicValueObservation;
//import eu.stamp_project.reneri.observations.ValueObservation;
//
//public abstract class NullValueCondition extends TargetedCondition<ValueObservation> {
//
//    public NullValueCondition() {
//        super(ValueObservation.class);
//    }
//
//    @Override
//    public boolean canTarget(ValueObservation observation) {
//        if(observation == null) {
//            return false;
//        }
//        if(observation instanceof AtomicValueObservation) {
//            return ((AtomicValueObservation) observation).isNullable();
//        }
//        return true;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        // All instances of the derived classes are equal since they all implement the same condition
//        // they could be singleton by we are avoiding the pattern ;)
//        return obj.getClass().equals(getClass());
//    }
//
//
//}
