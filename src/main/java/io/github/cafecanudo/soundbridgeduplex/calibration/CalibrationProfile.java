package io.github.cafecanudo.soundbridgeduplex.calibration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Optional;

public record CalibrationProfile(String output, String input, String calibratedAt, LinkChoice choice, boolean manual) {

    private static final String OUTPUT = "output";
    private static final String INPUT = "input";
    private static final String CALIBRATED_AT = "calibrated_at";
    private static final String CHOICE = "choice";
    private static final String MANUAL = "manual";

    public CalibrationProfile(String output, String input, String calibratedAt, LinkChoice choice) {
        this(output, input, calibratedAt, choice, false);
    }

    public boolean matches(String currentOutput, String currentInput) {
        return output.equals(currentOutput) && input.equals(currentInput);
    }

    public UniProfile values() {
        return new UniProfile(choice.mono(), choice.bandHighHz(), choice.peak(), choice.mcs());
    }

    public static UniProfile values(Optional<CalibrationProfile> profile) {
        return profile.map(CalibrationProfile::values).orElse(UniProfile.EMPTY);
    }

    public static CalibrationProfile adjusted(Optional<CalibrationProfile> base, UniProfile values,
                                              String output, String input, String adjustedAt) {
        Double snrDb = base.map(profile -> profile.choice().snrDb()).orElse(null);
        LinkChoice choice = new LinkChoice(values.peak(), values.mono(), values.bandHighHz(), values.mcs(), snrDb,
                List.of(), false, List.of());
        return new CalibrationProfile(output, input, adjustedAt, choice, true);
    }

    public String toJson() {
        JsonObject object = new JsonObject();
        object.addProperty(OUTPUT, output);
        object.addProperty(INPUT, input);
        object.addProperty(CALIBRATED_AT, calibratedAt);
        object.add(CHOICE, choice.toJson());
        object.addProperty(MANUAL, manual);
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
                    LinkChoice.fromJson(object.getAsJsonObject(CHOICE)),
                    object.has(MANUAL) && object.get(MANUAL).getAsBoolean()));
        } catch (JsonParseException | IllegalStateException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
