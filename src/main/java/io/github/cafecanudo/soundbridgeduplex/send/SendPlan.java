package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class SendPlan {

    private SendPlan() {
    }

    public static List<SendJob> jobs(Path projectBase, List<Path> items, Predicate<Path> isDirectory,
                                     String typedDestination) {
        Map<String, List<Path>> groups = groups(projectBase, items, isDirectory);
        if (groups.size() <= 1) {
            return items.stream().map(item -> new SendJob(List.of(item), typedDestination)).toList();
        }
        return toJobs(groups, false);
    }

    public static List<SendJob> jobs(Path projectBase, SendSelection selection, Predicate<Path> isDirectory,
                                     String typedDestination) {
        if (!selection.hasDeletions()) {
            return jobs(projectBase, selection.sends(), isDirectory, typedDestination);
        }
        List<SendJob> jobs = new ArrayList<>(toJobs(groups(projectBase, selection.deletions(), item -> false), true));
        jobs.addAll(toJobs(groups(projectBase, selection.sends(), isDirectory), false));
        return jobs;
    }

    public static boolean perItemDestination(Path projectBase, SendSelection selection, Predicate<Path> isDirectory) {
        return selection.hasDeletions() || groups(projectBase, selection.sends(), isDirectory).size() > 1;
    }

    private static Map<String, List<Path>> groups(Path projectBase, List<Path> items, Predicate<Path> isDirectory) {
        Map<String, List<Path>> groups = new LinkedHashMap<>();
        for (Path item : items) {
            String destination = DestinationResolver.defaultDestination(projectBase, List.of(item), isDirectory);
            groups.computeIfAbsent(destination, key -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    private static List<SendJob> toJobs(Map<String, List<Path>> groups, boolean delete) {
        return groups.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(item -> new SendJob(List.of(item), entry.getKey(), delete)))
                .toList();
    }
}
