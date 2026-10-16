package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

public record SendRequest(String launcher, List<List<String>> commands, List<SendJob> jobs, List<String> itemLabels,
                          List<String> deletedLabels, SendSnapshot snapshot, int commandDelayMs) {

    public SendRequest {
        commands = commands.stream().map(List::copyOf).toList();
        jobs = List.copyOf(jobs);
        itemLabels = List.copyOf(itemLabels);
        deletedLabels = List.copyOf(deletedLabels);
        commandDelayMs = Math.max(0, commandDelayMs);
    }

    public SendRequest(String launcher, List<List<String>> commands, List<SendJob> jobs, List<String> itemLabels,
                       List<String> deletedLabels, SendSnapshot snapshot) {
        this(launcher, commands, jobs, itemLabels, deletedLabels, snapshot, 0);
    }
}
