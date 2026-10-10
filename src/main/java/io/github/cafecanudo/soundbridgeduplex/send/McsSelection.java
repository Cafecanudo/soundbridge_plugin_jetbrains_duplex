package io.github.cafecanudo.soundbridgeduplex.send;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public record McsSelection(Kind kind, Integer value) {

    public enum Kind {
        AUTO,
        FIXED
    }

    public static final McsSelection AUTO = new McsSelection(Kind.AUTO, null);

    public static McsSelection fixed(int value) {
        return new McsSelection(Kind.FIXED, value);
    }

    public static McsSelection of(Integer value) {
        return value == null ? AUTO : fixed(value);
    }

    @Override
    public String toString() {
        return switch (kind) {
            case AUTO -> message("send.mcs.auto");
            case FIXED -> "MCS " + value;
        };
    }
}
