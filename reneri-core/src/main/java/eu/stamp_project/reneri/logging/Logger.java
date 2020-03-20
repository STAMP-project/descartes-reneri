package eu.stamp_project.reneri.logging;

import eu.stamp_project.reneri.observations.MethodInvocationObservation;
import eu.stamp_project.reneri.observations.Observation;
import eu.stamp_project.reneri.observations.StaticTypeObservation;
import javassist.expr.MethodCall;
import org.mapdb.DataInput2;

import java.util.stream.IntStream;

import static eu.stamp_project.reneri.observations.Observations.observe;


public abstract class Logger {

    public static Logger logger;

    public abstract void logObservation(Observation observation);

    public static void log(Observation observation) {
        if(logger != null) {
            logger.logObservation(observation);
        }
    }

}
