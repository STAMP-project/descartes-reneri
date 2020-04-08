package eu.stamp_project.reneri;

import javassist.CannotCompileException;

public class InvalidTransformationException extends Exception {

    public InvalidTransformationException(CannotCompileException exc) {
        super("Transformed code could not be compiled", exc);

    }

}
