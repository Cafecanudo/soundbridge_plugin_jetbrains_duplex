package io.github.cafecanudo.soundbridgeduplex.receive;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiveSequenceTest {

    @Test
    void sequenceNotifiesOnlyFailuresAndTheSummaryAtTheEnd() {
        ReceiveSequence sequence = new ReceiveSequence();

        List<ReceiveSequence.Notice> first = sequence.add(file("a.txt", true, true));
        List<ReceiveSequence.Notice> second = sequence.add(file("b.txt", false, true));
        List<ReceiveSequence.Notice> last = sequence.add(file("c.txt", true, false));

        assertTrue(first.isEmpty());
        assertEquals(1, second.size());
        assertTrue(second.get(0).warning());
        assertEquals(List.of(new ReceiveSequence.Notice("2 recebido(s) · 1 falha(s) · 0:00:03", true)), last);
        assertEquals(2, sequence.received());
        assertFalse(sequence.isOpen());
    }

    @Test
    void singleFileKeepsTheOldNoticeAndTimeoutClosesTheSequence() {
        ReceiveSequence sequence = new ReceiveSequence();

        assertEquals(List.of(new ReceiveSequence.Notice("Recebido: a.txt · 0:00:01", false)),
                sequence.add(file("a.txt", true, false)));
        sequence.add(file("b.txt", true, true));
        assertTrue(sequence.isOpen());
        assertEquals(List.of(new ReceiveSequence.Notice("Recebido: b.txt · 0:00:01", false)), sequence.end());
        assertTrue(sequence.end().isEmpty());
    }

    private static ReceivedFile file(String name, boolean ok, boolean more) {
        return new ReceivedFile(name, ok ? "C:/rx/" + name : "", 10, 1.0, ok, false, List.of(), more);
    }
}
