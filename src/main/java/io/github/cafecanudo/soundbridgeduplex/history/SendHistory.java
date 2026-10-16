package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service(Service.Level.PROJECT)
public final class SendHistory {

    private final Project project;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public SendHistory(Project project) {
        this.project = project;
    }

    public static SendHistory getInstance(Project project) {
        return project.getService(SendHistory.class);
    }

    public List<HistoryEntry> entries() {
        return HistoryLog.read(settings().history());
    }

    public void add(HistoryEntry entry) {
        settings().setHistory(HistoryLog.add(settings().history(), entry));
        listeners.forEach(Runnable::run);
    }

    public void addAll(List<HistoryEntry> entries) {
        List<String> stored = settings().history();
        for (HistoryEntry entry : entries) {
            stored = HistoryLog.add(stored, entry);
        }
        settings().setHistory(stored);
        listeners.forEach(Runnable::run);
    }

    public void replace(HistoryEntry old, HistoryEntry entry) {
        settings().setHistory(HistoryLog.replace(settings().history(), old, entry));
        listeners.forEach(Runnable::run);
    }

    public void clear() {
        settings().setHistory(List.of());
        listeners.forEach(Runnable::run);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }
}
