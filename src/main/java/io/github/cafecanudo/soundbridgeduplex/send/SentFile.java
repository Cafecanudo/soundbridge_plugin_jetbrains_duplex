package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

public record SentFile(int command, SendFileResult result, List<String> log) {

    public SentFile {
        log = List.copyOf(log);
    }
}
