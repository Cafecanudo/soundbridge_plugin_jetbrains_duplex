package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.icons.AllIcons;

import javax.swing.Icon;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public enum Direction {
    SENT,
    RECEIVED;

    public String label() {
        return message(this == SENT ? "history.direction.sent" : "history.direction.received");
    }

    public Icon icon() {
        return this == SENT ? AllIcons.Actions.Upload : AllIcons.Actions.Download;
    }
}
