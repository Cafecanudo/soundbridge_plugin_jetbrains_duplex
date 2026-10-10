package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEventParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SendEventReaderTest {

    @Test
    void tracksFileAndCounterFromInfo() {
        SendEventReader reader = new SendEventReader();
        reader.read(event("{\"schema\": 1, \"event\": \"info\", \"message\": \"[2/3] enviando a.txt (1 KB)\", \"name\": \"a.txt\"}"));

        SendProgress progress = reader.read(event("{\"schema\": 1, \"event\": \"progress\", \"name\": \"a.txt\", "
                + "\"status\": \"MCS 1\", \"fraction\": 0.5, \"kib_per_s\": 1.2, \"eta_s\": null}")).progress();

        assertEquals(new SendProgress("a.txt", "2/3", 0.5, 1.2, null, "MCS 1"), progress);
    }

    @Test
    void fileResultUsesCurrentFileAndLastMcs() {
        SendEventReader reader = new SendEventReader();
        reader.read(event("{\"schema\": 1, \"event\": \"info\", \"message\": \"enviando b.bin\", \"name\": \"b.bin\"}"));

        SendEventReader.Update update = reader.read(event("{\"schema\": 1, \"event\": \"result\", \"side\": \"tx\", "
                + "\"ok\": true, \"seconds\": 84.4, \"size\": 2048, \"rounds\": 2, \"acks_lost\": 0, \"mcs_trace\": [7, 6]}"));

        assertEquals(new SendFileResult("b.bin", true, 84.4, 2048, 6), update.result());
        assertEquals(new SendProgress("b.bin", "", 1.0, null, null, "concluído"), update.progress());
        assertEquals(List.of("OK · b.bin · 0:01:24"), update.log());
    }

    @Test
    void summaryIsLoggedWithoutFileResult() {
        SendEventReader.Update update = new SendEventReader().read(event("{\"schema\": 1, \"event\": \"result\", "
                + "\"side\": \"tx\", \"files\": 3, \"ok\": 2, \"seconds\": 61.0}"));

        assertNull(update.result());
        assertEquals(List.of("2/3 arquivos enviados · 0:01:01"), update.log());
    }

    @Test
    void formatsMinutesAndSeconds() {
        assertEquals("0:00:05", SendEventReader.formatTime(4.6));
        assertEquals("0:15:00", SendEventReader.formatTime(900));
        assertEquals("1:02:03", SendEventReader.formatTime(3723));
    }

    private static CliEvent event(String json) {
        return CliEventParser.parse(json).orElseThrow();
    }
}
