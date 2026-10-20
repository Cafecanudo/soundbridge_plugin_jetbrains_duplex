package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.IdeAlerts;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;
import io.github.cafecanudo.soundbridgeduplex.history.SendStatus;
import io.github.cafecanudo.soundbridgeduplex.history.SentEntries;
import io.github.cafecanudo.soundbridgeduplex.history.SendHistory;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyItem;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyOutcome;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyResultDialog;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

@Service(Service.Level.PROJECT)
public final class SendService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Project project;
    private final List<SendObserver> observers = new CopyOnWriteArrayList<>();
    private final List<String> log = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<ProgressIndicator> indicator = new AtomicReference<>();
    private final AtomicReference<HistoryEntry> pendingVerify = new AtomicReference<>();

    public SendService(Project project) {
        this.project = project;
    }

    public static SendService getInstance(Project project) {
        return project.getService(SendService.class);
    }

    public boolean isRunning() {
        return running.get();
    }

    public List<String> log() {
        return new ArrayList<>(log);
    }

    public void addObserver(SendObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(SendObserver observer) {
        observers.remove(observer);
    }

    public boolean start(SendRequest request) {
        if (!acquire()) {
            return false;
        }
        ProgressManager.getInstance().run(new SendTask(project, request, this));
        return true;
    }

    public boolean startVerify(VerifyRequest request) {
        if (!request.outside().isEmpty()) {
            notifyVerify(message("verify.notify.outside", request.outside().size(), request.workDirectory()),
                    NotificationType.WARNING);
        }
        if (request.isEmpty()) {
            notifyVerify(message("verify.notify.empty", request.workDirectory()), NotificationType.WARNING);
            return false;
        }
        if (!acquire()) {
            return false;
        }
        if (request.bidirectional()) {
            HistoryEntry requesting = verifyEntry(request, SendStatus.REQUESTING, List.of(), 0);
            pendingVerify.set(requesting);
            SendHistory.getInstance(project).add(requesting);
        }
        ProgressManager.getInstance().run(new VerifyTask(project, request, this));
        return true;
    }

    private boolean acquire() {
        if (CalibrationService.getInstance(project).isRunning()) {
            notifyUser(message("send.notify.busy"), NotificationType.WARNING);
            return false;
        }
        if (!running.compareAndSet(false, true)) {
            notifyUser(message("send.notify.running"), NotificationType.WARNING);
            return false;
        }
        log.clear();
        notifyRunning(true);
        return true;
    }

    public void cancel() {
        ProgressIndicator current = indicator.get();
        if (current != null) {
            current.cancel();
        }
    }

    void attach(ProgressIndicator current) {
        indicator.set(current);
    }

    void appendLog(String line) {
        log.add(line);
        onUi(() -> observers.forEach(observer -> observer.onLog(line)));
    }

    void publishProgress(SendProgress progress) {
        onUi(() -> observers.forEach(observer -> observer.onProgress(progress)));
    }

    void finish(SendRequest request, SendOutcome outcome, List<SentFile> files) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        SendHistory.getInstance(project).addAll(SentEntries.of(timestamp, request, files, outcome, log(),
                Files::isDirectory));
        indicator.set(null);
        running.set(false);
        notifyRunning(false);
        onUi(() -> observers.forEach(observer -> observer.onFinished(outcome)));
        notifyUser(outcome.summary(), outcome.succeeded() ? NotificationType.INFORMATION : NotificationType.WARNING);
    }

    void finishVerify(VerifyRequest request, VerifyOutcome outcome) {
        HistoryEntry entry = verifyEntry(request, outcome.status(), outcome.items(), outcome.seconds());
        HistoryEntry requesting = pendingVerify.getAndSet(null);
        if (requesting == null) {
            SendHistory.getInstance(project).add(entry);
        } else {
            SendHistory.getInstance(project).replace(requesting, entry);
        }
        indicator.set(null);
        running.set(false);
        notifyRunning(false);
        onUi(() -> observers.forEach(observer -> observer.onVerified(outcome)));
        if (outcome.answered()) {
            onUi(() -> new VerifyResultDialog(project, entry.timestamp(), outcome.items()).show());
        }
        notifyVerify(outcome.summary(), outcome.succeeded() ? NotificationType.INFORMATION : NotificationType.WARNING);
    }

    private HistoryEntry verifyEntry(VerifyRequest request, SendStatus status, List<VerifyItem> items,
                                     double seconds) {
        return HistoryEntry.ofVerify(LocalDateTime.now().format(TIMESTAMP), request.paths(), request.snapshot(), status,
                items, seconds, log());
    }

    private void notifyVerify(String content, NotificationType type) {
        IdeAlerts.notify(project, message("verify.notify.title"), content, type, message("send.notify.open"),
                this::openToolWindow);
    }

    private void notifyRunning(boolean value) {
        onUi(() -> observers.forEach(observer -> observer.onRunningChanged(value)));
    }

    public void notifyUser(String content, NotificationType type) {
        IdeAlerts.notify(project, message("send.notify.title"), content, type, message("send.notify.open"),
                this::openToolWindow);
    }

    private void openToolWindow() {
        SoundBridgeToolWindowFactory.show(project);
    }

    private static void onUi(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }
}
