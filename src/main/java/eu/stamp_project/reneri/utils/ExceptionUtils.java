package eu.stamp_project.reneri.utils;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class ExceptionUtils {

    // Credits to http://web.archive.org/web/20140406000326/http://java8blog.com/post/37385501926/fixing-checked-exceptions-in-java-8

    @FunctionalInterface
    public static interface ExceptionWrapper<E> {
        E wrap(Exception e);
    }

    public static <T> T propagate(Callable<T> callable) throws RuntimeException {
        return propagate(callable, RuntimeException::new);
    }

    public static <T, E extends Throwable> T propagate(Callable<T> callable, ExceptionWrapper<E> wrapper) throws E {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw wrapper.wrap(e);
        }
    }

}
