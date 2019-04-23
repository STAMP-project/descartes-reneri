package eu.stamp_project.reneri.diff;

import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.ObservedValue;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.util.Map;
import java.util.Set;

public class ObservedValueMap {

    //TODO: Change to MapDB
    private Map<String, Object> observedValues;
    private Set<String> deletedKeys;

    public ObservedValueMap() {
        DB db = DBMaker.memoryDB().make();

        //TODO: Try with a tempfileDB

        //TODO: Define a serializer, so we can put the static type here
        observedValues = db.hashMap("observed-values", Serializer.STRING, Serializer.JAVA).createOrOpen();
        deletedKeys = db.hashSet("deleted-keys", Serializer.STRING).createOrOpen();
    }

    public void put(Observation observation) {

        String key = observation.getPointcut();
        if(deletedKeys.contains(key)) {
            // If the observation was already banned, then don't keep it
            return;
        }

        Object currentValue = observedValues.get(key);
        ObservedValue valueToInsert = observation.getValue();
        if(currentValue == null) {
            observedValues.put(key, valueToInsert);
        }
        else if(!currentValue.equals(valueToInsert)) {
            observedValues.remove(key);
            deletedKeys.add(key);
        }


    }

    public ObservedValue get(String pointcut) {
        return (ObservedValue) observedValues.get(pointcut);
    }

    public boolean has(Observation observation) {
        Object value = observedValues.get(observation.getPointcut());
        return value != null && value.equals(observation.getValue());
    }

    /*
    Returns true if the key is contained and the value is different
    from the value of the given observation.
     */
    public boolean differsFrom(Observation observation) {
        Object value = observedValues.get(observation.getPointcut());
        return value != null && !value.equals(observation.getValue());

    }

    public boolean isEmpty() {
        return observedValues.isEmpty();
    }

}
