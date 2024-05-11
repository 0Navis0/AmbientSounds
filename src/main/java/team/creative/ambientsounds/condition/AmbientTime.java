package team.creative.ambientsounds.condition;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import team.creative.ambientsounds.environment.AmbientEnvironment;
import team.creative.creativecore.common.util.math.interpolation.LinearInterpolation;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.type.list.TupleList;

@JsonAdapter(AmbientTime.Serializer.class)
public abstract class AmbientTime {
    
    public static final HashMap<String, AmbientTimePremade> PREMADE = new HashMap<>();
    
    public static final double ANGLE_TO_TIME = 24D / 360;
    public static final double TIME_TO_ANGLE = 360D / 24;
    public static final double FADE = 1;
    
    public static final AmbientTime NONE = new AmbientTime() {
        @Override
        public double value(AmbientEnvironment env) {
            return 0;
        }
        
        @Override
        public JsonElement toJson() {
            return new JsonArray();
        }
    };
    /** From 6am till 6pm */
    public static final AmbientTimePremade DAY = new AmbientTimePremade("day", 1, 6, 18);
    /** From 7pm till 5am */
    public static final AmbientTimePremade NIGHT = new AmbientTimePremade("night", 0, 1, 4, 1, 5, 0, 19, 0, 19, 1, 24, 1);
    /** From 5am till 6am */
    public static final AmbientTimePremade SUNRISE = new AmbientTimePremade("sunrise", 1, 5, 6);
    /** From 6pm till 7pm */
    public static final AmbientTimePremade SUNSET = new AmbientTimePremade("sunset", 1, 18, 19);
    /** From 10:30am till 1:30pm */
    public static final AmbientTimePremade NOON = new AmbientTimePremade("noon", 0.5, 10.5, 13.5);
    /** From 10:30pm till 1:30am */
    public static final AmbientTimePremade MIDNIGHT = new AmbientTimePremade("midnight", 0, 1, 1, 1, 1.5, 0, 22.5, 0, 23, 1, 24, 1);
    
    public abstract double value(AmbientEnvironment env);
    
    public abstract JsonElement toJson();
    
    /** Done by the angle of the sun. 6am is 0 and angle goes to 360 */
    public static class AmbientTimeSunAngle extends AmbientTime {
        
        private final LinearInterpolation<Vec1d> interpolation;
        private final double[] times;
        
        public AmbientTimeSunAngle(double... times) {
            this.times = times;
            if (times.length % 2 != 0)
                throw new IllegalArgumentException("Invalid times " + Arrays.toString(times) + " cannot calculate ambient time");
            TupleList<Double, Vec1d> points = new TupleList<>();
            points.add(times[times.length - 2] - 360, new Vec1d(times[times.length - 1])); // Add the last point to before the start to allow for smooth transition over 0, same is done for the end
            for (int i = 0; i < times.length; i += 2)
                points.add(times[i], new Vec1d(times[i + 1]));
            points.add(times[0] + 360, new Vec1d(times[1]));
            interpolation = new LinearInterpolation<>(points);
        }
        
        @Override
        public double value(AmbientEnvironment env) {
            return interpolation.valueAt(env.sunAngle).x;
        }
        
        @Override
        public JsonElement toJson() {
            JsonObject object = new JsonObject();
            object.add("type", new JsonPrimitive("angle"));
            for (int i = 0; i < times.length; i += 2)
                object.add("" + times[i], new JsonPrimitive(times[i + 1]));
            return object;
        }
        
    }
    
    public static class AmbientTimeHour extends AmbientTime {
        
        private final LinearInterpolation<Vec1d> interpolation;
        private final double[] times;
        
        public AmbientTimeHour(double... times) {
            this.times = times;
            if (times.length % 2 != 0)
                throw new IllegalArgumentException("Invalid times " + Arrays.toString(times) + " cannot calculate ambient time");
            TupleList<Double, Vec1d> points = new TupleList<>();
            if (times[times.length - 2] < 24)
                points.add(times[times.length - 2] - 24, new Vec1d(times[times.length - 1])); // Add the last point to before the start to allow for smooth transition over 0, same is done for the end
            for (int i = 0; i < times.length; i += 2)
                points.add(times[i], new Vec1d(times[i + 1]));
            if (times[0] > 0)
                points.add(times[0] + 24, new Vec1d(times[1]));
            interpolation = new LinearInterpolation<>(points);
        }
        
        @Override
        public double value(AmbientEnvironment env) {
            return interpolation.valueAt(env.dayTimeHour).x;
        }
        
        @Override
        public JsonElement toJson() {
            JsonObject object = new JsonObject();
            for (int i = 0; i < times.length; i += 2)
                object.add("" + times[i], new JsonPrimitive(times[i + 1]));
            return object;
        }
        
    }
    
    public static class AmbientTimePremade extends AmbientTimeHour {
        
        public final String name;
        
        public AmbientTimePremade(String name, double fade, double begin, double end) {
            super(new double[] { begin, 0, (begin + fade), 1, (end - fade), 1, end, 0 });
            this.name = name;
            PREMADE.put(name, this);
        }
        
        public AmbientTimePremade(String name, double... times) {
            super(times);
            this.name = name;
            PREMADE.put(name, this);
        }
        
        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(name);
        }
        
    }
    
    public static class AmbientTimeMulti extends AmbientTime {
        
        public final AmbientTimePremade[] times;
        
        public AmbientTimeMulti(AmbientTimePremade... times) {
            this.times = times;
        }
        
        @Override
        public double value(AmbientEnvironment env) {
            double value = 0;
            for (int i = 0; i < times.length; i++)
                value = Math.max(value, times[i].value(env));
            return value;
        }
        
        @Override
        public JsonElement toJson() {
            JsonArray array = new JsonArray(times.length);
            for (int i = 0; i < times.length; i++)
                array.add(times[i].name);
            return array;
        }
        
    }
    
    public static class Serializer implements JsonDeserializer<AmbientTime>, JsonSerializer<AmbientTime> {
        
        @Override
        public JsonElement serialize(AmbientTime src, Type typeOfSrc, JsonSerializationContext context) {
            return src.toJson();
        }
        
        @Override
        public AmbientTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                if (!json.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Invalid json element given " + json);
                AmbientTimePremade premade = AmbientTime.PREMADE.get(json.getAsString());
                if (premade == null)
                    throw new JsonParseException("Invalid time given " + json);
                return premade;
            }
            if (json.isJsonArray()) {
                JsonArray array = json.getAsJsonArray();
                if (array.isEmpty())
                    return NONE;
                AmbientTimePremade[] content = new AmbientTimePremade[array.size()];
                for (int i = 0; i < content.length; i++) {
                    AmbientTimePremade premade = AmbientTimePremade.PREMADE.get(array.get(i).getAsString());
                    if (premade == null)
                        throw new JsonParseException("Invalid time given " + array.get(i));
                    content[i] = premade;
                }
                return new AmbientTimeMulti(content);
            }
            if (!json.isJsonObject())
                throw new JsonParseException("Invalid json element given " + json);
            JsonObject object = json.getAsJsonObject();
            DoubleList list = new DoubleArrayList();
            for (Entry<String, JsonElement> entry : object.entrySet()) {
                try {
                    list.add(Double.parseDouble(entry.getKey()));
                    list.add(entry.getValue().getAsDouble());
                } catch (NumberFormatException e) {}
            }
            if (object.has("type") && object.get("type").getAsString().equalsIgnoreCase("angle"))
                return new AmbientTimeSunAngle(list.toDoubleArray());
            return new AmbientTimeHour(list.toDoubleArray());
        }
        
    }
}
