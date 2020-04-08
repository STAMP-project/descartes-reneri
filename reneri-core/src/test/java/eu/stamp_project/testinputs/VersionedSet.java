package eu.stamp_project.testinputs;

import java.util.ArrayList;

public class VersionedSet<T> {


    private long version = 0;
    private ArrayList<T> elements = new ArrayList<>();

    public void add(T item) {
        if (elements.contains(item)) {
            return;
        }
        elements.add(item);
        incrementVersion();
    }

    private void incrementVersion() { version++; }

    public int size() { return elements.size(); }

    public boolean isEmpty() { return size() == 0; }

    public boolean contains(Object item) { return elements.contains(item); }

    protected long getVersion() { return version; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        VersionedSet<?> that = (VersionedSet<?>) other;
        if(that.size() != size()) return false;

        for(T item : elements) {
            if(!that.contains(item)) {
                return false;
            }
        }
        return true;
    }

    public VersionedSet<T> intersect(VersionedSet<T> other) {
        if (isEmpty() || other.isEmpty()) {
            return new VersionedSet<>();
        }
        VersionedSet<T> result = new VersionedSet<>();
        for(T item : elements) {
            if(other.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

}
