package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class QuickSend {

    private QuickSend() {
    }

    static void run(Project project, List<Path> items) {
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        ProjectConfig config = settings.config();
        Set<SendMode> calibrated = SendMode.calibrated(config, settings.uniProfile(), settings.calibrationProfile());
        Optional<SendMode> mode = SendMode.preferred(config, calibrated);
        SendService service = SendService.getInstance(project);
        if (mode.isEmpty()) {
            service.notifyUser(message("send.autoSend.notStarted", message("send.mode.none")), NotificationType.WARNING);
            return;
        }
        ProjectConfig effective = mode.get().applyTo(config);
        Path base = projectBase(project);
        String destination = DestinationResolver.defaultDestination(base, items, Files::isDirectory);
        List<SendJob> jobs = SendPlan.jobs(base, items, Files::isDirectory, destination);
        UniProfile parameters = SendCommandBuilder.txParameters(effective, settings.uniProfile(),
                settings.calibrationProfile());
        SendOptions options = new SendOptions("", settings.zipLevel(), McsSelection.of(parameters.mcs()),
                settings.replaceExisting());
        List<List<String>> commands = SendCommandBuilder.commands(effective, settings.uniProfile(),
                settings.calibrationProfile(), options, jobs);
        service.start(new SendRequest(effective.launcher(), commands, items.stream().map(Path::toString).toList()));
    }

    private static Path projectBase(Project project) {
        String basePath = project.getBasePath();
        return basePath == null ? null : Path.of(basePath);
    }
}
