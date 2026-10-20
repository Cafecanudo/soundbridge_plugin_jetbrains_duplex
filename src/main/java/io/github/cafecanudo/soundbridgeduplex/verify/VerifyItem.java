package io.github.cafecanudo.soundbridgeduplex.verify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record VerifyItem(String path, VerifyStatus status) {

    private static final String PATH = "path";
    private static final String STATUS = "status";

    public static List<VerifyItem> fromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<VerifyItem> items = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            parse(value).ifPresent(items::add);
        }
        return List.copyOf(items);
    }

    public static JsonArray toJson(List<VerifyItem> items) {
        JsonArray array = new JsonArray();
        for (VerifyItem item : items) {
            JsonObject object = new JsonObject();
            object.addProperty(PATH, item.path());
            object.addProperty(STATUS, item.status().wire());
            array.add(object);
        }
        return array;
    }

    private static Optional<VerifyItem> parse(JsonElement value) {
        if (!value.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject object = value.getAsJsonObject();
        if (!object.has(PATH) || !object.has(STATUS)) {
            return Optional.empty();
        }
        return VerifyStatus.fromWire(object.get(STATUS).getAsString())
                .map(status -> new VerifyItem(object.get(PATH).getAsString(), status));
    }
}
