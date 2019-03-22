package eu.stamp_project.reneri.diff;

import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.ObservedValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class DiffOnValues {

    private ObservedValueMap expected;

    private BagOfValues unexpected;

    public DiffOnValues(ObservedValueMap expected) {
        this.expected = expected;
        unexpected = new BagOfValues();
    }

    public void add(Observation observation) {
        if(!expected.has(observation)) {
            unexpected.add(observation);
        }
    }

    public boolean hasDiff() {
        return !unexpected.isEmpty();
    }

    public Collection<DiffRecord> getDiff() {
        return unexpected
                .stream()
                .map(
                        (entry) ->
                                new DiffRecord(
                                        entry.getKey(),
                                        // The set is kept here to keep these files compatible
                                        // with the files that were generated before
                                        Collections.singleton(expected.get(entry.getKey())),
                                        entry.getValue()
                                )
                )
                .collect(Collectors.toList());
    }


    public static class DiffRecord {

        private String pointcut;

        private Set<ObservedValue> expected;

        private Set<ObservedValue> unexpected;

        public DiffRecord(String pointcut, Set<ObservedValue> expected, Set<ObservedValue> unexpected) {
            this.pointcut = pointcut;
            this.expected = expected;
            this.unexpected = unexpected;
        }

        public String getPointcut() {
            return pointcut;
        }

        public Set<ObservedValue> getExpected() {
            return expected;
        }

        public Set<ObservedValue> getUnexpected() {
            return unexpected;
        }
    }
}
