package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum SendMode {
    ONE_WAY,
    TWO_WAY;

    public static Set<SendMode> calibrated(ProjectConfig config, UniProfile uniProfile,
                                           Optional<CalibrationProfile> biProfile) {
        Set<SendMode> modes = EnumSet.noneOf(SendMode.class);
        if (uniProfile.isComplete()) {
            modes.add(ONE_WAY);
        }
        boolean biCalibrated = biProfile
                .filter(profile -> profile.matches(config.output(), config.input()))
                .filter(profile -> profile.choice().isUsable())
                .isPresent();
        if (config.bidirectional() && biCalibrated) {
            modes.add(TWO_WAY);
        }
        return modes;
    }

    public static Optional<SendMode> preferred(ProjectConfig config, Set<SendMode> calibrated,
                                               Optional<SendMode> chosen) {
        return chosen.filter(calibrated::contains).or(() -> preferred(config, calibrated));
    }

    public static Optional<SendMode> preferred(ProjectConfig config, Set<SendMode> calibrated) {
        SendMode configured = config.bidirectional() ? TWO_WAY : ONE_WAY;
        if (calibrated.contains(configured)) {
            return Optional.of(configured);
        }
        return calibrated.stream().findFirst();
    }

    public ProjectConfig applyTo(ProjectConfig config) {
        return new ProjectConfig(config.launcher(), config.output(), config.input(), config.receiveDirectory(),
                this == TWO_WAY, config.profile());
    }
}
