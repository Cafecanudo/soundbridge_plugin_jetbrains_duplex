package io.github.cafecanudo.soundbridgeduplex.calibration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public record LinkChoice(
        Double peak,
        boolean mono,
        Double bandHighHz,
        Integer mcs,
        Double snrDb,
        List<String> warnings,
        boolean validated,
        List<SizeResult> tests) {

    private static final String PEAK = "peak";
    private static final String MONO = "mono";
    private static final String BAND = "band_high_hz";
    private static final String MCS = "mcs";
    private static final String SNR = "snr_db";
    private static final String WARNINGS = "warnings";
    private static final String VALIDATED = "validated";
    private static final String TESTS = "tests";
    private static final String SIZE = "size";
    private static final String PASSED = "passed";
    private static final String TESTED = "tested";
    private static final double HZ_PER_KHZ = 1000.0;
    private static final String SEPARATOR = " · ";
    private static final MathContext SIGNIFICANT_DIGITS = new MathContext(4);

    public LinkChoice {
        warnings = List.copyOf(warnings);
        tests = List.copyOf(tests);
    }

    public boolean isUsable() {
        return peak != null && bandHighHz != null && mcs != null;
    }

    public String summary() {
        if (!isUsable()) {
            return message("calibration.choice.unusable");
        }
        List<String> parts = new ArrayList<>();
        parts.add(message(mono ? "calibration.choice.mono" : "calibration.choice.stereo"));
        parts.add(significant(bandHighHz / HZ_PER_KHZ) + " kHz");
        parts.add("MCS " + mcs);
        parts.add(message("calibration.choice.peak", significant(peak)));
        if (snrDb != null) {
            parts.add(String.format(Locale.ROOT, "SNR %.0f dB", snrDb));
        }
        if (!tests.isEmpty()) {
            parts.add(tests.stream().map(SizeResult::label).collect(Collectors.joining(", ")));
        }
        return String.join(SEPARATOR, parts);
    }

    public static LinkChoice fromJson(JsonObject object) {
        return new LinkChoice(
                optionalDouble(object, PEAK),
                object.has(MONO) && object.get(MONO).getAsBoolean(),
                optionalDouble(object, BAND),
                object.has(MCS) && !object.get(MCS).isJsonNull() ? object.get(MCS).getAsInt() : null,
                optionalDouble(object, SNR),
                strings(object.getAsJsonArray(WARNINGS)),
                object.has(VALIDATED) && object.get(VALIDATED).getAsBoolean(),
                sizeResults(object.getAsJsonArray(TESTS)));
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.add(PEAK, number(peak));
        object.addProperty(MONO, mono);
        object.add(BAND, number(bandHighHz));
        object.add(MCS, number(mcs));
        object.add(SNR, number(snrDb));
        JsonArray warningArray = new JsonArray();
        warnings.forEach(warningArray::add);
        object.add(WARNINGS, warningArray);
        object.addProperty(VALIDATED, validated);
        JsonArray testArray = new JsonArray();
        tests.forEach(test -> testArray.add(sizeResult(test)));
        object.add(TESTS, testArray);
        return object;
    }

    private static String significant(double value) {
        return BigDecimal.valueOf(value).round(SIGNIFICANT_DIGITS).stripTrailingZeros().toPlainString();
    }

    private static Double optionalDouble(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsDouble();
    }

    private static JsonElement number(Number value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        return StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsString).toList();
    }

    private static List<SizeResult> sizeResults(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        return StreamSupport.stream(array.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(o -> new SizeResult(o.get(SIZE).getAsLong(), o.get(PASSED).getAsInt(), o.get(TESTED).getAsInt()))
                .toList();
    }

    private static JsonObject sizeResult(SizeResult result) {
        JsonObject object = new JsonObject();
        object.addProperty(SIZE, result.size());
        object.addProperty(PASSED, result.passed());
        object.addProperty(TESTED, result.tested());
        return object;
    }
}
