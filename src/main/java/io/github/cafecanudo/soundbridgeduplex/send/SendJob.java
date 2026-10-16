package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;
import java.util.List;

public record SendJob(List<Path> items, String destination, boolean delete) {

    public SendJob {
        items = List.copyOf(items);
    }

    public SendJob(List<Path> items, String destination) {
        this(items, destination, false);
    }
}
