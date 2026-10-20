package io.github.cafecanudo.soundbridgeduplex.ignore;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service(Service.Level.PROJECT)
public final class IgnoreService {

    private final Project project;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile IgnoreList cached;

    public IgnoreService(Project project) {
        this.project = project;
    }

    public static IgnoreService getInstance(Project project) {
        return project.getService(IgnoreService.class);
    }

    public IgnoreList list() {
        IgnoreList current = cached;
        if (current == null) {
            current = new IgnoreList(Path.of(settings().projectDirectory()), settings().ignored());
            cached = current;
        }
        return current;
    }

    public boolean isIgnored(Path path) {
        return list().isIgnored(path);
    }

    public void ignore(List<Path> paths) {
        store(list().with(paths));
    }

    public void unignore(List<String> entries) {
        store(list().without(entries));
    }

    public void unignorePaths(List<Path> paths) {
        IgnoreList current = list();
        store(current.without(paths.stream().map(current::key).toList()));
    }

    public void clear() {
        store(List.of());
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void store(List<String> entries) {
        settings().setIgnored(entries);
        cached = null;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                ProjectView.getInstance(project).refresh();
                listeners.forEach(Runnable::run);
            }
        }, ModalityState.any());
    }

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }
}
