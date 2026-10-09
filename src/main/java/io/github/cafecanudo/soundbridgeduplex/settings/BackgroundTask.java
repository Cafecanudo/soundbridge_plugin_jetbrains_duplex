package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import io.github.cafecanudo.soundbridgeduplex.cli.CliException;

import java.util.function.Consumer;

final class BackgroundTask {

    @FunctionalInterface
    interface CliCall<T> {
        T call() throws CliException;
    }

    private BackgroundTask() {
    }

    static <T> void run(CliCall<T> call, Consumer<T> onSuccess, Consumer<String> onFailure) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                T result = call.call();
                onUi(() -> onSuccess.accept(result));
            } catch (CliException e) {
                onUi(() -> onFailure.accept(e.getMessage()));
            }
        });
    }

    private static void onUi(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }
}
