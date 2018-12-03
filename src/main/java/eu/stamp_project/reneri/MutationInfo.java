package eu.stamp_project.reneri;

import com.google.gson.JsonObject;

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

    public boolean matches(String name, String desc) {
        return methodName.equals(name) && methodDescription.equals(desc);
    }





}
