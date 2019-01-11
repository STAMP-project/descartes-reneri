package eu.stamp_project.reneri;

import com.google.gson.*;
import eu.stamp_project.reneri.inference.Condition;
import spoon.reflect.cu.SourcePosition;

import java.lang.reflect.Type;

public class ConditionMismatch {

    private final String pointcut;

    private final Condition condition;

    private final SourcePosition position;

    public String getPointcut() {
        return pointcut;
    }

    public Condition getCondition() {
        return condition;
    }

    public SourcePosition getPosition() {
        return position;
    }

    public ConditionMismatch(String pointcut, Condition condition, SourcePosition position) {
        this.pointcut = pointcut;
        this.condition = condition;
        this.position = position;
    }

    public static JsonSerializer<ConditionMismatch> getSerializer() {
        return new JsonSerializer<ConditionMismatch>() {
            @Override
            public JsonElement serialize(ConditionMismatch src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject object = new JsonObject();

                object.addProperty("pointcut", src.pointcut);
                object.addProperty("condition", src.condition.getClass().getSimpleName());

                if(src.position != null) {

                    object.addProperty("file", src.position.getFile().getAbsolutePath());

                    object.addProperty("line", src.position.getLine());
                    object.addProperty("column", src.position.getColumn());
                    object.addProperty("endLine", src.position.getEndLine());
                    object.addProperty("endColumn", src.position.getEndColumn());
                }

                return object;

            }
        };
    }
}
