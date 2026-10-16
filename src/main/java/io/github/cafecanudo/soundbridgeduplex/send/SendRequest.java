package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

public record SendRequest(String launcher, List<List<String>> commands, List<SendJob> jobs, List<String> itemLabels,
                          List<String> deletedLabels, SendSnapshot snapshot) {

    public SendRequest {
        commands = commands.stream().map(List::copyOf).toList();
        jobs = List.copyOf(jobs);
        itemLabels = List.copyOf(itemLabels);
        deletedLabels = List.copyOf(deletedLabels);
    }
}
