package eu.stamp_project.reneri.observations;


public final class CollectionObservation extends ObjectObservation {

    private int size;

    public CollectionObservation(String type, int size, FieldObservation... fields) {
        super(type, fields);
        this.size = size;
    }

    public int getSize() {
        return size;
    }

}
