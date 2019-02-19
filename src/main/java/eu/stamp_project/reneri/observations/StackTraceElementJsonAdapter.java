package eu.stamp_project.reneri.observations;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class StackTraceElementJsonAdapter extends TypeAdapter<StackTraceElement> {

    private static final String CLASS_ATRIBUTE = "class";
    private static final String METHOD_ATTRIBUTE = "method";
    private static final String LINE_ATTRIBUTE = "line";

    @Override
    public void write(JsonWriter out, StackTraceElement value) throws IOException {
        out.beginObject()
                .name(CLASS_ATRIBUTE).value(value.getClassName())
                .name(METHOD_ATTRIBUTE).value(value.getMethodName())
                .name(LINE_ATTRIBUTE).value(value.getLineNumber())
                .endObject();
    }

    @Override
    public StackTraceElement read(JsonReader in) throws IOException {
        String declaringClass = "";
        String methodName = "";
        int lineNumber = -1;
        in.beginObject();
        while(in.hasNext()) {
            String name = in.nextName();
            if(name.equals(CLASS_ATRIBUTE)) {
                declaringClass = in.nextString();
            }
            else if(name.equals(METHOD_ATTRIBUTE)) {
                methodName = in.nextString();
            }
            else if(name.equals(LINE_ATTRIBUTE)) {
                lineNumber = in.nextInt();
            }
        }
        in.endObject();
        return new StackTraceElement(declaringClass, methodName, null, lineNumber);
    }
}
