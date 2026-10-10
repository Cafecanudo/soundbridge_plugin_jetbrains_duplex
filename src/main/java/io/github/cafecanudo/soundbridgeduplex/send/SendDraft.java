package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service(Service.Level.PROJECT)
public final class SendDraft {

    private final List<Consumer<List<Path>>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean autoSendRequested = new AtomicBoolean();
    private volatile List<Path> items = List.of();

    public static SendDraft getInstance(Project project) {
        return project.getService(SendDraft.class);
    }

    public List<Path> items() {
        return items;
    }

    public void setItems(List<Path> selected) {
        select(selected, false);
    }

    public void selectFromMenu(List<Path> selected) {
        select(selected, true);
    }

    public boolean consumeAutoSendRequest() {
        return autoSendRequested.getAndSet(false);
    }

    private void select(List<Path> selected, boolean fromMenu) {
        items = List.copyOf(selected);
        autoSendRequested.set(fromMenu);
        listeners.forEach(listener -> listener.accept(items));
    }

    public void addListener(Consumer<List<Path>> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<List<Path>> listener) {
        listeners.remove(listener);
    }
}
