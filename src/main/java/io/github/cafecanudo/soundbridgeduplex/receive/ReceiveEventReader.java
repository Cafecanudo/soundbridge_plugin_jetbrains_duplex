package io.github.cafecanudo.soundbridgeduplex.receive;

import com.google.gson.JsonElement;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyItem;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifySummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceiveEventReader {

    public record Update(List<String> log, ReceiveState state, SendProgress progress, ReceivedFile received,
                         String calibrationStage, ReceivedCalibration calibration, ReceivedVerification verification) {

        static final Update NONE = new Update(List.of(), null, null, null);

        Update(List<String> log, ReceiveState state, SendProgress progress, ReceivedFile received) {
            this(log, state, progress, received, null, null, null);
        }

        Update(List<String> log, ReceiveState state, SendProgress progress, ReceivedFile received,
               String calibrationStage, ReceivedCalibration calibration) {
            this(log, state, progress, received, calibrationStage, calibration, null);
        }
    }

    private static final String RX_SIDE = "rx";
    private static final double HZ_PER_KHZ = 1000.0;

    private final List<String> fileLog = new ArrayList<>();
    private String currentName = "";
    private boolean bidirectional;
    private LinkChoice arrival;

    public Update read(CliEvent event) {
        return switch (event.name()) {
            case "info" -> log(event.string("message").orElse(""), null);
            case "frame" -> frame(event);
            case "progress" -> new Update(List.of(), ReceiveState.RECEIVING, progress(event), null);
            case "result" -> result(event);
            case "more_timeout" -> log(message("receive.log.moreTimeout"), ReceiveState.LISTENING);
            case "serve" -> log(message("receive.log.serving", event.string("name").orElse("")), ReceiveState.SERVING);
            case "calibrate_stage" -> calibrationStage(event);
            case "calibrate_uni" -> oneWayResult(event);
            case "calibrate_forward" -> forward(event);
            case "calibrate_backward" -> backward(event);
            default -> Update.NONE;
        };
    }

    public List<String> text(String line) {
        fileLog.add(line);
        return List.of(line);
    }

    private Update frame(CliEvent event) {
        event.string("name").ifPresent(name -> currentName = name);
        bidirectional = event.bool("bidirectional").orElse(bidirectional);
        String line = message("receive.log.frame", currentName.isEmpty() ? message("receive.unknownFile") : currentName,
                integer(event, "round"), integer(event, "lost"), integer(event, "blocks"));
        return log(line, ReceiveState.RECEIVING);
    }

    private SendProgress progress(CliEvent event) {
        var fraction = event.number("fraction");
        var speed = event.number("kib_per_s");
        var eta = event.number("eta_s");
        return new SendProgress(event.string("name").orElse(currentName), "",
                fraction.isPresent() ? fraction.getAsDouble() : null,
                speed.isPresent() ? speed.getAsDouble() : null,
                eta.isPresent() ? eta.getAsDouble() : null,
                event.string("status").orElse(""));
    }

    private Update result(CliEvent event) {
        if (!event.string("side").filter(RX_SIDE::equals).isPresent()) {
            return Update.NONE;
        }
        if (event.bool("test").orElse(false)) {
            return log(message("receive.log.test"), ReceiveState.LISTENING);
        }
        if (event.bool("request").orElse(false)) {
            String served = event.string("served").orElse("");
            return log(message("receive.log.served." + served, event.string("name").orElse("")), ReceiveState.LISTENING);
        }
        if (event.bool("verify").orElse(false)) {
            return verification(event);
        }
        boolean ok = event.bool("ok").orElse(false);
        String name = event.string("name").orElse(currentName);
        double seconds = event.number("seconds").orElse(0);
        String memory = event.string("memory").orElse("");
        String line = !memory.isEmpty() ? message("receive.log.memory." + memory, name)
                : message(ok ? "receive.log.ok" : "receive.log.failed", name, SendEventReader.formatTime(seconds));
        fileLog.add(line);
        ReceivedFile received = new ReceivedFile(name, event.string("path").orElse(""),
                (long) event.number("size").orElse(0), seconds, ok, bidirectional, fileLog,
                event.bool("more").orElse(false), memory);
        fileLog.clear();
        currentName = "";
        return new Update(List.of(line), ReceiveState.LISTENING, null, received);
    }

    private Update verification(CliEvent event) {
        List<VerifyItem> items = VerifyItem.fromJson(event.data().get("items"));
        boolean ok = event.bool("ok").orElse(false) && !items.isEmpty();
        String line = ok ? message("receive.log.verified", VerifySummary.text(items)) : message("receive.log.verifyInvalid");
        fileLog.add(line);
        ReceivedVerification verification = new ReceivedVerification(items, event.number("seconds").orElse(0), ok,
                bidirectional, fileLog);
        fileLog.clear();
        currentName = "";
        return new Update(List.of(line), ReceiveState.LISTENING, null, null, null, null, verification);
    }

    private Update calibrationStage(CliEvent event) {
        String stage = event.string("stage").orElse("");
        return new Update(List.of(message("receive.log.calibrationStage", stage)), ReceiveState.CALIBRATING, null, null,
                stage, null);
    }

    private Update oneWayResult(CliEvent event) {
        ReceivedCalibration calibration = new ReceivedCalibration("", event.bool("one_way").orElse(true),
                LinkChoice.fromJson(event.data()), null, null, null);
        arrival = null;
        return new Update(List.of(oneWayCalibration(event)), ReceiveState.LISTENING, null, null, null, calibration);
    }

    private Update forward(CliEvent event) {
        arrival = choice(event);
        String summary = arrival == null ? message("receive.calibration.unknown") : arrival.summary();
        return new Update(List.of(message("receive.log.calibrationForward", summary)), ReceiveState.CALIBRATING, null,
                null, null, null);
    }

    private Update backward(CliEvent event) {
        LinkChoice departure = choice(event);
        ReceivedCalibration calibration = new ReceivedCalibration("", false, arrival, departure,
                event.string("profile").orElse(null), event.string("sent_profile").orElse(null));
        arrival = null;
        String summary = departure == null ? message("receive.calibration.unknown") : departure.summary();
        return new Update(List.of(message("receive.log.calibrationBackward", summary)), ReceiveState.LISTENING, null,
                null, null, calibration);
    }

    private static LinkChoice choice(CliEvent event) {
        JsonElement element = event.data().get("choice");
        return element != null && element.isJsonObject() ? LinkChoice.fromJson(element.getAsJsonObject()) : null;
    }

    private Update log(String line, ReceiveState state) {
        if (line.isEmpty()) {
            return new Update(List.of(), state, null, null);
        }
        fileLog.add(line);
        return new Update(List.of(line), state, null, null);
    }

    private static String oneWayCalibration(CliEvent event) {
        String mode = message(event.bool("mono").orElse(false) ? "calibration.choice.mono" : "calibration.choice.stereo");
        var band = event.number("band_high_hz");
        var peak = event.number("peak");
        var mcs = event.number("mcs");
        if (band.isEmpty() || peak.isEmpty() || mcs.isEmpty()) {
            return message("receive.log.calibrationUnusable");
        }
        return message("receive.log.calibration", mode,
                String.format(Locale.ROOT, "%.0f kHz", band.getAsDouble() / HZ_PER_KHZ),
                String.format(Locale.ROOT, "%s", peak.getAsDouble()), (int) mcs.getAsDouble());
    }

    private static int integer(CliEvent event, String key) {
        return (int) event.number(key).orElse(0);
    }
}
