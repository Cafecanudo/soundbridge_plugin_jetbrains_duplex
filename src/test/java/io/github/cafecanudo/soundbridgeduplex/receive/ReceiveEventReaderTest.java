package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEventParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiveEventReaderTest {

    @Test
    void frameMovesToReceivingAndResultBackToListening() {
        ReceiveEventReader reader = new ReceiveEventReader();

        ReceiveEventReader.Update frame = reader.read(event("{\"schema\": 1, \"event\": \"frame\", \"round\": 1, "
                + "\"blocks\": 12, \"lost\": 0, \"name\": \"src/Main.java\", \"size\": 800, \"bidirectional\": true}"));
        ReceiveEventReader.Update result = reader.read(event("{\"schema\": 1, \"event\": \"result\", \"side\": \"rx\", "
                + "\"ok\": true, \"name\": \"src/Main.java\", \"size\": 800, \"path\": \"C:/p/src/Main.java\", "
                + "\"seconds\": 12.4, \"more\": false}"));

        assertEquals(ReceiveState.RECEIVING, frame.state());
        assertEquals(ReceiveState.LISTENING, result.state());
        ReceivedFile received = result.received();
        assertEquals("src/Main.java", received.name());
        assertEquals("C:/p/src/Main.java", received.path());
        assertTrue(received.ok());
        assertTrue(received.bidirectional());
        assertEquals(List.of("recebendo src/Main.java · round 1 · 0/12 perdidos", "OK · src/Main.java · 0:00:12"),
                received.log());
    }

    @Test
    void calibrationTestIsNotAReceivedFile() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 1, \"event\": \"result\", "
                + "\"side\": \"rx\", \"ok\": true, \"test\": true}"));

        assertNull(update.received());
        assertEquals(ReceiveState.LISTENING, update.state());
    }

    @Test
    void failedResultIsRecorded() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 1, \"event\": \"result\", "
                + "\"side\": \"rx\", \"ok\": false, \"name\": \"a.bin\", \"size\": 0, \"path\": null, \"seconds\": 5}"));

        assertFalse(update.received().ok());
        assertEquals("", update.received().path());
    }

    @Test
    void oneWayCalibrationIsLoggedWithParameters() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 1, \"event\": "
                + "\"calibrate_uni\", \"mono\": true, \"band_high_hz\": 7000.0, \"peak\": 0.003, \"mcs\": 1, "
                + "\"snr_db\": 26, \"warnings\": [], \"one_way\": true}"));

        assertEquals(List.of("calibração só de ida recebida: mono · 7 kHz · pico 0.003 · MCS 1"), update.log());
    }

    private static CliEvent event(String json) {
        return CliEventParser.parse(json).orElseThrow();
    }
}
