package eu.stamp_project.reneri;

import eu.stamp_project.reneri.observations.TriggerObservation;
import javassist.NotFoundException;

import java.io.IOException;
import java.util.Set;

public interface TriggerCollector {

    Set<TriggerObservation> collectTriggers(String methodName, String methodDesc, String declaringClass, String testClass) throws NotFoundException, IOException;

}
