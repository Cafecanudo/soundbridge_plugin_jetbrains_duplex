package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreList;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyPlan;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record VerifyRequest(String launcher, List<List<String>> commands, List<String> paths, List<Path> outside,
                            Path workDirectory, SendSnapshot snapshot, int commandDelayMs, boolean bidirectional,
                            List<Path> ignored) {

    public VerifyRequest {
        commands = commands.stream().map(List::copyOf).toList();
        paths = List.copyOf(paths);
        outside = List.copyOf(outside);
        ignored = List.copyOf(ignored);
    }

    public static VerifyRequest of(SoundBridgeProjectSettings settings, IgnoreList ignore, SendMode mode,
                                   SendOptions options, List<Path> items) {
        return of(mode.applyTo(settings.config()), settings.uniProfile(), settings.calibrationProfile(),
                settings.destinationBase(), ignore, mode, options, items, settings.commandDelayMs(),
                VerifyPlan.DEFAULT_BATCH_CHARS);
    }

    public static VerifyRequest of(ProjectConfig config, UniProfile uniProfile, Optional<CalibrationProfile> biProfile,
                                   Path base, IgnoreList ignore, SendMode mode, SendOptions options, List<Path> items,
                                   int commandDelayMs, int batchChars) {
        List<Path> files = SendFiles.expand(items);
        List<Path> ignored = files.stream().filter(ignore::isIgnored).toList();
        VerifyPlan plan = VerifyPlan.of(base, files.stream().filter(file -> !ignore.isIgnored(file)).toList());
        SendOptions verifyOptions = new SendOptions("", options.zipLevel(), options.mcs(), false);
        List<List<String>> commands = plan.batches(batchChars).stream()
                .map(batch -> SendCommandBuilder.verifyArguments(config, uniProfile, biProfile, verifyOptions, batch))
                .toList();
        return new VerifyRequest(config.launcher(), commands, plan.paths(), plan.outside(), base,
                SendSnapshot.of(mode, "", verifyOptions), commandDelayMs, mode == SendMode.TWO_WAY, ignored);
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }
}
