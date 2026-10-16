package io.github.cafecanudo.soundbridgeduplex.history;

import io.github.cafecanudo.soundbridgeduplex.receive.ReceivedFile;
import io.github.cafecanudo.soundbridgeduplex.send.SendFileResult;
import io.github.cafecanudo.soundbridgeduplex.send.SendMode;
import io.github.cafecanudo.soundbridgeduplex.send.SendOutcome;
import io.github.cafecanudo.soundbridgeduplex.send.SendSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryTest {

    private static final SendSnapshot SNAPSHOT = new SendSnapshot(SendMode.TWO_WAY, "src/sss", 6, null, true);
    private static final List<SendFileResult> FILES = List.of(
            new SendFileResult("a.txt", true, 10.0, 2048, 7),
            new SendFileResult("b.txt", false, 20.0, 1024, 6));

    @Test
    void entryFromOutcomeSumsFilesAndKeepsLastMcs() {
        HistoryEntry entry = entry("2026-10-10 14:32:05", new SendOutcome(FILES, 31.0, false, null));

        assertEquals(2, entry.files());
        assertEquals(1, entry.okFiles());
        assertEquals(3072, entry.size());
        assertEquals(6, entry.finalMcs());
        assertEquals(SendStatus.FAILED, entry.status());
    }

    @Test
    void statusReflectsCancelAndSuccess() {
        assertEquals(SendStatus.CANCELLED, SendStatus.of(new SendOutcome(FILES, 1.0, true, null)));
        assertEquals(SendStatus.OK, SendStatus.of(new SendOutcome(List.of(FILES.get(0)), 1.0, false, null)));
    }

    @Test
    void entryRoundTripsThroughJson() {
        HistoryEntry entry = entry("2026-10-10 14:32:05", new SendOutcome(FILES, 31.0, false, null));

        assertEquals(entry, HistoryEntry.fromJson(entry.toJson()).orElseThrow());
        assertTrue(HistoryEntry.fromJson("{}").isEmpty());
    }

    @Test
    void logKeepsOnlyLastLines() {
        List<String> log = IntStream.range(0, HistoryEntry.MAX_LOG_LINES + 50).mapToObj(i -> "linha " + i).toList();
        HistoryEntry entry = HistoryEntry.of("t", List.of("a"), SNAPSHOT, new SendOutcome(FILES, 1.0, false, null), log);

        assertEquals(HistoryEntry.MAX_LOG_LINES, entry.log().size());
        assertEquals("linha 249", entry.log().get(entry.log().size() - 1));
    }

    @Test
    void historyIsNewestFirstAndCapped() {
        List<String> stored = new ArrayList<>();
        for (int i = 0; i < HistoryLog.MAX_ENTRIES + 5; i++) {
            stored = HistoryLog.add(stored, entry("2026-10-10 00:00:" + i, new SendOutcome(FILES, 1.0, false, null)));
        }

        List<HistoryEntry> entries = HistoryLog.read(stored);

        assertEquals(HistoryLog.MAX_ENTRIES, entries.size());
        assertEquals("2026-10-10 00:00:204", entries.get(0).timestamp());
    }

    @Test
    void receivedEntryRoundTripsAndOldEntriesAreSent() {
        ReceivedFile file = new ReceivedFile("src/Main.java", "C:/p/src/Main.java", 800, 12.4, true, false,
                List.of("OK"));
        HistoryEntry received = HistoryEntry.ofReceived("2026-10-10 15:00:00", file);

        assertEquals(Direction.RECEIVED, received.direction());
        assertEquals(List.of("C:/p/src/Main.java"), received.items());
        assertEquals(received, HistoryEntry.fromJson(received.toJson()).orElseThrow());
        assertEquals(file.name(), received.remoteName());
        String withoutName = received.toJson().replace(",\"remote_name\":\"" + file.name() + "\"", "");
        assertEquals("", HistoryEntry.fromJson(withoutName).orElseThrow().remoteName());
        String legacy = entry("t", new SendOutcome(FILES, 1.0, false, null)).toJson().replace("\"direction\":\"SENT\",", "");
        assertEquals(Direction.SENT, HistoryEntry.fromJson(legacy).orElseThrow().direction());
    }

    @Test
    void deletedItemsRoundTripAndLegacyEntriesHaveNone() {
        HistoryEntry entry = HistoryEntry.of("t", List.of("C:/p/a.txt"), List.of("C:/p/old.java"), SNAPSHOT,
                new SendOutcome(FILES, 1.0, false, null), List.of());

        assertEquals(entry, HistoryEntry.fromJson(entry.toJson()).orElseThrow());
        String legacy = entry.toJson().replace("\"deleted\":[\"C:/p/old.java\"],", "");
        assertEquals(List.of(), HistoryEntry.fromJson(legacy).orElseThrow().deleted());
    }

    @Test
    void formatsColumns() {
        assertEquals("2026-10-10 14:32", HistoryFormat.date("2026-10-10 14:32:05"));
        assertEquals("a.txt, b.txt", HistoryFormat.items(List.of("C:/p/a.txt", "C:/p/b.txt")));
        assertEquals("YTr.java", HistoryFormat.items(List.of("C:/p/YTr.java")));
        assertEquals("✕ old.java, a.txt", HistoryFormat.items(List.of("C:/p/a.txt"), List.of("C:/p/old.java")));
        assertEquals("512 B", HistoryFormat.size(512));
        assertEquals("2.0 KB", HistoryFormat.size(2048));
        assertEquals("1.5 MB", HistoryFormat.size(1572864));
        assertEquals("0:01:24", HistoryFormat.duration(84.4));
    }

    private static HistoryEntry entry(String timestamp, SendOutcome outcome) {
        return HistoryEntry.of(timestamp, List.of("C:/p/a.txt", "C:/p/b.txt"), SNAPSHOT, outcome, List.of("log 1", "log 2"));
    }
}
