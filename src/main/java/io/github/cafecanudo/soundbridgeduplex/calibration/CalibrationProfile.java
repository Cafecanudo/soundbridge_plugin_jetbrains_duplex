package io.github.cafecanudo.soundbridgeduplex.calibration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.Optional;

public record CalibrationProfile(String output, String input, String calibratedAt, LinkChoice choice) {

    private static final String OUTPUT = "output";
    private static final String INPUT = "input";
    private static final String CALIBRATED_AT = "calibrated_at";
    private static final String CHOICE = "choice";

    public boolean matches(String currentOutput, String currentInput) {
        return output.equals(currentOutput) && input.equals(currentInput);
    }

    public String toJson() {
        JsonObject object = new JsonObject();
        object.addProperty(OUTPUT, output);
        object.addProperty(INPUT, input);
        object.addProperty(CALIBRATED_AT, calibratedAt);
        object.add(CHOICE, choice.toJson());
        return object.toString();
    }

    public static Optional<CalibrationProfile> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            return Optional.of(new CalibrationProfile(
                    object.get(OUTPUT).getAsString(),
                    object.get(INPUT).getAsString(),
                    object.get(CALIBRATED_AT).getAsString(),
                    LinkChoice.fromJson(object.getAsJsonObject(CHOICE))));
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
