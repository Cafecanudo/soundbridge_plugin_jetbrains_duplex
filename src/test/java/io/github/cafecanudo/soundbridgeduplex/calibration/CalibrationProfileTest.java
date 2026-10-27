package io.github.cafecanudo.soundbridgeduplex.calibration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalibrationProfileTest {

    private static final LinkChoice CALIBRATED = new LinkChoice(0.2, false, 22000.0, 9, 41.0, List.of("clip"), true,
            List.of(new SizeResult(2048, 3, 3)));
    private static final CalibrationProfile PROFILE =
            new CalibrationProfile("dq8f", "v2yh", "2026-10-09 20:00", CALIBRATED);

    @Test
    void exposesSendValues() {
        assertEquals(new UniProfile(false, 22000.0, 0.2, 9), PROFILE.values());
        assertEquals(UniProfile.EMPTY, CalibrationProfile.values(Optional.empty()));
    }

    @Test
    void adjustedProfileBindsToCurrentDevicesAndDropsStaleResults() {
        UniProfile values = new UniProfile(true, 14000.0, 0.05, 6);

        CalibrationProfile adjusted = CalibrationProfile.adjusted(Optional.of(PROFILE), values, "ggxj", "k3m1", "t");

        assertTrue(adjusted.manual());
        assertTrue(adjusted.matches("ggxj", "k3m1"));
        assertEquals(values, adjusted.values());
        assertEquals(41.0, adjusted.choice().snrDb());
        assertFalse(adjusted.choice().validated());
        assertTrue(adjusted.choice().tests().isEmpty());
        assertTrue(adjusted.choice().warnings().isEmpty());
    }

    @Test
    void adjustedWithoutCalibrationIsUsable() {
        UniProfile values = new UniProfile(false, 7000.0, 0.003, 1);

        CalibrationProfile adjusted = CalibrationProfile.adjusted(Optional.empty(), values, "dq8f", "v2yh", "t");

        assertTrue(adjusted.choice().isUsable());
        assertNull(adjusted.choice().snrDb());
    }

    @Test
    void manualFlagSurvivesJsonAndDefaultsToFalse() {
        CalibrationProfile adjusted =
                CalibrationProfile.adjusted(Optional.of(PROFILE), PROFILE.values(), "dq8f", "v2yh", "t");

        assertEquals(Optional.of(adjusted), CalibrationProfile.fromJson(adjusted.toJson()));
        String legacy = PROFILE.toJson().replace(",\"manual\":false", "");
        assertFalse(legacy.contains("manual"));
        assertEquals(Optional.of(PROFILE), CalibrationProfile.fromJson(legacy));
    }
}
