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

    private final List<Consumer<SendSelection>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean autoSendRequested = new AtomicBoolean();
    private final AtomicBoolean verifyRequested = new AtomicBoolean();
    private final List<Consumer<ResendBatch>> resendListeners = new CopyOnWriteArrayList<>();
    private volatile ResendBatch pendingResend;
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

    public void selectForVerify(SendSelection selected) {
        verifyRequested.set(true);
        select(selected, false);
    }

    public boolean consumeVerifyRequest() {
        return verifyRequested.getAndSet(false);
    }

    public boolean consumeAutoSendRequest() {
        return autoSendRequested.getAndSet(false);
    }

    private void select(SendSelection selected, boolean fromMenu) {
        selection = selected;
        autoSendRequested.set(fromMenu);
        listeners.forEach(listener -> listener.accept(selection));
    }

    public void resend(ResendBatch batch) {
        pendingResend = batch;
        resendListeners.forEach(listener -> listener.accept(batch));
    }

    public ResendBatch consumeResend() {
        ResendBatch batch = pendingResend;
        pendingResend = null;
        return batch;
    }

    public void addResendListener(Consumer<ResendBatch> listener) {
        resendListeners.add(listener);
    }

    public void removeResendListener(Consumer<ResendBatch> listener) {
        resendListeners.remove(listener);
    }

    public void addListener(Consumer<SendSelection> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<SendSelection> listener) {
        listeners.remove(listener);
    }
}
