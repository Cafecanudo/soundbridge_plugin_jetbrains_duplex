package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;

public interface ReceiveObserver {

    void onLog(String line);

    void onStateChanged(ReceiveState state);

    void onProgress(SendProgress progress);

    default void onLogCleared() {
    }

    default void onCalibrationStage(String stage) {
    }

    default void onCalibration(ReceivedCalibration calibration) {
    }

    default void onSequence(int received, int failed, boolean open) {
    }
}
