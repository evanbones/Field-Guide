package com.evandev.fieldguide.api.variant;

import com.evandev.fieldguide.platform.Services;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class VariantConditionEvaluator {

    /**
     * Evaluates a JSON array of conditions. By default, an array acts as an "AND" check.
     * If the array is null or empty, it returns true.
     */
    public static boolean evaluateAll(JsonArray conditions) {
        if (conditions == null || conditions.isEmpty()) return true;

        for (JsonElement element : conditions) {
            if (element.isJsonObject() && !evaluate(element.getAsJsonObject())) {
                return false; // If any condition fails, the whole array fails
            }
        }
        return true;
    }

    /**
     * Recursively evaluates a single condition object.
     */
    public static boolean evaluate(JsonObject condition) {
        if (!condition.has("type")) return false;

        String type = condition.get("type").getAsString();

        return switch (type) {
            case "mod_loaded" -> {
                String modid = condition.get("modid").getAsString();
                yield Services.PLATFORM.isModLoaded(modid);
            }
            case "not" -> {
                JsonObject value = condition.getAsJsonObject("value");
                yield !evaluate(value);
            }
            case "and" -> {
                JsonArray values = condition.getAsJsonArray("values");
                yield evaluateAll(values);
            }
            case "or" -> {
                JsonArray values = condition.getAsJsonArray("values");
                boolean result = false;
                for (JsonElement element : values) {
                    if (element.isJsonObject() && evaluate(element.getAsJsonObject())) {
                        result = true;
                        break;
                    }
                }
                yield result;
            }
            default -> false;
        };
    }
}
