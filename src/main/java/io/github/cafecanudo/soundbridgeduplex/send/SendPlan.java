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
        Map<String, List<Path>> groups = new LinkedHashMap<>();
        for (Path item : items) {
            String destination = DestinationResolver.defaultDestination(projectBase, List.of(item), isDirectory);
            groups.computeIfAbsent(destination, key -> new ArrayList<>()).add(item);
        }
        if (groups.size() <= 1) {
            return items.isEmpty() ? List.of() : List.of(new SendJob(items, typedDestination));
        }
        return groups.entrySet().stream().map(entry -> new SendJob(entry.getValue(), entry.getKey())).toList();
    }

    public static boolean perItem(List<SendJob> jobs) {
        return jobs.size() > 1;
    }
}
