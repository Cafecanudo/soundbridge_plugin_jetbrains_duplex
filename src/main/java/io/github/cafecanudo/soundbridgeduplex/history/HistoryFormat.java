package io.github.cafecanudo.soundbridgeduplex.history;

import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendMode;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class HistoryFormat {

    private static final double KIB = 1024.0;
    private static final double MIB = KIB * KIB;
    private static final int DATE_TIME_LENGTH = 16;
    private static final String ITEM_SEPARATOR = ", ";

    private HistoryFormat() {
    }

    public static String date(String timestamp) {
        return timestamp.length() > DATE_TIME_LENGTH ? timestamp.substring(0, DATE_TIME_LENGTH) : timestamp;
    }

    public static String items(List<String> items) {
        return items.stream().map(HistoryFormat::fileName).collect(Collectors.joining(ITEM_SEPARATOR));
    }

    public static String items(List<String> items, List<String> deleted) {
        return Stream.concat(deleted.stream().map(item -> message("history.items.deleted", fileName(item))),
                        items.stream().map(HistoryFormat::fileName))
                .collect(Collectors.joining(ITEM_SEPARATOR));
    }

    private static String fileName(String item) {
        Path name = Path.of(item).getFileName();
        return name == null ? item : name.toString();
    }

    public static String mode(SendMode mode) {
        return message(mode == SendMode.TWO_WAY ? "history.mode.bi" : "history.mode.uni");
    }

    public static String size(long bytes) {
        if (bytes < KIB) {
            return bytes + " B";
        }
        if (bytes < MIB) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / KIB);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / MIB);
    }

    public static String mcs(Integer value) {
        return value == null ? "" : value.toString();
    }

    public static String duration(double seconds) {
        return SendEventReader.formatTime(seconds);
    }
}
