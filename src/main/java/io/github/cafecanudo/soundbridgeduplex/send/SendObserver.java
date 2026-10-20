package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.verify.VerifyOutcome;

public interface SendObserver {

    void onLog(String line);

    void onRunningChanged(boolean running);

    void onProgress(SendProgress progress);

    void onFinished(SendOutcome outcome);

    default void onVerified(VerifyOutcome outcome) {
    }

    default void onTotal(SendTotals.Total total) {
    }
}
