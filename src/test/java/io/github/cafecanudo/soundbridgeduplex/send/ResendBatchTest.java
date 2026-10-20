package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.history.Direction;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;
import io.github.cafecanudo.soundbridgeduplex.history.SendStatus;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyItem;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendBatchTest {

    private static final Path BASE = Path.of("C:/p");
    private static final SendSnapshot SNAPSHOT = new SendSnapshot(SendMode.TWO_WAY, "src", 6, 7, true);
    private static final List<VerifyItem> VERIFIED = List.of(new VerifyItem("src/a.txt", VerifyStatus.EXISTS),
            new VerifyItem("src/b.txt", VerifyStatus.DIFFERS), new VerifyItem("src/c.txt", VerifyStatus.MISSING));

    @Test
    void singleRowKeepsItsSnapshot() {
        ResendBatch batch = ResendBatch.of(List.of(sent("C:/p/x.txt")), BASE, false);

        assertEquals(List.of(Path.of("C:/p/x.txt")), batch.selection().sends());
        assertEquals(SNAPSHOT, batch.snapshot());
    }

    @Test
    void rowsAreJoinedWithoutRepeatsAndVerifiedPathsResolveOnTheBase() {
        HistoryEntry verified = verified();
        HistoryEntry deleted = new HistoryEntry(Direction.SENT, "t", List.of(), List.of("C:/p/velho.txt"), SNAPSHOT, 1,
                1, 0, null, 1.0, SendStatus.OK, List.of(), "", List.of());

        ResendBatch batch = ResendBatch.of(List.of(sent("C:/p/src/a.txt"), verified, deleted), BASE, false);

        assertEquals(List.of(Path.of("C:/p/src/a.txt"), BASE.resolve("src/b.txt"), BASE.resolve("src/c.txt")),
                batch.selection().sends());
        assertEquals(List.of(Path.of("C:/p/velho.txt")), batch.selection().deletions());
        assertNull(batch.snapshot());
    }

    @Test
    void onlyProblemsTakesMissingAndDifferentFromVerifications() {
        ResendBatch batch = ResendBatch.of(List.of(verified(), sent("C:/p/x.txt")), BASE, true);

        assertEquals(List.of(BASE.resolve("src/b.txt"), BASE.resolve("src/c.txt")), batch.selection().sends());
        assertTrue(ResendBatch.hasProblems(verified()));
        assertFalse(ResendBatch.hasProblems(sent("C:/p/x.txt")));
    }

    @Test
    void markedPathsFromTheDialogResolveOnTheBaseWithoutSnapshot() {
        ResendBatch batch = ResendBatch.ofPaths(List.of("src/b.txt", "src/c.txt", "src/b.txt"), BASE);

        assertEquals(List.of(BASE.resolve("src/b.txt"), BASE.resolve("src/c.txt")), batch.selection().sends());
        assertTrue(batch.selection().deletions().isEmpty());
        assertNull(batch.snapshot());
    }

    private static HistoryEntry sent(String item) {
        return new HistoryEntry(Direction.SENT, "t", List.of(item), List.of(), SNAPSHOT, 1, 1, 10, 7, 1.0,
                SendStatus.OK, List.of(), "", List.of());
    }

    private static HistoryEntry verified() {
        return HistoryEntry.ofVerify("t", VERIFIED.stream().map(VerifyItem::path).toList(), SNAPSHOT, SendStatus.OK,
                VERIFIED, 2.0, List.of());
    }
}
