package eu.stamp_project.reneri.observations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import java.util.Objects;

public class Observation {

    private String pointcut;

    private ObservedValue value;

    private Observation(String pointcut) {
        this.pointcut = pointcut;
    }

    public Observation(String pointcut, ObservedValue value) {
        Objects.requireNonNull(pointcut);
        Objects.requireNonNull(value);

        this.pointcut = pointcut;
        this.value = value;
    }

    public String getPointcut() {
        return pointcut;
    }

    public ObservedValue getValue() { return value; }

    public static Observation fromString(String definition) throws InvalidObservationException {
        try {
            Gson gson = new Gson();
            JsonObject object = gson.fromJson(definition, JsonObject.class);

            Observation observation = new Observation(get(object, "point"));

            //TODO: If the type of observations increase then refactor
            if (object.has("exception")) {
                observation.value = new ObservedException(get(object, "exception"), get(object, "message"));
            }
            else if (object.has("null")) {
                observation.value = new ObservedNullValue(get(object, "type"), object.get("null").getAsBoolean());
            }
            else {
                observation.value = new ObservedAtomicValue(get(object, "type"), get(object, "value"));
            }

            return observation;
        }
        catch (JsonSyntaxException exc) {
            throw new InvalidObservationException("Malformed JSON line: " + definition, exc);
        }
    }

    private static String get(JsonObject object, String property) throws InvalidObservationException {
        if(!object.has(property)) {
            throw new InvalidObservationException("Observation is expected to have a '" + property + "' attribute.");
        }

        JsonElement propertyValue = object.get(property);
        if(propertyValue.isJsonNull()) {
            return "null";
        }
        return propertyValue.getAsString();
    }



}
