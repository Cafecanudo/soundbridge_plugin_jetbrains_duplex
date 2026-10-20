package io.github.cafecanudo.soundbridgeduplex.verify;

import java.util.Arrays;
import java.util.Optional;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum VerifyStatus {
    DIFFERS("differs"),
    MISSING("missing"),
    EXISTS("exists");

    private final String wire;

    VerifyStatus(String wire) {
        this.wire = wire;
    }

    public static Optional<VerifyStatus> fromWire(String value) {
        return Arrays.stream(values()).filter(status -> status.wire.equals(value)).findFirst();
    }

    public String wire() {
        return wire;
    }

    public String label() {
        return message("verify.status." + wire);
    }

    public String count(long amount) {
        return message("verify.count." + wire, amount);
    }
}
