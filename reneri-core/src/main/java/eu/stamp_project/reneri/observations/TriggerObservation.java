package eu.stamp_project.reneri.observations;

public class TriggerObservation extends Observation {

    private String containerClassDescriptor, containerMethod, triggerClassDescriptor, trigger;

    private int lineNumber, depth;

    public TriggerObservation(String containerClassDescriptor, String containerMethod, String triggerClassDescriptor, String trigger, int lineNumber, int depth) {
        this.containerClassDescriptor = containerClassDescriptor;
        this.containerMethod = containerMethod;
        this.triggerClassDescriptor = triggerClassDescriptor;
        this.trigger = trigger;
        this.lineNumber = lineNumber;
        this.depth = depth;
    }

    public String getContainerClassDescriptor() {
        return containerClassDescriptor;
    }

    public String getContainerMethod() {
        return containerMethod;
    }

    public String getTriggerClassDescriptor() {
        return triggerClassDescriptor;
    }

    public String getTrigger() {
        return trigger;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
