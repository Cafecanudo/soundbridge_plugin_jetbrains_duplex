package io.github.cafecanudo.soundbridgeduplex.history;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum Direction {
    SENT,
    RECEIVED;

    public String label() {
        return message(this == SENT ? "history.direction.sent" : "history.direction.received");
    }
}
