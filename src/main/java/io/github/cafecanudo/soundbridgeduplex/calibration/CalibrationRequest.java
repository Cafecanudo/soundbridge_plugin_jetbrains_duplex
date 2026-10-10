package io.github.cafecanudo.soundbridgeduplex.calibration;

import java.util.ArrayList;
import java.util.List;

public record CalibrationRequest(
        String launcher, String output, String input, CalibrationMode mode, CalibrationOptions options) {

    public List<String> arguments() {
        if (mode == CalibrationMode.TWO_WAY) {
            return options.arguments(output, input);
        }
        List<String> arguments = new ArrayList<>(List.of("--calibrate-uni"));
        if (output != null && !output.isBlank()) {
            arguments.add("--out");
            arguments.add(output.strip());
        }
        return arguments;
    }
}
