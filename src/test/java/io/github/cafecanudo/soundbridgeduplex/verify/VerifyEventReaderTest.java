package io.github.cafecanudo.soundbridgeduplex.verify;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEventParser;
import io.github.cafecanudo.soundbridgeduplex.history.SendStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyEventReaderTest {

    private static final String ANSWERED = "{\"schema\": 2, \"event\": \"result\", \"side\": \"tx\", \"verify\": true, "
            + "\"ok\": true, \"status\": \"answered\", \"files\": 3, \"items\": ["
            + "{\"path\": \"src/a.txt\", \"status\": \"exists\"}, {\"path\": \"src/b.txt\", \"status\": \"differs\"}, "
            + "{\"path\": \"src/c.txt\", \"status\": \"missing\"}]}";

    @Test
    void answeredResultCarriesEveryItem() {
        VerifyEventReader.Update update = new VerifyEventReader().read(event(ANSWERED));

        assertEquals(VerifyEventReader.ANSWERED, update.batch().status());
        assertEquals(List.of(new VerifyItem("src/a.txt", VerifyStatus.EXISTS),
                new VerifyItem("src/b.txt", VerifyStatus.DIFFERS),
                new VerifyItem("src/c.txt", VerifyStatus.MISSING)), update.batch().items());
        assertEquals(List.of("verificação · 1 diferente(s) · 1 não existe(m) · 1 existe(m)"), update.log());
    }

    @Test
    void startEventLogsTheWait() {
        VerifyEventReader.Update update = new VerifyEventReader().read(event("{\"schema\": 2, \"event\": \"verify\", "
                + "\"side\": \"tx\", \"status\": \"requesting\", \"files\": 2, \"paths\": [\"a\", \"b\"]}"));

        assertNull(update.batch());
        assertEquals(List.of("verificando 2 arquivo(s) no destino · aguardando a resposta"), update.log());
    }

    @Test
    void sendResultIsIgnoredAndUnknownStatusIsAFailure() {
        VerifyEventReader reader = new VerifyEventReader();

        assertNull(reader.read(event("{\"schema\": 2, \"event\": \"result\", \"side\": \"tx\", \"ok\": true}")).batch());
        assertEquals(VerifyEventReader.FAILED, reader.read(event("{\"schema\": 2, \"event\": \"result\", "
                + "\"side\": \"tx\", \"verify\": true, \"status\": \"outro\"}")).batch().status());
    }

    @Test
    void outcomeJoinsBatchesAndFollowsTheirStatus() {
        VerifyEventReader.Batch first = new VerifyEventReader().read(event(ANSWERED)).batch();
        VerifyEventReader.Batch requested = new VerifyEventReader.Batch(VerifyEventReader.REQUESTED, true, List.of());
        VerifyEventReader.Batch noReply = new VerifyEventReader.Batch(VerifyEventReader.NO_REPLY, false, List.of());

        VerifyOutcome answered = VerifyOutcome.of(List.of(first, first), 2, 6, 1.0, false, null);

        assertEquals(SendStatus.OK, answered.status());
        assertEquals(6, answered.items().size());
        assertEquals(SendStatus.REQUESTED, VerifyOutcome.of(List.of(requested), 1, 3, 1.0, false, null).status());
        assertEquals(SendStatus.NO_REPLY, VerifyOutcome.of(List.of(noReply), 1, 3, 1.0, false, "código 1").status());
        assertEquals(SendStatus.FAILED, VerifyOutcome.of(List.of(first), 2, 6, 1.0, false, "código 1").status());
        assertEquals(SendStatus.CANCELLED, VerifyOutcome.of(List.of(), 1, 3, 1.0, true, null).status());
    }

    @Test
    void summaryNamesTheSingleStatusOrCountsEach() {
        VerifyItem same = new VerifyItem("a", VerifyStatus.EXISTS);

        assertEquals("existe", VerifySummary.text(List.of(same)));
        assertEquals("todos existem (2)", VerifySummary.text(List.of(same, same)));
        assertTrue(VerifySummary.allExist(List.of(same)));
        assertEquals(List.of("b", "a"), VerifySummary.problemsFirst(List.of(same,
                new VerifyItem("b", VerifyStatus.MISSING))).stream().map(VerifyItem::path).toList());
    }

    private static CliEvent event(String line) {
        return CliEventParser.parse(line).orElseThrow();
    }
}
