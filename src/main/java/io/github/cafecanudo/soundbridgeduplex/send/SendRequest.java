package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

public record SendRequest(String launcher, List<List<String>> commands, List<String> itemLabels,
                          List<String> deletedLabels, SendSnapshot snapshot) {

    public SendRequest {
        commands = commands.stream().map(List::copyOf).toList();
        itemLabels = List.copyOf(itemLabels);
        deletedLabels = List.copyOf(deletedLabels);
    }
}
