package eu.stamp_project.reneri.observations;

public class ObservedStackTrace extends ObservedValue {

    private StackTraceElement[] trace;

    public StackTraceElement[] getTrace() {
        return trace;
    }

    public ObservedStackTrace(StackTraceElement[] trace) {
        super("java.lang.StackTraceElement[]");
        this.trace = trace;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return stackTraceEquals((ObservedStackTrace) obj);
    }

    protected boolean stackTraceElementEquals(StackTraceElement one, StackTraceElement other) {
        return  // Same location or location is unknown for one of them
                // Not caring about the file name
                ( one.getLineNumber() == other.getLineNumber()  || one.getLineNumber() < 0 || other.getLineNumber() < 0 ) &&
                one.getMethodName().equals(other.getMethodName()) &&
                one.getClassName().equals(other.getClassName());
    }

    protected boolean stackTraceEquals(ObservedStackTrace that) {
        if(trace.length != that.trace.length) {
            return false;
        }

        for(int index = 0; index < trace.length; index++) {
            if(!stackTraceElementEquals(trace[index], that.trace[index])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        StringBuilder builder = new StringBuilder();
        int top = Math.min(5, trace.length);
        for(int index = 0; index < top; index++) {
            builder.append(trace[index].getMethodName());
        }
        return builder.toString().hashCode();
    }
}
