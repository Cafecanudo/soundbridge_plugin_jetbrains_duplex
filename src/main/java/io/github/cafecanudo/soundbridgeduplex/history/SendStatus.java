package io.github.cafecanudo.soundbridgeduplex.history;

import io.github.cafecanudo.soundbridgeduplex.send.SendOutcome;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum SendStatus {
    OK,
    FAILED,
    CANCELLED;

    public static SendStatus of(SendOutcome outcome) {
        if (outcome.cancelled()) {
            return CANCELLED;
        }
        return outcome.succeeded() ? OK : FAILED;
    }

    public String label() {
        return switch (this) {
            case OK -> message("history.status.ok");
            case FAILED -> message("history.status.failed");
            case CANCELLED -> message("history.status.cancelled");
        };
    }
}
