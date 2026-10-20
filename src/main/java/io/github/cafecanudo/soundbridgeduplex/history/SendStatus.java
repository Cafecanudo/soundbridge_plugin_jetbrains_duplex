package io.github.cafecanudo.soundbridgeduplex.history;

import io.github.cafecanudo.soundbridgeduplex.send.SendOutcome;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum SendStatus {
    OK,
    FAILED,
    CANCELLED,
    REQUESTING,
    REQUESTED,
    NO_REPLY,
    IGNORED;

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
            case REQUESTING -> message("history.status.requesting");
            case REQUESTED -> message("history.status.requested");
            case NO_REPLY -> message("history.status.noReply");
            case IGNORED -> message("history.status.ignored");
        };
    }
}
