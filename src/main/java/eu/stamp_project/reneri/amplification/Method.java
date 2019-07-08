package eu.stamp_project.reneri.amplification;

import com.google.gson.annotations.SerializedName;
import javassist.CtClass;
import javassist.CtMethod;

import java.util.Objects;

public class Method {

    //TODO: Migrate all other method abstractions to this one

    @SerializedName("class")
    private String className;

    private String name;

    @SerializedName("package")
    private String packageName;

    private String description;

    public Method(String className, String name, String packageName, String description) {
        this.className = className;
        this.name = name;
        this.packageName = packageName;
        this.description = description;
    }

    public Method(CtMethod method) {
        this(
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                method.getDeclaringClass().getPackageName(),
                method.getMethodInfo2().getDescriptor()
        );
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getDescription() {
        return description;
    }

    public String getClassQualifiedName() { return String.format("%s.%s", packageName, className); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;
        return Objects.equals(className, method.className) &&
                Objects.equals(name, method.name) &&
                Objects.equals(packageName, method.packageName) &&
                Objects.equals(description, method.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, name, packageName, description);
    }

    @Override
    public String toString() {
        return String.format("%s.%s.%s%s", packageName, className, name, description);
    }
}
