package io.github.cafecanudo.soundbridgeduplex.settings;

import java.util.Objects;

public record ProjectConfig(String launcher, String output, String input, String receiveDirectory, boolean bidirectional,
                            String profile) {

    public ProjectConfig {
        launcher = normalize(launcher);
        output = normalize(output);
        input = normalize(input);
        receiveDirectory = normalize(receiveDirectory);
        profile = normalize(profile);
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }
}
