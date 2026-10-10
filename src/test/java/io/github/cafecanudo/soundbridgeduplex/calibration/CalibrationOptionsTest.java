package io.github.cafecanudo.soundbridgeduplex.calibration;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEventParser;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalibrationOptionsTest {

    @Test
    void buildsFullCommandWithoutSavingInSoundBridge() {
        CalibrationOptions options = new CalibrationOptions(EnumSet.of(TestSize.S1MB, TestSize.S5K), true, true);

        assertEquals(List.of("--calibrate", "--out", "dq8f", "--in", "v2yh", "--calibrate-test", "5k,1mb"),
                options.arguments("dq8f", "v2yh"));
    }

    @Test
    void omitsEmptyDevicesAndDisabledOptions() {
        CalibrationOptions options = new CalibrationOptions(EnumSet.of(TestSize.S5K), false, false);

        assertEquals(List.of("--calibrate"), options.arguments("", " "));
    }

    @Test
    void decodesSizesIgnoringUnknown() {
        assertEquals(EnumSet.of(TestSize.S2K, TestSize.S10MB), CalibrationOptions.decodeSizes("10MB, 2k,xx"));
        assertEquals(EnumSet.noneOf(TestSize.class), CalibrationOptions.decodeSizes(null));
    }

    @Test
    void formatsTestAndAttemptLines() {
        var test = CliEventParser.parse("{\"schema\": 1, \"event\": \"calibrate_test\", \"index\": 2, \"count\": 10, "
                + "\"size\": 5120, \"ok\": true, \"rounds\": 1, \"seconds\": 3.46}").orElseThrow();
        var attempt = CliEventParser.parse("{\"schema\": 1, \"event\": \"calibrate_attempt\", \"attempt\": 1, "
                + "\"passed\": 7, \"count\": 10, \"size\": 204800, \"mcs\": 8}").orElseThrow();

        assertEquals(List.of("  teste 5k 2/10 · OK · 1 rounds · 3.5 s"), CalibrationLog.lines(test));
        assertEquals(List.of("tentativa 1: 200k 7/10 aprovados · MCS 8"), CalibrationLog.lines(attempt));
    }
}
