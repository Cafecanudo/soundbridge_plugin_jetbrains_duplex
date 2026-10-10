package io.github.cafecanudo.soundbridgeduplex.calibration;

public interface CalibrationObserver {

    void onLog(String line);

    void onRunningChanged(boolean running);

    void onProfileChanged(CalibrationProfile profile);
}
