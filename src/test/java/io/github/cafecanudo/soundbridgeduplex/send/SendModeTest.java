package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SendModeTest {

    private static final ProjectConfig UNI = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", false, "");
    private static final ProjectConfig BI = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", true, "");
    private static final UniProfile UNI_PROFILE = new UniProfile(true, 7000.0, 0.003, 1);
    private static final LinkChoice CHOICE = new LinkChoice(0.2, false, 22000.0, 9, 41.0, List.of(), true, List.of());
    private static final Optional<CalibrationProfile> BI_PROFILE =
            Optional.of(new CalibrationProfile("dq8f", "v2yh", "2026-10-09 20:00", CHOICE));

    @Test
    void bothModesWhenBothCalibrated() {
        Set<SendMode> modes = SendMode.calibrated(BI, UNI_PROFILE, BI_PROFILE);

        assertEquals(EnumSet.allOf(SendMode.class), modes);
        assertEquals(Optional.of(SendMode.TWO_WAY), SendMode.preferred(BI, modes));
        assertEquals(Optional.of(SendMode.ONE_WAY), SendMode.preferred(UNI, modes));
    }

    @Test
    void twoWayNeedsSettingAndCalibrationOfSameDevices() {
        Optional<CalibrationProfile> other = Optional.of(new CalibrationProfile("ggxj", "v2yh", "2026-10-09 20:00", CHOICE));

        assertEquals(EnumSet.of(SendMode.ONE_WAY), SendMode.calibrated(UNI, UNI_PROFILE, BI_PROFILE));
        assertEquals(EnumSet.of(SendMode.ONE_WAY), SendMode.calibrated(BI, UNI_PROFILE, other));
    }

    @Test
    void fallsBackToOtherCalibratedModeOrNone() {
        Set<SendMode> onlyBi = SendMode.calibrated(BI, UniProfile.EMPTY, BI_PROFILE);

        assertEquals(Optional.of(SendMode.TWO_WAY), SendMode.preferred(UNI, onlyBi));
        assertEquals(Optional.empty(), SendMode.preferred(BI, SendMode.calibrated(BI, UniProfile.EMPTY, Optional.empty())));
    }
}
