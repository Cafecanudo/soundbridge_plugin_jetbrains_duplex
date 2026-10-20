package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.icons.AllIcons;

import javax.swing.Icon;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum Direction {
    SENT,
    RECEIVED,
    VERIFY_SENT,
    VERIFY_RECEIVED;

    public String label() {
        return switch (this) {
            case SENT -> message("history.direction.sent");
            case RECEIVED -> message("history.direction.received");
            case VERIFY_SENT -> message("history.direction.verifySent");
            case VERIFY_RECEIVED -> message("history.direction.verifyReceived");
        };
    }

    public Icon icon() {
        return switch (this) {
            case SENT -> AllIcons.Actions.Upload;
            case RECEIVED -> AllIcons.Actions.Download;
            case VERIFY_SENT, VERIFY_RECEIVED -> AllIcons.Actions.Help;
        };
    }

    public boolean isVerify() {
        return this == VERIFY_SENT || this == VERIFY_RECEIVED;
    }
}
