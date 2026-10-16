package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Service(Service.Level.PROJECT)
public final class SendDraft {

    private final List<Consumer<SendSelection>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean autoSendRequested = new AtomicBoolean();
    private final List<Consumer<HistoryEntry>> resendListeners = new CopyOnWriteArrayList<>();
    private volatile HistoryEntry pendingResend;
    private volatile SendSelection selection = SendSelection.EMPTY;

    public static SendDraft getInstance(Project project) {
        return project.getService(SendDraft.class);
    }

    public List<Path> items() {
        return selection.sends();
    }

    public SendSelection selection() {
        return selection;
    }

    public void setItems(List<Path> selected) {
        setSelection(SendSelection.of(selected));
    }

    public void setSelection(SendSelection selected) {
        select(selected, false);
    }

    public void selectFromMenu(List<Path> selected) {
        selectFromMenu(SendSelection.of(selected));
    }

    public void selectFromMenu(SendSelection selected) {
        select(selected, true);
    }

    public boolean consumeAutoSendRequest() {
        return autoSendRequested.getAndSet(false);
    }

    private void select(SendSelection selected, boolean fromMenu) {
        selection = selected;
        autoSendRequested.set(fromMenu);
        listeners.forEach(listener -> listener.accept(selection));
    }

    public void resend(HistoryEntry entry) {
        pendingResend = entry;
        resendListeners.forEach(listener -> listener.accept(entry));
    }

    public HistoryEntry consumeResend() {
        HistoryEntry entry = pendingResend;
        pendingResend = null;
        return entry;
    }

    public void addResendListener(Consumer<HistoryEntry> listener) {
        resendListeners.add(listener);
    }

    public void removeResendListener(Consumer<HistoryEntry> listener) {
        resendListeners.remove(listener);
    }

    public void addListener(Consumer<SendSelection> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<SendSelection> listener) {
        listeners.remove(listener);
    }
}
