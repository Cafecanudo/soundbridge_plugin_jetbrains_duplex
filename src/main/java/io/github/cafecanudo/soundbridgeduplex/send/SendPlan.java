package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class SendPlan {

    static final int MAX_ITEMS_CHARS = 8000;
    private static final int SEPARATOR_CHARS = 1;

    private SendPlan() {
    }

    public static List<SendJob> jobs(Path projectBase, List<Path> items, Predicate<Path> isDirectory,
                                     String typedDestination) {
        Map<String, List<Path>> groups = groups(projectBase, items, isDirectory);
        if (groups.size() <= 1) {
            return items.isEmpty() ? List.of() : chunks(items, typedDestination, false);
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

    public static boolean perItem(List<SendJob> jobs) {
        return jobs.size() > 1;
    }

    public static boolean perItemDestination(Path projectBase, SendSelection selection, Predicate<Path> isDirectory) {
        return selection.hasDeletions() || perItem(jobs(projectBase, selection.sends(), isDirectory, ""));
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
                .flatMap(entry -> chunks(entry.getValue(), entry.getKey(), delete).stream())
                .toList();
    }

    private static List<SendJob> chunks(List<Path> items, String destination, boolean delete) {
        List<SendJob> jobs = new ArrayList<>();
        List<Path> current = new ArrayList<>();
        int length = 0;
        for (Path item : items) {
            int size = item.toString().length() + SEPARATOR_CHARS;
            if (!current.isEmpty() && length + size > MAX_ITEMS_CHARS) {
                jobs.add(new SendJob(current, destination, delete));
                current = new ArrayList<>();
                length = 0;
            }
            current.add(item);
            length += size;
        }
        if (!current.isEmpty()) {
            jobs.add(new SendJob(current, destination, delete));
        }
        return jobs;
    }
}
