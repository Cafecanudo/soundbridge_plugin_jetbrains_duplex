package io.github.cafecanudo.soundbridgeduplex.cli;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Optional;

public final class CliEventParser {

    private static final String SCHEMA_KEY = "schema";
    private static final String EVENT_KEY = "event";
    private static final String JSON_OBJECT_START = "{";

    private CliEventParser() {
    }

    public static Optional<CliEvent> parse(String line) {
        String text = line.strip();
        if (!text.startsWith(JSON_OBJECT_START)) {
            return Optional.empty();
        }
        return toObject(text).flatMap(CliEventParser::toEvent);
    }

    private static Optional<JsonObject> toObject(String text) {
        try {
            JsonElement element = JsonParser.parseString(text);
            return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
        } catch (JsonParseException e) {
            return Optional.empty();
        }
    }

    private static Optional<CliEvent> toEvent(JsonObject object) {
        JsonElement schema = object.remove(SCHEMA_KEY);
        JsonElement event = object.remove(EVENT_KEY);
        if (!isInt(schema) || !isString(event)) {
            return Optional.empty();
        }
        return Optional.of(new CliEvent(schema.getAsInt(), event.getAsString(), object));
    }

    private static boolean isInt(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    private static boolean isString(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }
}
