package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import io.github.cafecanudo.soundbridgeduplex.IdeAlerts;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.history.Direction;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;
import io.github.cafecanudo.soundbridgeduplex.history.SendHistory;
import io.github.cafecanudo.soundbridgeduplex.send.SendCommandBuilder;
import io.github.cafecanudo.soundbridgeduplex.send.SendService;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

@Service(Service.Level.PROJECT)
public final class ResendRequestService {

    static final long ANSWER_TIMEOUT_SECONDS = 120;

    private final Project project;
    private volatile Pending pending;
    private volatile boolean requesting;

    private record Pending(String name, HistoryEntry entry, boolean startedReceive) {
    }

    public ResendRequestService(Project project) {
        this.project = project;
    }

    public static ResendRequestService getInstance(Project project) {
        return project.getService(ResendRequestService.class);
    }

    public boolean canRequest(HistoryEntry entry) {
        return entry.direction() == Direction.RECEIVED && !remoteName(entry, receiveDirectory()).isEmpty();
    }

    public void request(HistoryEntry entry) {
        String name = remoteName(entry, receiveDirectory());
        ProjectConfig config = settings().config();
        if (name.isEmpty() || !canRequest(entry)) {
            notifyUser(message("request.notify.noName"), NotificationType.WARNING);
            return;
        }
        if (requesting || pending != null || SendService.getInstance(project).isRunning()
                || CalibrationService.getInstance(project).isRunning()) {
            notifyUser(message("request.notify.busy"), NotificationType.WARNING);
            return;
        }
        if (config.output().isEmpty()) {
            notifyUser(message("request.notify.noOutput"), NotificationType.WARNING);
            return;
        }
        RemoteTarget target = RemoteTarget.of(name);
        List<String> arguments = SendCommandBuilder.requestArguments(config, settings().uniProfile(),
                settings().calibrationProfile(), Path.of(target.file()), target.destination());
        requesting = true;
        ProgressManager.getInstance().run(new RequestTask(project, config.launcher(), arguments, name,
                ok -> finished(ok, name, entry)));
    }

    boolean complete(ReceivedFile file, HistoryEntry received) {
        Pending current = pending;
        if (current == null || !current.name().equals(file.name())) {
            return false;
        }
        pending = null;
        SendHistory.getInstance(project).replace(current.entry(), received);
        if (current.startedReceive()) {
            ReceiveService.getInstance(project).stop();
        }
        return true;
    }

    static String remoteName(HistoryEntry entry, String receiveDirectory) {
        if (!entry.remoteName().isEmpty()) {
            return entry.remoteName();
        }
        if (entry.items().isEmpty()) {
            return "";
        }
        Path item = Path.of(entry.items().get(0));
        if (!item.isAbsolute()) {
            return item.toString().replace('\\', '/');
        }
        Path base = receiveDirectory.isBlank() ? null : Path.of(receiveDirectory).toAbsolutePath().normalize();
        Path normalized = item.toAbsolutePath().normalize();
        if (base == null || !normalized.startsWith(base) || normalized.equals(base)) {
            return "";
        }
        return base.relativize(normalized).toString().replace('\\', '/');
    }

    private void finished(boolean ok, String name, HistoryEntry entry) {
        requesting = false;
        if (!ok) {
            notifyUser(message("request.notify.failed", name), NotificationType.WARNING);
            return;
        }
        ReceiveService receive = ReceiveService.getInstance(project);
        boolean start = receive.state() == ReceiveState.STOPPED;
        pending = new Pending(name, entry, start);
        if (start) {
            receive.start(new ReceiveOptions(receiveBidirectional(), settings().config().receiveDirectory()));
        }
        notifyUser(message("request.notify.waiting", name), NotificationType.INFORMATION);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> expire(name), ANSWER_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
    }

    private void expire(String name) {
        Pending current = pending;
        if (current == null || !current.name().equals(name)) {
            return;
        }
        pending = null;
        if (current.startedReceive()) {
            ReceiveService.getInstance(project).stop();
        }
        notifyUser(message("request.notify.timeout", name), NotificationType.WARNING);
    }

    private boolean receiveBidirectional() {
        return !settings().config().output().isEmpty() && settings().receiveBidirectional();
    }

    private String receiveDirectory() {
        return settings().config().receiveDirectory();
    }

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }

    private void notifyUser(String content, NotificationType type) {
        IdeAlerts.notify(project, message("request.notify.title"), content, type, message("send.notify.open"),
                () -> ToolWindows.show(project, ToolWindows.HISTORY));
    }
}
