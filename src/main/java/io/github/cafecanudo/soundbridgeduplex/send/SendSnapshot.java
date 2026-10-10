package io.github.cafecanudo.soundbridgeduplex.send;

public record SendSnapshot(SendMode mode, String destination, Integer zipLevel, Integer mcs, boolean replaceExisting) {

    public static SendSnapshot of(SendMode mode, String destination, SendOptions options) {
        Integer mcs = options.mcs().kind() == McsSelection.Kind.FIXED ? options.mcs().value() : null;
        return new SendSnapshot(mode, destination, options.zipLevel(), mcs, options.replaceExisting());
    }
}
