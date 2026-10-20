package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.history.Direction;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryLog;
import io.github.cafecanudo.soundbridgeduplex.history.SendStatus;
import io.github.cafecanudo.soundbridgeduplex.send.SendMode;
import io.github.cafecanudo.soundbridgeduplex.send.SendSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResendRequestTest {

    private static final String BASE = Path.of("C:/rx").toAbsolutePath().toString();

    @Test
    void remoteTargetSplitsDestinationAndFile() {
        assertEquals(new RemoteTarget("src/app", "Main.java"), RemoteTarget.of("src/app/Main.java"));
        assertEquals(new RemoteTarget("", "a.txt"), RemoteTarget.of("a.txt"));
        assertEquals(new RemoteTarget("src", "b.txt"), RemoteTarget.of("src\\b.txt"));
    }

    @Test
    void remoteNameComesFromEntryOrFromReceivePath() {
        HistoryEntry withName = received(Path.of(BASE, "src", "Main.java").toString(), "src/Main.java");
        HistoryEntry legacy = received(Path.of(BASE, "src", "Main.java").toString(), "");
        HistoryEntry failed = received("src/Main.java", "");
        HistoryEntry outside = received(Path.of("D:/outro/a.txt").toAbsolutePath().toString(), "");

        assertEquals("src/Main.java", ResendRequestService.remoteName(withName, BASE));
        assertEquals("src/Main.java", ResendRequestService.remoteName(legacy, BASE));
        assertEquals("src/Main.java", ResendRequestService.remoteName(failed, BASE));
        assertEquals("", ResendRequestService.remoteName(outside, BASE));
    }

    @Test
    void replaceKeepsThePositionAndAddsWhenMissing() {
        HistoryEntry first = received("a.txt", "a.txt");
        HistoryEntry second = received("b.txt", "b.txt");
        HistoryEntry answer = received(Path.of(BASE, "a.txt").toString(), "a.txt");
        List<String> stored = HistoryLog.add(HistoryLog.add(List.of(), first), second);

        List<String> replaced = HistoryLog.replace(stored, first, answer);
        List<String> added = HistoryLog.replace(stored, answer, second);

        assertEquals(List.of(second, answer), HistoryLog.read(replaced));
        assertEquals(3, HistoryLog.read(added).size());
    }

    private static HistoryEntry received(String item, String remoteName) {
        SendSnapshot snapshot = new SendSnapshot(SendMode.TWO_WAY, "", null, null, false);
        return new HistoryEntry(Direction.RECEIVED, "t", List.of(item),
                List.of(), snapshot, 1, 0, 0, null, 1.0, SendStatus.FAILED,
                List.of(), remoteName, List.of());
    }
}
