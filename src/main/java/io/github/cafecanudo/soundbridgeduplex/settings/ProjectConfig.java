package io.github.cafecanudo.soundbridgeduplex.settings;

import java.util.Objects;

public record ProjectConfig(String launcher, String output, String input, String receiveDirectory) {

    public ProjectConfig {
        launcher = normalize(launcher);
        output = normalize(output);
        input = normalize(input);
        receiveDirectory = normalize(receiveDirectory);
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }
}
