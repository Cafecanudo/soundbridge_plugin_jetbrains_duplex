package io.github.cafecanudo.soundbridgeduplex.receive;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum ReceiveState {
    STOPPED,
    LISTENING,
    RECEIVING,
    CALIBRATING,
    PAUSED;

    public String label() {
        return switch (this) {
            case STOPPED -> message("receive.state.stopped");
            case LISTENING -> message("receive.state.listening");
            case RECEIVING -> message("receive.state.receiving");
            case CALIBRATING -> message("receive.state.calibrating");
            case PAUSED -> message("receive.state.paused");
        };
    }
}
