//package eu.stamp_project.reneri.inference;
//
//
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonSerializationContext;
//import com.google.gson.JsonSerializer;
//import eu.stamp_project.reneri.observations.AtomicValueObservation;
//
//import java.lang.reflect.Type;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//
//public class SetOfValues extends AtomicValueCondition {
//
//    public Set<?> getValues() {
//        return values;
//    }
//
//    private Set<?> values;
//
//    public SetOfValues(Collection<?> values) {
//        this.values = new HashSet<>(values);
//    }
//
//    @Override
//    public boolean holdsFor(AtomicValueObservation observation) {
//        return values.contains(observation.getValue());
//    }
//
//}
