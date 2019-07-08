package eu.stamp_project.reneri.amplification;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class Test {

    @SerializedName("class")
    private String className;

    private String method;

    public Test(String className) {
        this(className, null);
    }

    public Test(String className, String method) {
        this.className = className;
        this.method = method;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public boolean isFullClass() { return method == null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test test = (Test) o;
        return Objects.equals(className, test.className) &&
                Objects.equals(method, test.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, method);
    }

    @Override
    public String toString() {
        if(method == null) {
            return className;
        }
        return String.format("%s.%s", className, method);
    }
}