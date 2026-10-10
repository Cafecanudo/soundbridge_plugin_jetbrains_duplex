package io.github.cafecanudo.soundbridgeduplex.calibration;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniProfileTest {

    @Test
    void buildsSendArguments() {
        UniProfile profile = new UniProfile(true, 7000.0, 0.003, 1);

        assertTrue(profile.isComplete());
        assertEquals(List.of("--mono", "--band-high", "7000", "--peak", "0.003", "--mcs", "1"), profile.arguments());
    }

    @Test
    void incompleteProfileOmitsMissingValues() {
        UniProfile profile = new UniProfile(false, null, 0.5, null);

        assertFalse(profile.isComplete());
        assertEquals(List.of("--peak", "0.5"), profile.arguments());
    }

    @Test
    void parsesPeakWithCommaAndRejectsOutOfRange() {
        assertEquals(Optional.of(0.003), UniProfile.parsePeak(" 0,003 "));
        assertEquals(Optional.empty(), UniProfile.parsePeak(""));
        assertThrows(NumberFormatException.class, () -> UniProfile.parsePeak("1.5"));
        assertThrows(NumberFormatException.class, () -> UniProfile.parsePeak("0"));
        assertThrows(NumberFormatException.class, () -> UniProfile.parsePeak("abc"));
    }

    @Test
    void oneWayRequestUsesCalibrateUniWithOutputOnly() {
        CalibrationOptions options = new CalibrationOptions(EnumSet.of(TestSize.S5K), true, true);
        CalibrationRequest oneWay = new CalibrationRequest("python -m soundbridge", "dq8f", "v2yh", CalibrationMode.ONE_WAY, options);
        CalibrationRequest twoWay = new CalibrationRequest("python -m soundbridge", "dq8f", "v2yh", CalibrationMode.TWO_WAY, options);

        assertEquals(List.of("--calibrate-uni", "--out", "dq8f"), oneWay.arguments());
        assertEquals(List.of("--calibrate", "--out", "dq8f", "--in", "v2yh", "--calibrate-test", "5k"), twoWay.arguments());
    }
}
