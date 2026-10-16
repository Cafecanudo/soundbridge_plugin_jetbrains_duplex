package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReceiveCommandBuilderTest {

    private static final ProjectConfig CONFIG = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", true, "");
    private static final LinkChoice CHOICE = new LinkChoice(0.0055, true, 5000.0, 1, 14.0, List.of(), true, List.of());

    @Test
    void oneWayUsesOnlyInput() {
        assertEquals(List.of("--rx", "C:/p", "--in", "v2yh", "--keep"),
                ReceiveCommandBuilder.arguments(CONFIG, new ReceiveOptions(false, "C:/p"), Optional.empty()));
    }

    @Test
    void twoWayAddsOutputAndAckPeakOfSameDevices() {
        Optional<CalibrationProfile> profile = Optional.of(new CalibrationProfile("dq8f", "v2yh", "t", CHOICE));

        assertEquals(List.of("--rx", "C:/p", "--in", "v2yh", "--keep", "--out", "dq8f", "--ack-peak", "0.0055"),
                ReceiveCommandBuilder.arguments(CONFIG, new ReceiveOptions(true, "C:/p"), profile));
    }

    @Test
    void twoWayWithoutMatchingCalibrationHasNoAckPeak() {
        Optional<CalibrationProfile> other = Optional.of(new CalibrationProfile("ggxj", "v2yh", "t", CHOICE));

        assertEquals(List.of("--rx", "D:/rx", "--in", "v2yh", "--keep", "--out", "dq8f"),
                ReceiveCommandBuilder.arguments(CONFIG, new ReceiveOptions(true, "D:/rx"), other));
    }

    @Test
    void serveAddsFlagOnTwoWay() {
        assertEquals(List.of("--rx", "C:/p", "--in", "v2yh", "--keep", "--out", "dq8f", "--serve"),
                ReceiveCommandBuilder.arguments(CONFIG, new ReceiveOptions(true, "C:/p", true), Optional.empty()));
    }

    @Test
    void profileGoesOnlyWithTwoWay() {
        ProjectConfig withProfile = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", true, "casa");

        assertEquals(List.of("--rx", "C:/p", "--in", "v2yh", "--keep", "--out", "dq8f", "--profile", "casa"),
                ReceiveCommandBuilder.arguments(withProfile, new ReceiveOptions(true, "C:/p"), Optional.empty()));
        assertEquals(List.of("--rx", "C:/p", "--in", "v2yh", "--keep"),
                ReceiveCommandBuilder.arguments(withProfile, new ReceiveOptions(false, "C:/p"), Optional.empty()));
    }
}
