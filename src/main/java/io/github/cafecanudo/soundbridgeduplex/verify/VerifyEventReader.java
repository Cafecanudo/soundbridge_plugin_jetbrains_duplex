package io.github.cafecanudo.soundbridgeduplex.verify;

import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;

import java.util.List;
import java.util.Set;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class VerifyEventReader {

    public static final String REQUESTING = "requesting";
    public static final String REQUESTED = "requested";
    public static final String ANSWERED = "answered";
    public static final String NO_REPLY = "no_reply";
    public static final String FAILED = "failed";

    public record Batch(String status, boolean ok, List<VerifyItem> items) {

        public Batch {
            items = List.copyOf(items);
        }
    }

    public record Update(List<String> log, SendProgress progress, Batch batch) {

        static final Update NONE = new Update(List.of(), null, null);
    }

    private static final String SIDE = "side";
    private static final String TX_SIDE = "tx";
    private static final String VERIFY = "verify";
    private static final String STATUS = "status";
    private static final String ITEMS = "items";
    private static final Set<String> KNOWN_RESULTS = Set.of(REQUESTED, ANSWERED, NO_REPLY, FAILED);

    private final SendEventReader delegate = new SendEventReader();

    public Update read(CliEvent event) {
        return switch (event.name()) {
            case VERIFY -> started(event);
            case "result" -> result(event);
            default -> delegated(delegate.read(event));
        };
    }

    private static Update started(CliEvent event) {
        String status = event.string(STATUS).orElse("");
        String line = message(REQUESTING.equals(status) ? "verify.log.requesting" : "verify.log.requested",
                (int) event.number("files").orElse(0));
        return new Update(List.of(line), null, null);
    }

    private static Update result(CliEvent event) {
        if (!event.string(SIDE).filter(TX_SIDE::equals).isPresent() || !event.bool(VERIFY).orElse(false)) {
            return Update.NONE;
        }
        String status = event.string(STATUS).filter(KNOWN_RESULTS::contains).orElse(FAILED);
        List<VerifyItem> items = VerifyItem.fromJson(event.data().get(ITEMS));
        String line = ANSWERED.equals(status) ? message("verify.log.answered", VerifySummary.text(items))
                : message("verify.log.result." + status);
        return new Update(List.of(line), null, new Batch(status, event.bool("ok").orElse(false), items));
    }

    private static Update delegated(SendEventReader.Update update) {
        return new Update(update.log(), update.progress(), null);
    }
}
