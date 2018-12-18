package eu.stamp_project.reneri.observations;

public class InvalidObservationException extends Exception {

    public InvalidObservationException(String message) {
        super(message);
    }

    public InvalidObservationException(String message, Throwable cause) {
        super(message, cause);
    }


}
