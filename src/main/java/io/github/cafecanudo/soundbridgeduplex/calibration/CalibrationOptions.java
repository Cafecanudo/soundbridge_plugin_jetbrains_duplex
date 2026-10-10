package io.github.cafecanudo.soundbridgeduplex.calibration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record CalibrationOptions(Set<TestSize> sizes, boolean validate, boolean save) {

    public static final CalibrationOptions DEFAULT = new CalibrationOptions(EnumSet.of(TestSize.DEFAULT), true, true);

    private static final String SIZE_SEPARATOR = ",";

    public CalibrationOptions {
        sizes = sizes.isEmpty() ? EnumSet.noneOf(TestSize.class) : EnumSet.copyOf(sizes);
    }

    public List<String> arguments(String output, String input) {
        List<String> arguments = new ArrayList<>(List.of("--calibrate"));
        addOption(arguments, "--out", output);
        addOption(arguments, "--in", input);
        if (validate && !sizes.isEmpty()) {
            arguments.add("--calibrate-test");
            arguments.add(encodeSizes(sizes));
        }
        return arguments;
    }

    public static String encodeSizes(Collection<TestSize> sizes) {
        return sizes.stream().sorted().map(TestSize::label).collect(Collectors.joining(SIZE_SEPARATOR));
    }

    public static Set<TestSize> decodeSizes(String text) {
        Set<TestSize> sizes = EnumSet.noneOf(TestSize.class);
        Arrays.stream(text == null ? new String[0] : text.split(SIZE_SEPARATOR))
                .map(TestSize::ofLabel)
                .flatMap(Optional::stream)
                .forEach(sizes::add);
        return sizes;
    }

    private static void addOption(List<String> arguments, String flag, String value) {
        if (value != null && !value.isBlank()) {
            arguments.add(flag);
            arguments.add(value.strip());
        }
    }
}
