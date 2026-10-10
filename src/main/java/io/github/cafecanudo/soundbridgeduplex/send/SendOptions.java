package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.Objects;

public record SendOptions(String destination, Integer zipLevel, McsSelection mcs, boolean replaceExisting) {

    public static final int DEFAULT_ZIP_LEVEL = 9;
    public static final int MIN_ZIP_LEVEL = 0;
    public static final SendOptions DEFAULT = new SendOptions("", DEFAULT_ZIP_LEVEL, McsSelection.AUTO, false);

    public SendOptions {
        destination = Objects.requireNonNullElse(destination, "").strip();
        mcs = Objects.requireNonNullElse(mcs, McsSelection.AUTO);
    }

    public boolean compressed() {
        return zipLevel != null;
    }
}
