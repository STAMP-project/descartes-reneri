package eu.stamp_project.reneri.amplification;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AmplificationTarget {

    @Expose(serialize = false)
    private Test test;

    @Expose(serialize = false)
    private HashSet<Mutation> mutations = new HashSet<>();

    @SerializedName("original")
    @Expose(serialize = false)
    private String originalTestClass;

    public AmplificationTarget(Test test) {
        this.test = test;
        originalTestClass = test.getClassName();
    }

    public AmplificationTarget(Test test, String actualTestClass) {
        this(test);
        if(actualTestClass != null && !actualTestClass.equals(test.getClassName())) {
            this.test = new Test(actualTestClass, test.getMethod());
            originalTestClass = test.getClassName();
        }
    }

    public Test getTest() {
        return test;
    }

    public Set<Mutation> getMutations() {
        return mutations;
    }

    public String getOriginalTestClass() {
        return originalTestClass;
    }

    public boolean isOriginalTarget() {
        return originalTestClass.equals(test.getClassName());
    }
}
