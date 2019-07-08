package eu.stamp_project.reneri.amplification;

import java.util.Objects;

public class Mutation {

    private String mutator;

    private Method method;

    public Mutation(String mutator, Method method) {
        this.mutator = mutator;
        this.method = method;
    }

    public String getMutator() {
        return mutator;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mutation mutation = (Mutation) o;
        return Objects.equals(mutator, mutation.mutator) &&
                Objects.equals(method, mutation.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutator, method);
    }

    @Override
    public String toString() {
        return String.format("%s::%s", method, mutator);
    }
}
