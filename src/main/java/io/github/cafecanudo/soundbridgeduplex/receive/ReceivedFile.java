package io.github.cafecanudo.soundbridgeduplex.receive;

import java.util.List;

public record ReceivedFile(String name, String path, long size, double seconds, boolean ok, boolean bidirectional,
                           List<String> log, boolean more) {

    public ReceivedFile {
        log = List.copyOf(log);
    }

    public ReceivedFile(String name, String path, long size, double seconds, boolean ok, boolean bidirectional,
                        List<String> log) {
        this(name, path, size, seconds, ok, bidirectional, log, false);
    }
}
