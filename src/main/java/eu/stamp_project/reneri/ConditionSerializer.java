//package eu.stamp_project.reneri;
//
//import com.google.gson.*;
//import eu.stamp_project.reneri.inference.Condition;
//import eu.stamp_project.reneri.inference.SetOfValues;
//
//import java.lang.reflect.Type;
//
//public class ConditionSerializer implements JsonSerializer<Condition> {
//
//    @Override
//    public JsonElement serialize(Condition src, Type typeOfSrc, JsonSerializationContext context) {
//        if(src instanceof SetOfValues) {
//            JsonObject result = new JsonObject();
//            result.addProperty("type", "SetOfValues");
//            result.add("values", context.serialize(((SetOfValues) src).getValues()));
//            return result;
//        }
//
//        return new JsonPrimitive(src.getClass().getSimpleName());
//    }
//}
