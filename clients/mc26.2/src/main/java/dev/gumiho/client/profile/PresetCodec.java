package dev.gumiho.client.profile;

import com.google.gson.*;
import dev.gumiho.client.config.GumihoConfig;
import java.lang.reflect.Field;
import java.util.Set;

/** Portable preferences only: never imports server permissions or player identities. */
public final class PresetCodec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> PRIVATE = Set.of("schemaVersion", "allowSingleplayer", "allowedServers",
            "playerGroups", "pinnedTarget", "activeProfile");
    public enum Kind { KIT, HUD }
    private PresetCodec() { }
    public static GumihoConfig copy(GumihoConfig config) {
        return JSON.fromJson(JSON.toJson(config), GumihoConfig.class);
    }
    private static boolean portable(Field field, Kind kind) {
        String name = field.getName();
        if (PRIVATE.contains(name)) return false;
        if (kind == Kind.HUD) return name.equals("hudX") || name.equals("hudY") || name.equals("hudScales");
        return field.getType().isPrimitive() || field.getType() == String.class
                || name.equals("modules") || name.equals("hudX") || name.equals("hudY") || name.equals("hudScales");
    }
    public static String encode(GumihoConfig config, Kind kind, String game) {
        JsonObject values = JSON.toJsonTree(config).getAsJsonObject();
        JsonObject settings = new JsonObject();
        for (Field field : GumihoConfig.class.getFields())
            if (portable(field, kind) && values.has(field.getName()))
                settings.add(field.getName(), values.get(field.getName()));
        JsonObject result = new JsonObject();
        result.addProperty("format", "GUMIHO_PRESET");
        result.addProperty("version", 1);
        result.addProperty("kind", kind.name());
        result.addProperty("minecraft", game);
        result.add("settings", settings);
        return JSON.toJson(result);
    }
    public static GumihoConfig merge(String text, Kind kind, GumihoConfig current) {
        if (text == null || text.length() > 65536) throw new IllegalArgumentException("Preset is too large");
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            if (!root.get("format").getAsString().equals("GUMIHO_PRESET")
                    || !root.get("version").isJsonPrimitive() || !root.get("version").getAsJsonPrimitive().isNumber()
                    || root.get("version").getAsDouble() != 1 || !root.get("kind").getAsString().equals(kind.name()))
                throw new IllegalArgumentException("Wrong preset type or format version");
            JsonObject source = root.getAsJsonObject("settings");
            if (source == null || source.size() == 0) throw new IllegalArgumentException("Empty preset");
            JsonObject merged = JSON.toJsonTree(current).getAsJsonObject();
            int accepted = 0;
            for (Field field : GumihoConfig.class.getFields()) {
                String name = field.getName();
                if (!portable(field, kind) || !source.has(name)) continue;
                JsonElement value = source.get(name);
                validate(field, value);
                if (name.equals("modules") || name.equals("hudX") || name.equals("hudY") || name.equals("hudScales")) {
                    JsonObject entries = merged.getAsJsonObject(name);
                    if (entries == null) { entries = new JsonObject(); merged.add(name, entries); }
                    for (var entry : value.getAsJsonObject().entrySet()) entries.add(entry.getKey(), entry.getValue().deepCopy());
                } else merged.add(name, value.deepCopy());
                accepted++;
            }
            if (accepted == 0) throw new IllegalArgumentException("No compatible settings");
            // Partial layouts must contain matched X/Y entries, not silently move only one axis.
            if (source.has("hudX") != source.has("hudY")
                    || (source.has("hudX") && !source.getAsJsonObject("hudX").keySet().equals(source.getAsJsonObject("hudY").keySet())))
                throw new IllegalArgumentException("Layout must contain matching X and Y positions");
            return JSON.fromJson(merged, GumihoConfig.class);
        } catch (IllegalArgumentException error) { throw error; }
        catch (RuntimeException error) { throw new IllegalArgumentException("Invalid preset JSON", error); }
    }
    private static void validate(Field field, JsonElement value) {
        String name = field.getName();
        if (value == null || value.isJsonNull()) throw new IllegalArgumentException("Missing " + name);
        if (name.equals("modules") || name.equals("hudX") || name.equals("hudY") || name.equals("hudScales")) {
            if (!value.isJsonObject() || value.getAsJsonObject().size() > 512)
                throw new IllegalArgumentException("Invalid " + name);
            for (var entry : value.getAsJsonObject().entrySet()) {
                if (!entry.getKey().matches("[a-z0-9_]{1,80}")) throw new IllegalArgumentException("Invalid module ID");
                JsonElement cell = entry.getValue();
                if (!cell.isJsonPrimitive()) throw new IllegalArgumentException("Invalid map value");
                if (name.equals("modules")) {
                    if (!cell.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid toggle");
                } else if (name.equals("hudScales")) {
                    if (!cell.getAsJsonPrimitive().isNumber() || !Double.isFinite(cell.getAsDouble())
                            || cell.getAsDouble()<0.5 || cell.getAsDouble()>1.25)
                        throw new IllegalArgumentException("Invalid HUD scale");
                } else {
                    double number = cell.getAsDouble();
                    if (!cell.getAsJsonPrimitive().isNumber() || !Double.isFinite(number)
                            || number != Math.rint(number) || Math.abs(number) > 100000)
                        throw new IllegalArgumentException("Invalid HUD position");
                }
            }
        } else if (field.getType() == boolean.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Invalid boolean " + name);
        } else if (field.getType() == String.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().length() > 80)
                throw new IllegalArgumentException("Invalid text " + name);
        } else {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                    || !Double.isFinite(value.getAsDouble()) || Math.abs(value.getAsDouble()) > 100000)
                throw new IllegalArgumentException("Invalid number " + name);
            if (field.getType() == int.class && value.getAsDouble() != Math.rint(value.getAsDouble()))
                throw new IllegalArgumentException("Invalid integer " + name);
        }
    }
}
