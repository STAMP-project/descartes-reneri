package eu.stamp_project.reneri;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class MutationInfo {

    public String mutator;

    public String className;

    public String packageName;

    public String methodName;

    public String methodDescription;

    public MutationInfo(String mutator, String className, String packageName, String methodName, String methodDescription) {
        this.mutator = mutator;
        this.className = className;
        this.packageName = packageName;
        this.methodName = methodName;
        this.methodDescription = methodDescription;
    }

    public MutationInfo(JsonObject obj) {

        mutator = obj.getAsJsonPrimitive("mutator").getAsString();

        JsonObject methodInformation = obj.getAsJsonObject("method");
        className = methodInformation.getAsJsonPrimitive("class").getAsString();
        packageName = methodInformation.getAsJsonPrimitive("package").getAsString();
        methodName = methodInformation.getAsJsonPrimitive("name").getAsString();
        methodDescription = methodInformation.getAsJsonPrimitive("description").getAsString();

    }

    public String getClassQualifiedName() {
        return packageName + "." + className;
    }

    public boolean matches(String name, String desc) {
        return methodName.equals(name) && methodDescription.equals(desc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MutationInfo that = (MutationInfo) o;
        return Objects.equals(mutator, that.mutator) &&
                Objects.equals(className, that.className) &&
                Objects.equals(packageName, that.packageName) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(methodDescription, that.methodDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutator, className, packageName, methodName, methodDescription);
    }
}
