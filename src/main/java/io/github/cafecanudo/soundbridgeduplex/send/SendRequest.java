package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

public record SendRequest(String launcher, List<List<String>> commands, List<String> itemLabels) {

    public SendRequest {
        commands = commands.stream().map(List::copyOf).toList();
        itemLabels = List.copyOf(itemLabels);
    }
}
