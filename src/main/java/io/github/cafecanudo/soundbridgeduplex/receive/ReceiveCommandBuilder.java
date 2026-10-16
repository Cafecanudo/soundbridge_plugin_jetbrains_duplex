package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReceiveCommandBuilder {

    private ReceiveCommandBuilder() {
    }

    public static List<String> arguments(ProjectConfig config, ReceiveOptions options,
                                         Optional<CalibrationProfile> biProfile) {
        List<String> arguments = new ArrayList<>(List.of("--rx", options.directory(), "--in", config.input(), "--keep"));
        if (options.bidirectional() && !config.output().isEmpty()) {
            arguments.add("--out");
            arguments.add(config.output());
            if (!config.profile().isBlank()) {
                arguments.add("--profile");
                arguments.add(config.profile().strip());
            }
            ackPeak(config, biProfile).ifPresent(peak -> {
                arguments.add("--ack-peak");
                arguments.add(peak.toString());
            });
        }
        return arguments;
    }

    public static Optional<Double> ackPeak(ProjectConfig config, Optional<CalibrationProfile> biProfile) {
        return biProfile
                .filter(profile -> profile.matches(config.output(), config.input()))
                .map(profile -> profile.choice().peak());
    }
}
