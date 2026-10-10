package io.github.cafecanudo.soundbridgeduplex.receive;

import java.util.List;

public record ReceivedFile(String name, String path, long size, double seconds, boolean ok, boolean bidirectional,
                           List<String> log) {

    public ReceivedFile {
        log = List.copyOf(log);
    }
}
