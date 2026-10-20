package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

public record SendRequest(String launcher, List<List<String>> commands, List<SendJob> jobs, List<String> itemLabels,
                          List<String> deletedLabels, SendSnapshot snapshot, int commandDelayMs,
                          List<String> ignoredLabels, List<String> ignoredDeletedLabels) {

    public SendRequest {
        commands = commands.stream().map(List::copyOf).toList();
        jobs = List.copyOf(jobs);
        itemLabels = List.copyOf(itemLabels);
        deletedLabels = List.copyOf(deletedLabels);
        commandDelayMs = Math.max(0, commandDelayMs);
        ignoredLabels = List.copyOf(ignoredLabels);
        ignoredDeletedLabels = List.copyOf(ignoredDeletedLabels);
    }

    public SendRequest(String launcher, List<List<String>> commands, List<SendJob> jobs, List<String> itemLabels,
                       List<String> deletedLabels, SendSnapshot snapshot, int commandDelayMs) {
        this(launcher, commands, jobs, itemLabels, deletedLabels, snapshot, commandDelayMs, List.of(), List.of());
    }

    public boolean hasIgnored() {
        return !ignoredLabels.isEmpty() || !ignoredDeletedLabels.isEmpty();
    }

    public int ignoredCount() {
        return ignoredLabels.size() + ignoredDeletedLabels.size();
    }

    public SendRequest(String launcher, List<List<String>> commands, List<SendJob> jobs, List<String> itemLabels,
                       List<String> deletedLabels, SendSnapshot snapshot) {
        this(launcher, commands, jobs, itemLabels, deletedLabels, snapshot, 0);
    }
}
