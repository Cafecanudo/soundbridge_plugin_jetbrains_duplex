package io.github.cafecanudo.soundbridgeduplex.calibration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public record CalibrationResult(LinkChoice forward, LinkChoice backward, List<String> notes, boolean saved) {

    private static final String RESULT_EVENT = "result";
    private static final String SIDE = "side";
    private static final String CALIBRATE_SIDE = "calibrate";
    private static final String FORWARD = "forward";
    private static final String BACKWARD = "backward";
    private static final String NOTES = "notes";
    private static final String SAVED = "saved";

    public CalibrationResult {
        notes = List.copyOf(notes);
    }

    public boolean isUsable() {
        return forward != null && forward.isUsable();
    }

    public static Optional<CalibrationResult> from(CliEvent event) {
        if (!RESULT_EVENT.equals(event.name()) || !event.string(SIDE).filter(CALIBRATE_SIDE::equals).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new CalibrationResult(
                choice(event.data().get(FORWARD)),
                choice(event.data().get(BACKWARD)),
                notes(event.data().getAsJsonArray(NOTES)),
                event.bool(SAVED).orElse(false)));
    }

    private static LinkChoice choice(JsonElement element) {
        return element == null || element.isJsonNull() ? null : LinkChoice.fromJson(element.getAsJsonObject());
    }

    private static List<String> notes(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        return StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsString).toList();
    }
}
