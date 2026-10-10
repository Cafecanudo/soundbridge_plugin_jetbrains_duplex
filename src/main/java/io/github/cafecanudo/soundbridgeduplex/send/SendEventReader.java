package io.github.cafecanudo.soundbridgeduplex.send;

import com.google.gson.JsonArray;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class SendEventReader {

    public record Update(List<String> log, SendProgress progress, SendFileResult result) {

        static final Update NONE = new Update(List.of(), null, null);

        static Update log(String line) {
            return new Update(List.of(line), null, null);
        }
    }

    private static final Pattern COUNTER = Pattern.compile("^\\[(\\d+/\\d+)]");
    private static final String NAME = "name";
    private static final String MESSAGE = "message";
    private static final String SIDE = "side";
    private static final String TX_SIDE = "tx";
    private static final String FILES = "files";
    private static final String MCS_TRACE = "mcs_trace";
    private static final int SECONDS_PER_MINUTE = 60;

    private String currentFile = "";
    private String counter = "";

    public Update read(CliEvent event) {
        return switch (event.name()) {
            case "info" -> info(event);
            case "progress" -> new Update(List.of(), progress(event), null);
            case "round" -> Update.log(message("send.log.round", integer(event, "round"), integer(event, "mcs"),
                    integer(event, "blocks")));
            case "ack" -> Update.log(message("send.log.ack", integer(event, "lost"), integer(event, "sent"),
                    integer(event, "missing"), integer(event, "total")));
            case "ack_lost" -> Update.log(message("send.log.ackLost", integer(event, "round")));
            case "result" -> result(event);
            default -> Update.NONE;
        };
    }

    public static String formatTime(double seconds) {
        long total = Math.round(seconds);
        return String.format(Locale.ROOT, "%d:%02d", total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE);
    }

    private Update info(CliEvent event) {
        String text = event.string(MESSAGE).orElse("");
        event.string(NAME).ifPresent(name -> {
            currentFile = name;
            Matcher matcher = COUNTER.matcher(text);
            counter = matcher.find() ? matcher.group(1) : "";
        });
        return text.isEmpty() ? Update.NONE : Update.log(text);
    }

    private SendProgress progress(CliEvent event) {
        return new SendProgress(event.string(NAME).orElse(currentFile), counter,
                number(event, "fraction"), number(event, "kib_per_s"), number(event, "eta_s"),
                event.string("status").orElse(""));
    }

    private Update result(CliEvent event) {
        if (!event.string(SIDE).filter(TX_SIDE::equals).isPresent()) {
            return Update.NONE;
        }
        if (event.data().has(FILES)) {
            return Update.log(message("send.log.summary", integer(event, "ok"), integer(event, FILES),
                    formatTime(event.number("seconds").orElse(0))));
        }
        boolean ok = event.bool("ok").orElse(false);
        double seconds = event.number("seconds").orElse(0);
        SendFileResult result = new SendFileResult(currentFile, ok, seconds, (long) event.number("size").orElse(0),
                lastMcs(event));
        String line = message(ok ? "send.log.fileOk" : "send.log.fileFailed", currentFile, formatTime(seconds));
        return new Update(List.of(line), null, result);
    }

    private static Integer lastMcs(CliEvent event) {
        if (!event.data().has(MCS_TRACE) || !event.data().get(MCS_TRACE).isJsonArray()) {
            return null;
        }
        JsonArray trace = event.data().getAsJsonArray(MCS_TRACE);
        return trace.isEmpty() ? null : trace.get(trace.size() - 1).getAsInt();
    }

    private static Double number(CliEvent event, String key) {
        var value = event.number(key);
        return value.isPresent() ? value.getAsDouble() : null;
    }

    private static int integer(CliEvent event, String key) {
        return (int) event.number(key).orElse(0);
    }
}
