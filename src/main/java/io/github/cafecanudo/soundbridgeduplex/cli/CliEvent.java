package io.github.cafecanudo.soundbridgeduplex.cli;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Optional;
import java.util.OptionalDouble;

public record CliEvent(int schema, String name, JsonObject data) {

    public static final int SUPPORTED_SCHEMA = 1;

    public boolean isSupportedSchema() {
        return schema <= SUPPORTED_SCHEMA;
    }

    public Optional<String> string(String key) {
        return primitive(key).filter(JsonPrimitive::isString).map(JsonPrimitive::getAsString);
    }

    public OptionalDouble number(String key) {
        return primitive(key)
                .filter(JsonPrimitive::isNumber)
                .map(value -> OptionalDouble.of(value.getAsDouble()))
                .orElse(OptionalDouble.empty());
    }

    public Optional<Boolean> bool(String key) {
        return primitive(key).filter(JsonPrimitive::isBoolean).map(JsonPrimitive::getAsBoolean);
    }

    private Optional<JsonPrimitive> primitive(String key) {
        JsonElement element = data.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return Optional.empty();
        }
        return Optional.of(element.getAsJsonPrimitive());
    }
}
