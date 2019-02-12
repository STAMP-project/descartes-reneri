package eu.stamp_project.reneri.diff;

import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.ObservedValue;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class BagOfValues {

    private HashMap<String, Set<ObservedValue>>  observedValues;

    public BagOfValues() { observedValues = new HashMap<>(); }

    public BagOfValues(int initialCapacity) { observedValues = new HashMap<>(initialCapacity); }

    public void add(Observation observation) {
        observedValues
                .computeIfAbsent(observation.getPointcut(), (key) -> new HashSet<>())
                .add(observation.getValue())
        ;
    }

    public boolean containsPoint(String point) {
        return observedValues.containsKey(point);
    }

    public boolean containsObservation(Observation observation) {
        Set<ObservedValue> values = observedValues.get(observation.getPointcut());
        if(values == null) {
            return false;
        }
        return values.contains(observation.getValue());
    }

    public Set<ObservedValue> getObservedValues(String pointcut) {
        return observedValues.getOrDefault(pointcut, Collections.emptySet());
    }

    public int getPointCount() {
        return observedValues.size();
    }

    public boolean isEmpty() { return observedValues.isEmpty(); }

    public void forEach(BiConsumer<String, Set<ObservedValue>> action) {
        observedValues.forEach(action);
    }

    public Stream<Map.Entry<String, Set<ObservedValue>>>  stream() {
        return observedValues.entrySet().stream();
    }
}
