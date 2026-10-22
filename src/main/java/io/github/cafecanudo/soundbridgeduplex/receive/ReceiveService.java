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
import io.github.cafecanudo.soundbridgeduplex.send.SendService;
import io.github.cafecanudo.soundbridgeduplex.send.SentRegistry;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifySummary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

@Service(Service.Level.PROJECT)
public final class ReceiveService implements Disposable {

    private static final long STOP_WAIT_MILLIS = 5000;
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String MORE_TIMEOUT = "more_timeout";

    private final Project project;
    private final List<ReceiveObserver> observers = new CopyOnWriteArrayList<>();
    private final ReceiveSequence sequence = new ReceiveSequence();
    private final List<String> log = new CopyOnWriteArrayList<>();
    private volatile CliProcess process;
    private volatile ReceiveState state = ReceiveState.STOPPED;
    private volatile boolean wanted;
    private volatile boolean paused;
    private volatile ReceiveOptions options;
    private volatile boolean calibrateOnly;
    private volatile ReceivedCalibration lastCalibration;

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

    public ReceiveOptions options() {
        return options;
    }

    public boolean isCalibrateOnly() {
        return calibrateOnly;
    }

    public ReceivedCalibration lastCalibration() {
        return lastCalibration;
    }

    public void start(ReceiveOptions requested) {
        start(requested, false);
    }

    public void startCalibration(ReceiveOptions requested) {
        start(requested, true);
    }

    private synchronized void start(ReceiveOptions requested, boolean calibration) {
        CliProcess current = process;
        if (current != null && current.isRunning()) {
            if (!wanted) {
                startAfterExit(current, requested, calibration);
            }
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
        calibrateOnly = calibration;
        wanted = true;
        log.clear();
        onUi(() -> observers.forEach(ReceiveObserver::onLogCleared));
        launch(config);
    }

    public void directoryChanged(String directory) {
        boolean running = state != ReceiveState.STOPPED;
        if (running) {
            stop();
        }
        onUi(() -> observers.forEach(observer -> observer.onDirectoryChanged(directory)));
        if (running) {
            notifyUser(message("receive.notify.directoryChanged", directory), NotificationType.INFORMATION);
        }
    }

    public synchronized void stop() {
        wanted = false;
        paused = false;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            terminate();
            if (!wanted) {
                setState(ReceiveState.STOPPED);
            }
        });
    }

    public void changeOptions(ReceiveOptions requested) {
        synchronized (this) {
            if (paused) {
                options = requested;
                return;
            }
            if (!wanted) {
                return;
            }
            wanted = false;
        }
        appendLog(message("receive.log.restarting"));
        boolean calibration = calibrateOnly;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            terminate();
            onUi(() -> start(requested, calibration));
        });
    }

    private void startAfterExit(CliProcess stopping, ReceiveOptions requested, boolean calibration) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!stopping.waitFor(STOP_WAIT_MILLIS)) {
                stopping.kill();
                stopping.waitFor(STOP_WAIT_MILLIS);
            }
            onUi(() -> start(requested, calibration));
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
        HistoryEntry entry = HistoryEntry.ofReceived(timestamp, file);
        onUi(() -> {
            if (!ResendRequestService.getInstance(project).complete(file, entry)) {
                SendHistory.getInstance(project).add(entry);
            }
        });
        if (file.ok() && !file.path().isEmpty() && !file.inMemory()) {
            Path path = Path.of(file.path());
            SentRegistry.state(path).ifPresent(state ->
                    SoundBridgeProjectSettings.getInstance(project).recordSent(Map.of(SentRegistry.key(path), state)));
            refresh(path);
        }
        List<ReceiveSequence.Notice> notices;
        synchronized (sequence) {
            notices = sequence.add(file);
        }
        publish(notices);
    }

    private void endSequence() {
        List<ReceiveSequence.Notice> notices;
        synchronized (sequence) {
            notices = sequence.end();
        }
        publish(notices);
    }

    private void publish(List<ReceiveSequence.Notice> notices) {
        int received;
        int failed;
        boolean open;
        synchronized (sequence) {
            received = sequence.received();
            failed = sequence.failed();
            open = sequence.isOpen();
        }
        onUi(() -> observers.forEach(observer -> observer.onSequence(received, failed, open)));
        notices.forEach(notice -> notifyUser(notice.text(),
                notice.warning() ? NotificationType.WARNING : NotificationType.INFORMATION));
    }

    private void verified(ReceivedVerification verification) {
        HistoryEntry entry = HistoryEntry.ofVerifyReceived(LocalDateTime.now().format(TIMESTAMP), verification);
        onUi(() -> SendHistory.getInstance(project).add(entry));
        String text = verification.ok() ? message("receive.notify.verified", VerifySummary.text(verification.items()))
                : message("receive.log.verifyInvalid");
        notifyUser(text, verification.ok() && VerifySummary.allExist(verification.items())
                ? NotificationType.INFORMATION : NotificationType.WARNING);
    }

    private void calibrated(ReceivedCalibration calibration) {
        ReceivedCalibration stamped = calibration.at(LocalDateTime.now().format(TIMESTAMP));
        lastCalibration = stamped;
        onUi(() -> observers.forEach(observer -> observer.onCalibration(stamped)));
        notifyUser(message("receive.notify.calibrated"), NotificationType.INFORMATION);
        if (calibrateOnly) {
            stop();
        }
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
        endSequence();
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
            if (MORE_TIMEOUT.equals(event.name())) {
                endSequence();
            }
            if (update.state() != null && !paused) {
                setState(update.state());
            }
            if (update.progress() != null) {
                onUi(() -> observers.forEach(observer -> observer.onProgress(update.progress())));
            }
            if (update.received() != null) {
                received(update.received());
            }
            if (update.calibrationStage() != null) {
                onUi(() -> observers.forEach(observer -> observer.onCalibrationStage(update.calibrationStage())));
            }
            if (update.verification() != null) {
                verified(update.verification());
            }
            if (update.calibration() != null) {
                calibrated(update.calibration());
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
