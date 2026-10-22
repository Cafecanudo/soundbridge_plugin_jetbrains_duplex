package io.github.cafecanudo.soundbridgeduplex.send;

public record SendSnapshot(SendMode mode, String destination, Integer zipLevel, Integer mcs, boolean replaceExisting,
                           boolean packageFolders, boolean copyMemory) {

    public SendSnapshot(SendMode mode, String destination, Integer zipLevel, Integer mcs, boolean replaceExisting) {
        this(mode, destination, zipLevel, mcs, replaceExisting, false, false);
    }

    public static SendSnapshot of(SendMode mode, String destination, SendOptions options) {
        Integer mcs = options.mcs().kind() == McsSelection.Kind.FIXED ? options.mcs().value() : null;
        return new SendSnapshot(mode, destination, options.zipLevel(), mcs, options.replaceExisting(),
                options.packageFolders(), options.copyMemory());
    }
}
