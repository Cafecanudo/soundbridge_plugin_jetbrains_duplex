package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

record SendItem(Path path, boolean delete) {

    @Override
    public String toString() {
        return delete ? message("send.items.delete", path.toString()) : path.toString();
    }
}
