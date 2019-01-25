package eu.stamp_project.reneri;

import org.pitest.mutationtest.engine.MutationIdentifier;

import java.util.ArrayList;
import java.util.List;

public class MethodRecord {

    private String name;

    private String description;

    private String packageName;

    private String className;

    private List<MutationIdentifier> mutations;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDeclaringClass() {
        return className;
    }

    public String getPackage() {
        return packageName;
    }

    public String getClassQualifiedName() {
        return packageName + "." + className;
    }

    public List<MutationIdentifier> getMutations() {
        return mutations;
    }

    public MethodRecord(String name, String description, String className, String packageName) {
        this(name, description, className, packageName, new ArrayList<>());
    }

    public MethodRecord(String name, String description, String className, String packageName, List<MutationIdentifier> mutations) {
        this.name = name;
        this.description = description;
        this.className = className;
        this.packageName = packageName;
        this.mutations = mutations;
    }

    @Override
    public String toString() {
        return String.format("%s.%s.%s%s", packageName, className, name, description);
    }


}
