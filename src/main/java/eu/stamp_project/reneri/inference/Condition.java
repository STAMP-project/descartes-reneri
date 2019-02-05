package eu.stamp_project.reneri.inference;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.PointObservationCollection;

import java.lang.reflect.Type;
import java.util.function.Predicate;

public interface Condition extends Predicate<Observation[]> {

    default boolean appliesTo(PointObservationCollection pointObservations) {
        return pointObservations.stream().allMatch(this);
    }

    static JsonSerializer<Condition> getSerializer() {
        return (Condition src, Type typeOfSrc, JsonSerializationContext context) -> new JsonPrimitive(src.getClass().getSimpleName());
    }


}
