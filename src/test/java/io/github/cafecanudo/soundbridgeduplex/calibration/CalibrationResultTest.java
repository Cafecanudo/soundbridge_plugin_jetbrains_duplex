package io.github.cafecanudo.soundbridgeduplex.calibration;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEventParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalibrationResultTest {

    private static final String RESULT = "{\"schema\": 1, \"event\": \"result\", \"side\": \"calibrate\", "
            + "\"forward\": {\"peak\": 0.0055, \"mono\": true, \"band_high_hz\": 5000.0, \"mcs\": 1, \"snr_db\": 14.2, "
            + "\"warnings\": [\"AGC na entrada\"], \"validated\": true, "
            + "\"tests\": [{\"size\": 5120, \"passed\": 10, \"tested\": 10}]}, "
            + "\"backward\": {\"peak\": 0.2, \"mono\": false, \"band_high_hz\": 22000.0, \"mcs\": 9, \"snr_db\": 41.0, "
            + "\"warnings\": [], \"validated\": false, \"tests\": []}, "
            + "\"test_sizes\": [\"5k\"], \"saved\": true, \"notes\": []}";

    @Test
    void parsesBothDirections() {
        CalibrationResult result = CalibrationResult.from(event(RESULT)).orElseThrow();

        assertTrue(result.isUsable());
        assertTrue(result.saved());
        assertEquals(1, result.forward().mcs());
        assertTrue(result.forward().mono());
        assertEquals(List.of(new SizeResult(5120, 10, 10)), result.forward().tests());
        assertEquals(9, result.backward().mcs());
    }

    @Test
    void summarizesChoice() {
        CalibrationResult result = CalibrationResult.from(event(RESULT)).orElseThrow();

        assertEquals("mono · 5 kHz · MCS 1 · pico 0.0055 · SNR 14 dB · 5k 10/10", result.forward().summary());
        assertEquals("estéreo · 22 kHz · MCS 9 · pico 0.2 · SNR 41 dB", result.backward().summary());
    }

    @Test
    void acceptsMissingBackward() {
        String json = "{\"schema\": 1, \"event\": \"result\", \"side\": \"calibrate\", \"forward\": null, "
                + "\"backward\": null, \"saved\": false, \"notes\": [\"sem resposta\"]}";

        CalibrationResult result = CalibrationResult.from(event(json)).orElseThrow();

        assertFalse(result.isUsable());
        assertNull(result.backward());
        assertEquals(List.of("sem resposta"), result.notes());
    }

    @Test
    void ignoresTransferResult() {
        assertTrue(CalibrationResult.from(event("{\"schema\": 1, \"event\": \"result\", \"side\": \"tx\"}")).isEmpty());
    }

    @Test
    void profileRoundTripsThroughJson() {
        LinkChoice choice = CalibrationResult.from(event(RESULT)).orElseThrow().forward();
        CalibrationProfile profile = new CalibrationProfile("dq8f", "v2yh", "2026-10-09 20:00", choice);

        CalibrationProfile restored = CalibrationProfile.fromJson(profile.toJson()).orElseThrow();

        assertEquals(profile, restored);
        assertTrue(restored.matches("dq8f", "v2yh"));
        assertFalse(restored.matches("ggxj", "v2yh"));
    }

    @Test
    void invalidProfileJsonIsEmpty() {
        assertTrue(CalibrationProfile.fromJson("").isEmpty());
        assertTrue(CalibrationProfile.fromJson("{\"output\": 1}").isEmpty());
    }

    private static CliEvent event(String json) {
        return CliEventParser.parse(json).orElseThrow();
    }
}
