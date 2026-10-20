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

    @Test
    void oneWayCalibrationBecomesCopyableResult() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 2, \"event\": "
                + "\"calibrate_uni\", \"mono\": false, \"band_high_hz\": 22000.0, \"peak\": 0.9, \"mcs\": 8, "
                + "\"snr_db\": 35, \"warnings\": [], \"one_way\": true}"));

        ReceivedCalibration calibration = update.calibration();
        assertTrue(calibration.oneWay());
        assertTrue(calibration.canCopy());
        assertEquals("--band-high 22000 --peak 0.9 --mcs 8", calibration.txFlags());
        assertEquals(ReceiveState.LISTENING, update.state());
    }

    @Test
    void twoWayCalibrationCollectsArrivalDepartureAndProfile() {
        ReceiveEventReader reader = new ReceiveEventReader();
        String choice = "{\"peak\": 0.5, \"mono\": false, \"band_high_hz\": 22000.0, \"mcs\": 9, \"snr_db\": 40.0, "
                + "\"warnings\": [], \"validated\": true, \"tests\": [{\"size\": 2048, \"passed\": 3, \"tested\": 3}]}";

        ReceiveEventReader.Update stage = reader.read(event("{\"schema\": 2, \"event\": \"calibrate_stage\", "
                + "\"stage\": \"sondagem recebida, medindo\"}"));
        ReceiveEventReader.Update forward = reader.read(event("{\"schema\": 2, \"event\": \"calibrate_forward\", "
                + "\"choice\": " + choice + "}"));
        ReceiveEventReader.Update backward = reader.read(event("{\"schema\": 2, \"event\": \"calibrate_backward\", "
                + "\"choice\": " + choice + ", \"saved\": true, \"profile\": \"casa\", \"sent_profile\": null}"));

        assertEquals("sondagem recebida, medindo", stage.calibrationStage());
        assertEquals(ReceiveState.CALIBRATING, forward.state());
        assertNull(forward.calibration());
        ReceivedCalibration calibration = backward.calibration();
        assertFalse(calibration.oneWay());
        assertFalse(calibration.canCopy());
        assertEquals(9, calibration.arrival().mcs());
        assertEquals(1, calibration.departure().tests().size());
        assertEquals("casa", calibration.savedProfile());
        assertNull(calibration.sentProfile());
    }

    @Test
    void servedRequestIsLoggedAndIsNotAReceivedFile() {
        ReceiveEventReader reader = new ReceiveEventReader();

        ReceiveEventReader.Update serving = reader.read(event("{\"schema\": 2, \"event\": \"serve\", "
                + "\"name\": \"src/Main.java\", \"path\": \"C:/p/src/Main.java\", \"size\": 10, \"bidirectional\": true}"));
        ReceiveEventReader.Update result = reader.read(event("{\"schema\": 2, \"event\": \"result\", \"side\": \"rx\", "
                + "\"ok\": true, \"request\": true, \"served\": \"sent\", \"name\": \"src/Main.java\"}"));

        assertEquals(ReceiveState.SERVING, serving.state());
        assertNull(result.received());
        assertEquals(List.of("pedido de reenvio atendido: src/Main.java"), result.log());
    }

    @Test
    void choiceAsTextFromOldCliHasNoDetails() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 2, \"event\": "
                + "\"calibrate_backward\", \"choice\": \"LinkChoice(peak=0.5)\", \"saved\": false, \"profile\": null, "
                + "\"sent_profile\": null}"));

        assertNull(update.calibration().departure());
        assertNull(update.calibration().arrival());
    }

    @Test
    void verificationIsNotAReceivedFile() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 2, \"event\": \"result\", "
                + "\"side\": \"rx\", \"ok\": true, \"verify\": true, \"name\": \"\", \"path\": null, "
                + "\"items\": [{\"path\": \"src/a.txt\", \"status\": \"differs\"}]}"));

        assertNull(update.received());
        assertEquals(ReceiveState.LISTENING, update.state());
        assertTrue(update.verification().ok());
        assertEquals("src/a.txt", update.verification().items().get(0).path());
        assertEquals(List.of("verificação recebida · diferente"), update.log());
    }

    @Test
    void verificationWithoutItemsIsInvalid() {
        ReceiveEventReader.Update update = new ReceiveEventReader().read(event("{\"schema\": 2, \"event\": \"result\", "
                + "\"side\": \"rx\", \"ok\": false, \"verify\": true}"));

        assertFalse(update.verification().ok());
    }

    private static CliEvent event(String json) {
        return CliEventParser.parse(json).orElseThrow();
    }
}
