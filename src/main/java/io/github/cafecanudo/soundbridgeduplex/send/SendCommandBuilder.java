package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SendCommandBuilder {

    private static final String ITEM_SEPARATOR = ";";
    private static final String MORE_FLAG = "--more";
    private static final String REPLACE_FLAG = "--force-repl";
    private static final String DELETE_FLAG = "--delete";
    private static final String REQUEST_FLAG = "--request";
    private static final String TX_FLAG = "--tx";
    private static final String VERIFY_FLAG = "--verify";
    private static final Pattern GLOB_SPECIAL = Pattern.compile("[\\[*?]");

    private SendCommandBuilder() {
    }

    public static List<String> arguments(ProjectConfig config, UniProfile uniProfile,
                                         Optional<CalibrationProfile> biProfile, SendOptions options, List<Path> items) {
        List<String> arguments = linkArguments(TX_FLAG, joined(items, SendCommandBuilder::literal), config, uniProfile,
                biProfile, options.mcs());
        addOption(arguments, "--dest", options.destination());
        addCompression(arguments, options);
        if (options.replaceExisting()) {
            arguments.add(REPLACE_FLAG);
        }
        return arguments;
    }

    public static List<String> deleteArguments(ProjectConfig config, UniProfile uniProfile,
                                               Optional<CalibrationProfile> biProfile, McsSelection mcs, SendJob job) {
        List<String> arguments = linkArguments(TX_FLAG, joined(job.items(), Path::toString), config, uniProfile,
                biProfile, mcs);
        addOption(arguments, "--dest", job.destination());
        arguments.add(DELETE_FLAG);
        return arguments;
    }

    public static List<String> requestArguments(ProjectConfig config, UniProfile uniProfile,
                                                Optional<CalibrationProfile> biProfile, Path item, String destination) {
        List<String> arguments = linkArguments(TX_FLAG, item.toString(), config, uniProfile, biProfile,
                McsSelection.AUTO);
        addOption(arguments, "--dest", destination);
        arguments.add(REQUEST_FLAG);
        return arguments;
    }

    public static List<String> verifyArguments(ProjectConfig config, UniProfile uniProfile,
                                               Optional<CalibrationProfile> biProfile, SendOptions options,
                                               List<String> paths) {
        List<String> arguments = linkArguments(VERIFY_FLAG, String.join(ITEM_SEPARATOR, paths), config, uniProfile,
                biProfile, options.mcs());
        addCompression(arguments, options);
        return arguments;
    }

    public static List<List<String>> commands(ProjectConfig config, UniProfile uniProfile,
                                              Optional<CalibrationProfile> biProfile, SendOptions options,
                                              List<SendJob> jobs) {
        List<List<String>> commands = new ArrayList<>();
        for (int i = 0; i < jobs.size(); i++) {
            SendJob job = jobs.get(i);
            SendOptions jobOptions = new SendOptions(job.destination(), options.zipLevel(), options.mcs(),
                    options.replaceExisting());
            List<String> arguments = job.delete()
                    ? deleteArguments(config, uniProfile, biProfile, options.mcs(), job)
                    : arguments(config, uniProfile, biProfile, jobOptions, job.items());
            if (i < jobs.size() - 1) {
                arguments.add(MORE_FLAG);
            }
            commands.add(arguments);
        }
        return commands;
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

    private static String joined(List<Path> items, Function<Path, String> render) {
        return items.stream().map(render).collect(Collectors.joining(ITEM_SEPARATOR));
    }

    private static List<String> linkArguments(String actionFlag, String items, ProjectConfig config,
                                              UniProfile uniProfile, Optional<CalibrationProfile> biProfile,
                                              McsSelection mcs) {
        List<String> arguments = new ArrayList<>();
        arguments.add(actionFlag);
        arguments.add(items);
        addOption(arguments, "--out", config.output());
        addOption(arguments, "--profile", config.profile());
        if (config.bidirectional()) {
            arguments.add("--bi");
            addOption(arguments, "--in", config.input());
        }
        UniProfile parameters = txParameters(config, uniProfile, biProfile);
        arguments.addAll(withMcs(parameters, mcs).arguments());
        if (mcs.kind() == McsSelection.Kind.AUTO && parameters.mcs() != null) {
            arguments.add("--auto");
        }
        return arguments;
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

    static String literal(Path item) {
        return GLOB_SPECIAL.matcher(item.toString()).replaceAll("[$0]");
    }

    private static void addOption(List<String> arguments, String flag, String value) {
        if (value != null && !value.isBlank()) {
            arguments.add(flag);
            arguments.add(value.strip());
        }
    }
}
