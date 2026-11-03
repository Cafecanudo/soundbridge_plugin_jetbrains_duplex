package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreFilter;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreService;
import io.github.cafecanudo.soundbridgeduplex.memory.MemoryFiles;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class QuickSend {

    private static final int OUTSIDE_SHOWN = 3;

    private QuickSend() {
    }

    static void run(Project project, SendSelection chosen) {
        IgnoreFilter.Result filtered = IgnoreFilter.apply(SendFiles.prepare(chosen,
                SoundBridgeProjectSettings.getInstance(project).expandFolders()),
                IgnoreService.getInstance(project).list(), Files::isDirectory);
        SendSelection selection = filtered.kept();
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        ProjectConfig config = settings.config();
        Set<SendMode> calibrated = SendMode.calibrated(config, settings.uniProfile(), settings.calibrationProfile());
        Optional<SendMode> mode = SendMode.preferred(config, calibrated, settings.sendMode());
        SendService service = SendService.getInstance(project);
        Path base = settings.destinationBase();
        List<Path> outside = SendPlan.outsideBase(base, selection, MemoryFiles.defaultRoot());
        if (!outside.isEmpty()) {
            service.notifyUser(outsideMessage(base, outside), NotificationType.WARNING);
            return;
        }
        if (mode.isEmpty()) {
            service.notifyUser(message("send.autoSend.notStarted", message("send.mode.none")), NotificationType.WARNING);
            return;
        }
        ProjectConfig effective = mode.get().applyTo(config);
        boolean perItem = !selection.isEmpty() && SendPlan.perItemDestination(base, selection, Files::isDirectory);
        String destination = perItem || selection.isEmpty() ? ""
                : DestinationResolver.defaultDestination(base, selection.sends(), Files::isDirectory);
        List<SendJob> jobs = selection.isEmpty() ? List.of() : SendPlan.jobs(base, selection, Files::isDirectory, destination);
        UniProfile parameters = SendCommandBuilder.txParameters(effective, settings.uniProfile(),
                settings.calibrationProfile());
        SendOptions options = new SendOptions("", settings.zipLevel(), McsSelection.of(parameters.mcs()),
                settings.replaceExisting(), settings.packageFolders(), settings.sendCopyMemory());
        List<List<String>> commands = SendCommandBuilder.commands(effective, settings.uniProfile(),
                settings.calibrationProfile(), options, jobs);
        SendSnapshot snapshot = SendSnapshot.of(mode.get(), destination, options);
        service.start(new SendRequest(effective.launcher(), commands, jobs, labels(selection.sends()),
                labels(selection.deletions()), snapshot, settings.commandDelayMs(), labels(filtered.ignoredSends()),
                labels(filtered.ignoredDeletions())));
    }

    static List<String> labels(List<Path> paths) {
        return paths.stream().map(Path::toString).toList();
    }

    static String outsideMessage(Path base, List<Path> outside) {
        String names = outside.stream().limit(OUTSIDE_SHOWN).map(Path::toString).collect(Collectors.joining(", "));
        if (outside.size() > OUTSIDE_SHOWN) {
            names += ", …";
        }
        return message("send.outside.blocked", outside.size(), base, names);
    }

}
