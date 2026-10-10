package io.github.cafecanudo.soundbridgeduplex.calibration;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class CalibrationLog {

    private static final String MESSAGE = "message";
    private static final String STAGE = "stage";
    private static final String SIZE = "size";
    private static final String INDEX = "index";
    private static final String COUNT = "count";
    private static final String OK = "ok";
    private static final String ROUNDS = "rounds";
    private static final String SECONDS = "seconds";
    private static final String ATTEMPT = "attempt";
    private static final String PASSED = "passed";
    private static final String MCS = "mcs";
    private static final String NESTED_INDENT = "  ";
    private static final String SIDE = "side";
    private static final String ONE_WAY_SIDE = "calibrate_uni";

    private CalibrationLog() {
    }

    public static List<String> lines(CliEvent event) {
        return switch (event.name()) {
            case "info" -> event.string(MESSAGE).map(List::of).orElse(List.of());
            case "calibrate_stage" -> event.string(STAGE).map(s -> List.of(message("calibration.log.stage", s))).orElse(List.of());
            case "calibrate_test" -> List.of(testLine(event));
            case "calibrate_attempt" -> List.of(attemptLine(event));
            case "result" -> resultEventLines(event);
            default -> List.of();
        };
    }

    private static List<String> resultEventLines(CliEvent event) {
        if (event.string(SIDE).filter(ONE_WAY_SIDE::equals).isPresent()) {
            return List.of(message("calibration.log.oneWaySent"));
        }
        return CalibrationResult.from(event).map(CalibrationLog::resultLines).orElse(List.of());
    }

    public static List<String> resultLines(CalibrationResult result) {
        List<String> lines = new ArrayList<>();
        result.notes().forEach(note -> lines.add(message("calibration.log.warning", note)));
        lines.add(message("calibration.log.forward", describe(result.forward())));
        lines.add(message("calibration.log.backward", describe(result.backward())));
        return lines;
    }

    private static String describe(LinkChoice choice) {
        if (choice == null) {
            return message("calibration.choice.unusable");
        }
        String warnings = choice.warnings().isEmpty() ? "" : " · " + String.join(" · ", choice.warnings());
        return choice.summary() + warnings;
    }

    private static String testLine(CliEvent event) {
        String status = message(event.bool(OK).orElse(false) ? "calibration.log.ok" : "calibration.log.failed");
        return NESTED_INDENT + message("calibration.log.test",
                TestSize.labelOf((long) event.number(SIZE).orElse(0)),
                (int) event.number(INDEX).orElse(0),
                (int) event.number(COUNT).orElse(0),
                status,
                (int) event.number(ROUNDS).orElse(0),
                String.format(Locale.ROOT, "%.1f", event.number(SECONDS).orElse(0)));
    }

    private static String attemptLine(CliEvent event) {
        return message("calibration.log.attempt",
                (int) event.number(ATTEMPT).orElse(0),
                TestSize.labelOf((long) event.number(SIZE).orElse(0)),
                (int) event.number(PASSED).orElse(0),
                (int) event.number(COUNT).orElse(0),
                (int) event.number(MCS).orElse(0));
    }
}
