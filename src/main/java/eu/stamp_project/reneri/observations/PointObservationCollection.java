package eu.stamp_project.reneri.observations;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class PointObservationCollection {

    private String pointcut;
    private List<Observation[]> archives = new LinkedList<>();

    public PointObservationCollection(String pointcut, List<Observation> observations) {
        this.pointcut = pointcut;
        addArchive(observations);
    }

    public void addArchive(List<Observation> archive) {
        archives.add(archive.stream().toArray(Observation[]::new));
    }

    public int size() {
        return archives.size();
    }

    public Observation[] get(int index) {
        return archives.get(index);
    }

    public Stream<Observation[]> stream() {
        return archives.stream();
    }


}
