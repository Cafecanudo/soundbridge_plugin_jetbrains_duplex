package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

record SendItem(Path path, boolean delete, String destination) {

    SendItem(Path path, boolean delete) {
        this(path, delete, null);
    }

    @Override
    public String toString() {
        String label = delete ? message("send.items.delete", path.toString()) : path.toString();
        if (destination == null) {
            return label;
        }
        return message("send.items.destination", label,
                destination.isEmpty() ? message("send.items.destination.root") : destination);
    }
}
