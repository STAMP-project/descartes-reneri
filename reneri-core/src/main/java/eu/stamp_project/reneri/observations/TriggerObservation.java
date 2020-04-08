package eu.stamp_project.reneri.observations;

public class TriggerObservation extends Observation {

    private String containerClass, containerMethod, triggerClass, trigger;

    private int lineNumber, depth;

    public TriggerObservation(String containerClass, String containerMethod, String triggerClass, String trigger, int lineNumber, int depth) {
        this.containerClass = containerClass;
        this.containerMethod = containerMethod;
        this.triggerClass = triggerClass;
        this.trigger = trigger;
        this.lineNumber = lineNumber;
        this.depth = depth;
    }

    public String getContainerClass() {
        return containerClass;
    }

    public String getContainerMethod() {
        return containerMethod;
    }

    public String getTriggerClass() {
        return triggerClass;
    }

    public String getTrigger() {
        return trigger;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getDepth() { return depth; }
}
