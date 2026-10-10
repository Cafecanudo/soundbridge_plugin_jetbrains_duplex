package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.cafecanudo.soundbridgeduplex.IdeAlerts;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliExitCode;
import io.github.cafecanudo.soundbridgeduplex.cli.CliListener;
import io.github.cafecanudo.soundbridgeduplex.cli.CliProcess;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;
import io.github.cafecanudo.soundbridgeduplex.history.SendHistory;
import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendService;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

@Service(Service.Level.PROJECT)
public final class ReceiveService implements Disposable {

    private static final long STOP_WAIT_MILLIS = 5000;
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Project project;
    private final List<ReceiveObserver> observers = new CopyOnWriteArrayList<>();
    private final List<String> log = new CopyOnWriteArrayList<>();
    private volatile CliProcess process;
    private volatile ReceiveState state = ReceiveState.STOPPED;
    private volatile boolean wanted;
    private volatile boolean paused;
    private volatile ReceiveOptions options;

    public ReceiveService(Project project) {
        this.project = project;
    }

    public static ReceiveService getInstance(Project project) {
        return project.getService(ReceiveService.class);
    }

    public ReceiveState state() {
        return state;
    }

    public List<String> log() {
        return new ArrayList<>(log);
    }

    public void addObserver(ReceiveObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ReceiveObserver observer) {
        observers.remove(observer);
    }

    public synchronized void start(ReceiveOptions requested) {
        if (process != null && process.isRunning()) {
            return;
        }
        if (SendService.getInstance(project).isRunning() || CalibrationService.getInstance(project).isRunning()) {
            notifyUser(message("receive.notify.busy"), NotificationType.WARNING);
            return;
        }
        ProjectConfig config = SoundBridgeProjectSettings.getInstance(project).config();
        if (config.input().isEmpty()) {
            notifyUser(message("receive.notify.noInput"), NotificationType.WARNING);
            return;
        }
        options = requested;
        wanted = true;
        launch(config);
    }

    public synchronized void stop() {
        wanted = false;
        paused = false;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            terminate();
            setState(ReceiveState.STOPPED);
        });
    }

    public boolean pauseForTransmit() {
        synchronized (this) {
            if (!wanted || process == null || !process.isRunning()) {
                return false;
            }
            paused = true;
        }
        terminate();
        setState(ReceiveState.PAUSED);
        appendLog(message("receive.log.paused"));
        return true;
    }

    public synchronized void resumeAfterTransmit() {
        if (!paused || !wanted) {
            return;
        }
        paused = false;
        appendLog(message("receive.log.resumed"));
        launch(SoundBridgeProjectSettings.getInstance(project).config());
    }

    @Override
    public void dispose() {
        wanted = false;
        CliProcess current = process;
        if (current != null && current.isRunning()) {
            current.kill();
        }
    }

    private void launch(ProjectConfig config) {
        Path directory = Path.of(options.directory());
        try {
            Files.createDirectories(directory);
            List<String> arguments = ReceiveCommandBuilder.arguments(config, options,
                    SoundBridgeProjectSettings.getInstance(project).calibrationProfile());
            process = CliProcess.start(CliCommand.of(config.launcher(), arguments), WORK_DIRECTORY, new Listener());
            setState(ReceiveState.LISTENING);
        } catch (ExecutionException | java.io.IOException e) {
            wanted = false;
            appendLog(e.getMessage());
            setState(ReceiveState.STOPPED);
            notifyUser(message("receive.notify.failed", e.getMessage()), NotificationType.WARNING);
        }
    }

    private void terminate() {
        CliProcess current = process;
        if (current == null || !current.isRunning()) {
            return;
        }
        current.cancel();
        if (!current.waitFor(STOP_WAIT_MILLIS)) {
            current.kill();
            current.waitFor(STOP_WAIT_MILLIS);
        }
    }

    private void received(ReceivedFile file) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        onUi(() -> SendHistory.getInstance(project).add(HistoryEntry.ofReceived(timestamp, file)));
        if (file.ok() && !file.path().isEmpty()) {
            refresh(Path.of(file.path()));
        }
        String text = message(file.ok() ? "receive.notify.received" : "receive.notify.incomplete", file.name(),
                SendEventReader.formatTime(file.seconds()));
        notifyUser(text, file.ok() ? NotificationType.INFORMATION : NotificationType.WARNING);
    }

    private static void refresh(Path path) {
        Path candidate = path;
        VirtualFile existing = null;
        while (candidate != null && existing == null) {
            existing = LocalFileSystem.getInstance().findFileByNioFile(candidate);
            candidate = candidate.getParent();
        }
        if (existing != null) {
            VfsUtil.markDirtyAndRefresh(true, true, true, existing);
        }
    }

    private void exited(CliExitCode code) {
        boolean expected;
        synchronized (this) {
            expected = paused || !wanted;
            if (!expected) {
                wanted = false;
            }
        }
        if (!expected) {
            setState(ReceiveState.STOPPED);
            notifyUser(message("receive.notify.stopped", code.code()), NotificationType.WARNING);
        }
    }

    private void appendLog(String line) {
        log.add(line);
        onUi(() -> observers.forEach(observer -> observer.onLog(line)));
    }

    private void setState(ReceiveState value) {
        state = value;
        onUi(() -> observers.forEach(observer -> observer.onStateChanged(value)));
    }

    private void notifyUser(String content, NotificationType type) {
        IdeAlerts.notify(project, message("receive.notify.title"), content, type, message("send.notify.open"),
                () -> ToolWindows.show(project, ToolWindows.RECEIVE));
    }

    private static void onUi(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    private final class Listener implements CliListener {

        private final ReceiveEventReader reader = new ReceiveEventReader();

        @Override
        public void onEvent(CliEvent event) {
            ReceiveEventReader.Update update = reader.read(event);
            update.log().forEach(ReceiveService.this::appendLog);
            if (update.state() != null && !paused) {
                setState(update.state());
            }
            if (update.progress() != null) {
                onUi(() -> observers.forEach(observer -> observer.onProgress(update.progress())));
            }
            if (update.received() != null) {
                received(update.received());
            }
        }

        @Override
        public void onText(String line, boolean error) {
            if (!line.isBlank()) {
                reader.text(line).forEach(ReceiveService.this::appendLog);
            }
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exited(code);
        }
    }
}
