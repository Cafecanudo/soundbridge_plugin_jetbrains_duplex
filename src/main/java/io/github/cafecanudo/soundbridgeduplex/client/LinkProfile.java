package io.github.cafecanudo.soundbridgeduplex.client;

import java.util.Objects;

public record LinkProfile(String name, String output, String input, String directory) {

    public LinkProfile {
        name = Objects.requireNonNullElse(name, "").strip();
        output = Objects.requireNonNullElse(output, "").strip();
        input = Objects.requireNonNullElse(input, "").strip();
        directory = Objects.requireNonNullElse(directory, "").strip();
    }
}
