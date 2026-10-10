package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SendCommandBuilder {

    private static final String ITEM_SEPARATOR = ";";

    private SendCommandBuilder() {
    }

    public static List<String> arguments(ProjectConfig config, UniProfile uniProfile,
                                         Optional<CalibrationProfile> biProfile, SendOptions options, List<Path> items) {
        List<String> arguments = new ArrayList<>();
        arguments.add("--tx");
        arguments.add(items.stream().map(Path::toString).collect(Collectors.joining(ITEM_SEPARATOR)));
        addOption(arguments, "--out", config.output());
        if (config.bidirectional()) {
            arguments.add("--bi");
            addOption(arguments, "--in", config.input());
        }
        UniProfile parameters = txParameters(config, uniProfile, biProfile);
        arguments.addAll(withMcs(parameters, options.mcs()).arguments());
        if (options.mcs().kind() == McsSelection.Kind.AUTO && parameters.mcs() != null) {
            arguments.add("--auto");
        }
        addOption(arguments, "--dest", options.destination());
        addCompression(arguments, options);
        return arguments;
    }

    public static UniProfile txParameters(ProjectConfig config, UniProfile uniProfile,
                                          Optional<CalibrationProfile> biProfile) {
        if (!config.bidirectional()) {
            return uniProfile;
        }
        return biProfile
                .filter(profile -> profile.matches(config.output(), config.input()))
                .map(CalibrationProfile::choice)
                .filter(LinkChoice::isUsable)
                .map(choice -> new UniProfile(choice.mono(), choice.bandHighHz(), choice.peak(), choice.mcs()))
                .orElse(UniProfile.EMPTY);
    }

    private static UniProfile withMcs(UniProfile parameters, McsSelection selection) {
        return switch (selection.kind()) {
            case AUTO -> new UniProfile(parameters.mono(), parameters.bandHighHz(), parameters.peak(), null);
            case FIXED -> new UniProfile(parameters.mono(), parameters.bandHighHz(), parameters.peak(), selection.value());
        };
    }

    private static void addCompression(List<String> arguments, SendOptions options) {
        if (!options.compressed()) {
            arguments.add("--no-zip");
        } else if (options.zipLevel() != SendOptions.DEFAULT_ZIP_LEVEL) {
            arguments.add("--zip-level");
            arguments.add(options.zipLevel().toString());
        }
    }

    private static void addOption(List<String> arguments, String flag, String value) {
        if (value != null && !value.isBlank()) {
            arguments.add(flag);
            arguments.add(value.strip());
        }
    }
}
