package io.github.cafecanudo.soundbridgeduplex.client;

import java.util.Objects;

public record DeviceDefaults(String output, String input, String directory) {

    public DeviceDefaults {
        output = normalize(output);
        input = normalize(input);
        directory = normalize(directory);
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }
}
