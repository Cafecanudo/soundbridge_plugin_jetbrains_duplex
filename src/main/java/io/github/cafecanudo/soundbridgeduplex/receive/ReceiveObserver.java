package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;

public interface ReceiveObserver {

    void onLog(String line);

    void onStateChanged(ReceiveState state);

    void onProgress(SendProgress progress);
}
