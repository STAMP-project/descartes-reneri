package eu.stamp_project.reneri.observations;

public class InvalidObservationFileException extends Exception {


    private String fileName;

    public InvalidObservationFileException(String message, String fileName, Throwable cause) {
        super(String.format("%s in file: %s", message, fileName), cause);

        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
