package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;
import java.util.List;

public record SendJob(List<Path> items, String destination) {

    public SendJob {
        items = List.copyOf(items);
    }
}
