package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

@Service(Service.Level.PROJECT)
public final class SendService {

    private final Project project;
    private final List<SendObserver> observers = new CopyOnWriteArrayList<>();
    private final List<String> log = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<ProgressIndicator> indicator = new AtomicReference<>();

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
        if (CalibrationService.getInstance(project).isRunning()) {
            notifyUser(message("send.notify.busy"), NotificationType.WARNING);
            return false;
        }
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        log.clear();
        notifyRunning(true);
        ProgressManager.getInstance().run(new SendTask(project, request, this));
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

    void finish(SendOutcome outcome) {
        indicator.set(null);
        running.set(false);
        notifyRunning(false);
        notifyUser(outcome.summary(), outcome.succeeded() ? NotificationType.INFORMATION : NotificationType.WARNING);
    }

    private void notifyRunning(boolean value) {
        onUi(() -> observers.forEach(observer -> observer.onRunningChanged(value)));
    }

    private void notifyUser(String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(CalibrationService.NOTIFICATION_GROUP)
                .createNotification(message("send.notify.title"), content, type)
                .notify(project);
    }

    private static void onUi(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }
}
