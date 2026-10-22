package io.github.cafecanudo.soundbridgeduplex.history;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.cafecanudo.soundbridgeduplex.receive.ReceivedFile;
import io.github.cafecanudo.soundbridgeduplex.receive.ReceivedVerification;
import io.github.cafecanudo.soundbridgeduplex.send.SendFileResult;
import io.github.cafecanudo.soundbridgeduplex.send.SendMode;
import io.github.cafecanudo.soundbridgeduplex.send.SendOutcome;
import io.github.cafecanudo.soundbridgeduplex.send.SendSnapshot;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyItem;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyStatus;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifySummary;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public record HistoryEntry(
        Direction direction,
        String timestamp,
        List<String> items,
        List<String> deleted,
        SendSnapshot snapshot,
        int files,
        int okFiles,
        long size,
        Integer finalMcs,
        double seconds,
        SendStatus status,
        List<String> log,
        String remoteName,
        List<VerifyItem> verified) {

    public static final int MAX_LOG_LINES = 200;

    private static final String DIRECTION = "direction";
    private static final String TIMESTAMP = "timestamp";
    private static final String ITEMS = "items";
    private static final String DELETED = "deleted";
    private static final String MODE = "mode";
    private static final String DESTINATION = "destination";
    private static final String ZIP_LEVEL = "zip_level";
    private static final String MCS = "mcs";
    private static final String REPLACE = "replace";
    private static final String PACKAGE = "package";
    private static final String MEMORY = "memory";
    private static final String FILES = "files";
    private static final String OK_FILES = "ok_files";
    private static final String SIZE = "size";
    private static final String FINAL_MCS = "final_mcs";
    private static final String SECONDS = "seconds";
    private static final String STATUS = "status";
    private static final String LOG = "log";
    private static final String REMOTE_NAME = "remote_name";
    private static final String VERIFIED = "verified";

    public HistoryEntry {
        items = List.copyOf(items);
        deleted = List.copyOf(deleted);
        log = List.copyOf(log.size() > MAX_LOG_LINES ? log.subList(log.size() - MAX_LOG_LINES, log.size()) : log);
        remoteName = Objects.requireNonNullElse(remoteName, "");
        verified = List.copyOf(Objects.requireNonNullElse(verified, List.of()));
    }

    public static HistoryEntry of(String timestamp, List<String> items, SendSnapshot snapshot, SendOutcome outcome,
                                  List<String> log) {
        return of(timestamp, items, List.of(), snapshot, outcome, log);
    }

    public static HistoryEntry of(String timestamp, List<String> items, List<String> deleted, SendSnapshot snapshot,
                                  SendOutcome outcome, List<String> log) {
        List<SendFileResult> results = outcome.files();
        Integer finalMcs = results.isEmpty() ? null : results.get(results.size() - 1).finalMcs();
        long size = results.stream().mapToLong(SendFileResult::size).sum();
        return new HistoryEntry(Direction.SENT, timestamp, items, deleted, snapshot, results.size(), (int) outcome.okCount(),
                size, finalMcs, outcome.seconds(), SendStatus.of(outcome), log, "", List.of());
    }

    public static HistoryEntry ofFile(String timestamp, String item, boolean delete, SendSnapshot snapshot,
                                      SendFileResult result, SendStatus status, List<String> log) {
        boolean ok = result != null && result.ok();
        return new HistoryEntry(Direction.SENT, timestamp, delete ? List.of() : List.of(item),
                delete ? List.of(item) : List.of(), snapshot, 1, ok ? 1 : 0, result == null ? 0 : result.size(),
                result == null ? null : result.finalMcs(), result == null ? 0 : result.seconds(), status, log, "",
                List.of());
    }

    public static HistoryEntry ofReceived(String timestamp, ReceivedFile file) {
        SendSnapshot snapshot = new SendSnapshot(file.bidirectional() ? SendMode.TWO_WAY : SendMode.ONE_WAY, "",
                null, null, false);
        String item = file.path().isEmpty() ? file.name() : file.path();
        return new HistoryEntry(Direction.RECEIVED, timestamp, List.of(item), List.of(), snapshot, 1, file.ok() ? 1 : 0,
                file.size(), null, file.seconds(), file.ok() ? SendStatus.OK : SendStatus.FAILED, file.log(), file.name(),
                List.of());
    }

    public static HistoryEntry ofIgnored(String timestamp, List<String> items, List<String> deleted,
                                         SendSnapshot snapshot) {
        return new HistoryEntry(Direction.SENT, timestamp, items, deleted, snapshot, items.size() + deleted.size(), 0, 0,
                null, 0, SendStatus.IGNORED, List.of(), "", List.of());
    }

    public static HistoryEntry ofVerify(String timestamp, List<String> paths, SendSnapshot snapshot, SendStatus status,
                                        List<VerifyItem> verified, double seconds, List<String> log) {
        return new HistoryEntry(Direction.VERIFY_SENT, timestamp, paths, List.of(), snapshot, paths.size(),
                (int) VerifySummary.count(verified, VerifyStatus.EXISTS), 0, null, seconds, status, log, "", verified);
    }

    public static HistoryEntry ofVerifyReceived(String timestamp, ReceivedVerification verification) {
        SendSnapshot snapshot = new SendSnapshot(verification.bidirectional() ? SendMode.TWO_WAY : SendMode.ONE_WAY, "",
                null, null, false);
        List<String> paths = verification.items().stream().map(VerifyItem::path).toList();
        SendStatus status = verification.ok() ? SendStatus.OK : SendStatus.FAILED;
        return new HistoryEntry(Direction.VERIFY_RECEIVED, timestamp, paths, List.of(), snapshot, paths.size(),
                (int) VerifySummary.count(verification.items(), VerifyStatus.EXISTS), 0, null, verification.seconds(),
                status, verification.log(), "", verification.items());
    }

    public String toJson() {
        JsonObject object = new JsonObject();
        object.addProperty(DIRECTION, direction.name());
        object.addProperty(TIMESTAMP, timestamp);
        object.add(ITEMS, strings(items));
        object.add(DELETED, strings(deleted));
        object.addProperty(MODE, snapshot.mode().name());
        object.addProperty(DESTINATION, snapshot.destination());
        object.add(ZIP_LEVEL, number(snapshot.zipLevel()));
        object.add(MCS, number(snapshot.mcs()));
        object.addProperty(REPLACE, snapshot.replaceExisting());
        object.addProperty(PACKAGE, snapshot.packageFolders());
        object.addProperty(MEMORY, snapshot.copyMemory());
        object.addProperty(FILES, files);
        object.addProperty(OK_FILES, okFiles);
        object.addProperty(SIZE, size);
        object.add(FINAL_MCS, number(finalMcs));
        object.addProperty(SECONDS, seconds);
        object.addProperty(STATUS, status.name());
        object.add(LOG, strings(log));
        object.addProperty(REMOTE_NAME, remoteName);
        if (!verified.isEmpty()) {
            object.add(VERIFIED, VerifyItem.toJson(verified));
        }
        return object.toString();
    }

    public static Optional<HistoryEntry> fromJson(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            SendSnapshot snapshot = new SendSnapshot(SendMode.valueOf(o.get(MODE).getAsString()),
                    o.get(DESTINATION).getAsString(), integer(o, ZIP_LEVEL), integer(o, MCS),
                    o.get(REPLACE).getAsBoolean(), flag(o, PACKAGE), flag(o, MEMORY));
            Direction direction = o.has(DIRECTION) ? Direction.valueOf(o.get(DIRECTION).getAsString()) : Direction.SENT;
            List<String> deleted = o.has(DELETED) ? strings(o.getAsJsonArray(DELETED)) : List.of();
            return Optional.of(new HistoryEntry(direction, o.get(TIMESTAMP).getAsString(), strings(o.getAsJsonArray(ITEMS)),
                    deleted, snapshot, o.get(FILES).getAsInt(), o.get(OK_FILES).getAsInt(), o.get(SIZE).getAsLong(),
                    integer(o, FINAL_MCS), o.get(SECONDS).getAsDouble(), SendStatus.valueOf(o.get(STATUS).getAsString()),
                    strings(o.getAsJsonArray(LOG)), o.has(REMOTE_NAME) ? o.get(REMOTE_NAME).getAsString() : "",
                    VerifyItem.fromJson(o.get(VERIFIED))));
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static List<String> strings(JsonArray array) {
        return StreamSupport.stream(Objects.requireNonNull(array).spliterator(), false)
                .map(JsonElement::getAsString)
                .toList();
    }

    private static JsonElement number(Number value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    private static Integer integer(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsInt();
    }

    private static boolean flag(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }
}
