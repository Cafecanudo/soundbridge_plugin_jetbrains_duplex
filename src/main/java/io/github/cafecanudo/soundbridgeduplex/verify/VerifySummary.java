package io.github.cafecanudo.soundbridgeduplex.verify;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class VerifySummary {

    private static final String PART_SEPARATOR = " · ";

    private VerifySummary() {
    }

    public static long count(List<VerifyItem> items, VerifyStatus status) {
        return items.stream().filter(item -> item.status() == status).count();
    }

    public static boolean allExist(List<VerifyItem> items) {
        return !items.isEmpty() && count(items, VerifyStatus.EXISTS) == items.size();
    }

    public static String text(List<VerifyItem> items) {
        if (items.size() == 1) {
            return items.get(0).status().label();
        }
        if (allExist(items)) {
            return message("verify.summary.allExist", items.size());
        }
        return Arrays.stream(VerifyStatus.values())
                .filter(status -> count(items, status) > 0)
                .map(status -> status.count(count(items, status)))
                .collect(Collectors.joining(PART_SEPARATOR));
    }

    public static List<VerifyItem> problemsFirst(List<VerifyItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(VerifyItem::status).thenComparing(VerifyItem::path))
                .toList();
    }
}
