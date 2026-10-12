package io.github.cafecanudo.soundbridgeduplex.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle;
import io.github.cafecanudo.soundbridgeduplex.cli.CliException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public record DevicesDocument(
        List<AudioDevice> outputs,
        List<AudioDevice> inputs,
        Map<String, String> aliases,
        DeviceDefaults defaults,
        List<LinkProfile> profiles,
        String configPath) {

    private static final String OUTPUTS = "outputs";
    private static final String INPUTS = "inputs";
    private static final String ALIASES = "aliases";
    private static final String DEFAULTS = "defaults";
    private static final String PROFILES = "profiles";
    private static final String CONFIG = "config";
    private static final String DEFAULT_OUT = "out";
    private static final String DEFAULT_IN = "in";
    private static final String DEFAULT_DIR = "dir";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CHANNELS = "channels";

    public static DevicesDocument parse(String json) throws CliException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return new DevicesDocument(
                    devices(root.getAsJsonArray(OUTPUTS)),
                    devices(root.getAsJsonArray(INPUTS)),
                    strings(root.getAsJsonObject(ALIASES)),
                    defaults(root.getAsJsonObject(DEFAULTS)),
                    profiles(root.getAsJsonObject(PROFILES)),
                    text(root, CONFIG));
        } catch (JsonParseException | IllegalStateException | ClassCastException | NullPointerException e) {
            throw new CliException(SoundBridgeBundle.message("cli.invalidDevices", e.getMessage()), e);
        }
    }

    private static List<AudioDevice> devices(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(o -> new AudioDevice(text(o, ID), text(o, NAME), o.get(CHANNELS).getAsInt()))
                .toList();
    }

    private static Map<String, String> strings(JsonObject object) {
        Map<String, String> map = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> map.put(entry.getKey(), entry.getValue().getAsString()));
        return Map.copyOf(map);
    }

    private static List<LinkProfile> profiles(JsonObject object) {
        if (object == null) {
            return List.of();
        }
        return object.entrySet().stream()
                .map(entry -> {
                    JsonObject fields = entry.getValue().getAsJsonObject();
                    return new LinkProfile(entry.getKey(), text(fields, DEFAULT_OUT), text(fields, DEFAULT_IN),
                            text(fields, DEFAULT_DIR));
                })
                .sorted(Comparator.comparing(LinkProfile::name))
                .toList();
    }

    private static DeviceDefaults defaults(JsonObject object) {
        if (object == null) {
            return new DeviceDefaults("", "", "");
        }
        return new DeviceDefaults(text(object, DEFAULT_OUT), text(object, DEFAULT_IN), text(object, DEFAULT_DIR));
    }

    private static String text(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }
}
