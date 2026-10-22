package io.github.cafecanudo.soundbridgeduplex.receive;

import java.util.List;

public record ReceivedFile(String name, String path, long size, double seconds, boolean ok, boolean bidirectional,
                           List<String> log, boolean more, String memory) {

    public ReceivedFile {
        log = List.copyOf(log);
        memory = memory == null ? "" : memory;
    }

    public ReceivedFile(String name, String path, long size, double seconds, boolean ok, boolean bidirectional,
                        List<String> log, boolean more) {
        this(name, path, size, seconds, ok, bidirectional, log, more, "");
    }

    public ReceivedFile(String name, String path, long size, double seconds, boolean ok, boolean bidirectional,
                        List<String> log) {
        this(name, path, size, seconds, ok, bidirectional, log, false, "");
    }

    public boolean inMemory() {
        return !memory.isEmpty();
    }
}
