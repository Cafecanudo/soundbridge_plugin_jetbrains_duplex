package io.github.cafecanudo.soundbridgeduplex.calibration;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.IdeAlerts;
import io.github.cafecanudo.soundbridgeduplex.send.SendService;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

@Service(Service.Level.PROJECT)
public final class CalibrationService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Project project;
    private final List<CalibrationObserver> observers = new CopyOnWriteArrayList<>();
    private final List<String> log = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<ProgressIndicator> indicator = new AtomicReference<>();

    public CalibrationService(Project project) {
        this.project = project;
    }

    public static CalibrationService getInstance(Project project) {
        return project.getService(CalibrationService.class);
    }

    public boolean isRunning() {
        return running.get();
    }

    public List<String> log() {
        return new ArrayList<>(log);
    }

    public void addObserver(CalibrationObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(CalibrationObserver observer) {
        observers.remove(observer);
    }

    public boolean start(CalibrationRequest request) {
        if (SendService.getInstance(project).isRunning()) {
            notifyUser(message("calibration.notify.busy"), NotificationType.WARNING);
            return false;
        }
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        log.clear();
        notifyRunning(true);
        ProgressManager.getInstance().run(new CalibrationTask(project, request, this));
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

    void finish(CalibrationRequest request, CalibrationResult result, String failure, boolean succeeded) {
        indicator.set(null);
        running.set(false);
        notifyRunning(false);
        if (request.mode() == CalibrationMode.ONE_WAY) {
            finishOneWay(failure, succeeded);
        } else if (result != null && result.isUsable()) {
            CalibrationProfile profile = storeProfile(request, result);
            notifyUser(message("calibration.notify.done", profile.choice().summary()), NotificationType.INFORMATION);
        } else {
            notifyUser(failure != null ? failure : message("calibration.notify.unusable"), NotificationType.WARNING);
        }
    }

    private void finishOneWay(String failure, boolean succeeded) {
        if (succeeded) {
            notifyUser(message("calibration.notify.oneWayDone"), NotificationType.INFORMATION);
        } else {
            notifyUser(failure != null ? failure : message("calibration.notify.oneWayFailed"), NotificationType.WARNING);
        }
    }

    private CalibrationProfile storeProfile(CalibrationRequest request, CalibrationResult result) {
        CalibrationProfile profile = new CalibrationProfile(
                request.output(), request.input(), LocalDateTime.now().format(TIMESTAMP), result.forward());
        if (request.options().save()) {
            SoundBridgeProjectSettings.getInstance(project).updateCalibrationProfile(profile);
            onUi(() -> observers.forEach(observer -> observer.onProfileChanged(profile)));
        }
        return profile;
    }

    private void notifyRunning(boolean value) {
        onUi(() -> observers.forEach(observer -> observer.onRunningChanged(value)));
    }

    private void notifyUser(String content, NotificationType type) {
        IdeAlerts.notify(project, message("calibration.notify.title"), content, type);
    }

    private static void onUi(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }
}
