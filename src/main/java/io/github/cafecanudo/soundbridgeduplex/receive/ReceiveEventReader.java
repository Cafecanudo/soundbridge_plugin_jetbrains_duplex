package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceiveEventReader {

    public record Update(List<String> log, ReceiveState state, SendProgress progress, ReceivedFile received) {

        static final Update NONE = new Update(List.of(), null, null, null);
    }

    private static final String RX_SIDE = "rx";
    private static final double HZ_PER_KHZ = 1000.0;

    private final List<String> fileLog = new ArrayList<>();
    private String currentName = "";
    private boolean bidirectional;

    public Update read(CliEvent event) {
        return switch (event.name()) {
            case "info" -> log(event.string("message").orElse(""), null);
            case "frame" -> frame(event);
            case "progress" -> new Update(List.of(), ReceiveState.RECEIVING, progress(event), null);
            case "result" -> result(event);
            case "more_timeout" -> log(message("receive.log.moreTimeout"), ReceiveState.LISTENING);
            case "calibrate_uni" -> log(oneWayCalibration(event), null);
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
        boolean ok = event.bool("ok").orElse(false);
        String name = event.string("name").orElse(currentName);
        double seconds = event.number("seconds").orElse(0);
        String line = message(ok ? "receive.log.ok" : "receive.log.failed", name, SendEventReader.formatTime(seconds));
        fileLog.add(line);
        ReceivedFile received = new ReceivedFile(name, event.string("path").orElse(""),
                (long) event.number("size").orElse(0), seconds, ok, bidirectional, fileLog);
        fileLog.clear();
        currentName = "";
        return new Update(List.of(line), ReceiveState.LISTENING, null, received);
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
