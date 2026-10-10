package io.github.cafecanudo.soundbridgeduplex.send;

public interface SendObserver {

    void onLog(String line);

    void onRunningChanged(boolean running);

    void onProgress(SendProgress progress);

    void onFinished(SendOutcome outcome);
}
