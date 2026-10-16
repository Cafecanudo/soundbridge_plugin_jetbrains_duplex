package io.github.cafecanudo.soundbridgeduplex.history;

import io.github.cafecanudo.soundbridgeduplex.send.SendFileResult;
import io.github.cafecanudo.soundbridgeduplex.send.SendJob;
import io.github.cafecanudo.soundbridgeduplex.send.SendMode;
import io.github.cafecanudo.soundbridgeduplex.send.SendOutcome;
import io.github.cafecanudo.soundbridgeduplex.send.SendRequest;
import io.github.cafecanudo.soundbridgeduplex.send.SendSnapshot;
import io.github.cafecanudo.soundbridgeduplex.send.SentFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SentEntriesTest {

    private static final SendSnapshot SNAPSHOT = new SendSnapshot(SendMode.TWO_WAY, "", 9, null, false);
    private static final Path OLD = Path.of("C:/p/src/old.java");
    private static final Path A = Path.of("C:/p/src/a.java");
    private static final Path B = Path.of("C:/p/src/b.java");
    private static final Path C = Path.of("C:/p/lib/c.java");

    @Test
    void everyFileIsItsOwnEntryWithItsDestinationAndLog() {
        SendRequest request = request();
        List<SentFile> files = List.of(
                new SentFile(0, new SendFileResult("old.java", true, 1.0, 0, null), List.of("apagando")),
                new SentFile(1, new SendFileResult("a.java", true, 2.0, 100, 7), List.of("a ok")),
                new SentFile(1, new SendFileResult("b.java", false, 3.0, 200, 6), List.of("b falhou")));

        List<HistoryEntry> entries = SentEntries.of("t", request, files, new SendOutcome(List.of(), 9.0, false, "saída 1"),
                List.of("log inteiro"));

        assertEquals(4, entries.size());
        assertEquals(List.of(OLD.toString()), entries.get(0).deleted());
        assertEquals(List.of(A.toString()), entries.get(1).items());
        assertEquals("src", entries.get(1).snapshot().destination());
        assertEquals(List.of("a ok"), entries.get(1).log());
        assertEquals(SendStatus.OK, entries.get(1).status());
        assertEquals(SendStatus.FAILED, entries.get(2).status());
        assertEquals(200, entries.get(2).size());
        assertEquals(List.of(C.toString()), entries.get(3).items());
        assertEquals("lib", entries.get(3).snapshot().destination());
        assertEquals(SendStatus.FAILED, entries.get(3).status());
        assertEquals(List.of("saída 1"), entries.get(3).log());
    }

    @Test
    void filesNotReachedAfterCancelAreCancelled() {
        List<HistoryEntry> entries = SentEntries.of("t", request(), List.of(),
                new SendOutcome(List.of(), 1.0, true, null), List.of());

        assertEquals(SendStatus.CANCELLED, entries.get(1).status());
    }

    private static SendRequest request() {
        List<SendJob> jobs = List.of(new SendJob(List.of(OLD), "src", true), new SendJob(List.of(A, B), "src"),
                new SendJob(List.of(C), "lib"));
        return new SendRequest("python -m soundbridge", List.of(List.of(), List.of(), List.of()), jobs, List.of(),
                List.of(), SNAPSHOT);
    }

    @Test
    void folderSentWholeIsOneEntryWithItsFilesSummed() {
        Path folder = Path.of("C:/p/docs");
        SendRequest request = new SendRequest("python -m soundbridge", List.of(List.of()),
                List.of(new SendJob(List.of(folder), "docs")), List.of(), List.of(), SNAPSHOT);
        List<SentFile> files = List.of(
                new SentFile(0, new SendFileResult("a.txt", true, 1.0, 100, 7), List.of("a")),
                new SentFile(0, new SendFileResult("b.txt", true, 2.0, 200, 6), List.of("b")));

        List<HistoryEntry> entries = SentEntries.of("t", request, files, new SendOutcome(List.of(), 3.0, false, null),
                List.of(), folder::equals);

        assertEquals(1, entries.size());
        assertEquals(List.of(folder.toString()), entries.get(0).items());
        assertEquals(2, entries.get(0).files());
        assertEquals(300, entries.get(0).size());
        assertEquals(SendStatus.OK, entries.get(0).status());
        assertEquals("docs", entries.get(0).snapshot().destination());
    }
}
