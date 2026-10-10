package io.github.cafecanudo.soundbridgeduplex.calibration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

public record UniProfile(boolean mono, Double bandHighHz, Double peak, Integer mcs) {

    public static final List<Double> BANDS_HZ = List.of(5000.0, 7000.0, 8000.0, 10000.0, 14000.0, 18000.0, 22000.0);
    public static final List<Integer> MCS_VALUES = IntStream.rangeClosed(0, 9).boxed().toList();
    public static final UniProfile EMPTY = new UniProfile(false, null, null, null);

    private static final double PEAK_MIN_EXCLUSIVE = 0.0;
    private static final double PEAK_MAX = 1.0;

    public boolean isComplete() {
        return bandHighHz != null && peak != null && mcs != null;
    }

    public List<String> arguments() {
        List<String> arguments = new ArrayList<>();
        if (mono) {
            arguments.add("--mono");
        }
        if (bandHighHz != null) {
            arguments.add("--band-high");
            arguments.add(String.format(Locale.ROOT, "%.0f", bandHighHz));
        }
        if (peak != null) {
            arguments.add("--peak");
            arguments.add(peak.toString());
        }
        if (mcs != null) {
            arguments.add("--mcs");
            arguments.add(mcs.toString());
        }
        return arguments;
    }

    public static Optional<Double> parsePeak(String text) {
        String value = text == null ? "" : text.strip().replace(',', '.');
        if (value.isEmpty()) {
            return Optional.empty();
        }
        double peak = Double.parseDouble(value);
        if (peak <= PEAK_MIN_EXCLUSIVE || peak > PEAK_MAX) {
            throw new NumberFormatException(value);
        }
        return Optional.of(peak);
    }

    public static Double parseDouble(String text) {
        return text == null || text.isBlank() ? null : Double.valueOf(text);
    }

    public static Integer parseInteger(String text) {
        return text == null || text.isBlank() ? null : Integer.valueOf(text);
    }
}
