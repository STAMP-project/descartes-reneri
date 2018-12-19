package eu.stamp_project.reneri.observations;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class Observation {

    private String pointcut;

    public Observation(String pointcut) {
        this.pointcut = pointcut;
    }

    public String getPointcut() {
        return pointcut;
    }

    private static String get(JsonObject object, String property) throws InvalidObservationException {
        if(!object.has(property)) {
            throw new InvalidObservationException("Observation is expected to have a '" + property + "' attribute.");
        }
        return object.get(property).getAsString();
    }

    public static Observation fromLine(String line) throws InvalidObservationException {
        Gson gson = new Gson();
        JsonObject object = gson.fromJson(line, JsonObject.class);

        String pointcut = get(object, "point");

        //TODO: If the type of observations increase then refactor
        if(object.has("exception")) {
            return new ExceptionObservation(pointcut, get(object, "exception"), get(object, "message"));
        }

        if(object.has("null")) {
            return new NullValueObservation(pointcut, object.get("type").getAsString(), object.get("null").getAsBoolean());
        }

        return new AtomicValueObservation(pointcut, get(object, "type"), get(object, "NullValueCondition"));
    }

}
