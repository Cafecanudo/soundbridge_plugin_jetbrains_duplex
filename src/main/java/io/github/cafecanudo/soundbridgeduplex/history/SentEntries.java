package io.github.cafecanudo.soundbridgeduplex.history;

import io.github.cafecanudo.soundbridgeduplex.send.SendFileResult;
import io.github.cafecanudo.soundbridgeduplex.send.SendJob;
import io.github.cafecanudo.soundbridgeduplex.send.SendOutcome;
import io.github.cafecanudo.soundbridgeduplex.send.SendRequest;
import io.github.cafecanudo.soundbridgeduplex.send.SendSnapshot;
import io.github.cafecanudo.soundbridgeduplex.send.SentFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SentEntries {

    private SentEntries() {
    }

    public static List<HistoryEntry> of(String timestamp, SendRequest request, List<SentFile> files,
                                        SendOutcome outcome, List<String> log) {
        return of(timestamp, request, files, outcome, log, path -> false);
    }

    public static List<HistoryEntry> of(String timestamp, SendRequest request, List<SentFile> files,
                                        SendOutcome outcome, List<String> log, Predicate<Path> isDirectory) {
        if (request.jobs().isEmpty()) {
            return List.of(HistoryEntry.of(timestamp, request.itemLabels(), request.deletedLabels(), request.snapshot(),
                    outcome, log));
        }
        List<HistoryEntry> entries = new ArrayList<>();
        for (int command = 0; command < request.jobs().size(); command++) {
            SendJob job = request.jobs().get(command);
            List<SentFile> results = resultsOf(files, command);
            SendSnapshot snapshot = withDestination(request.snapshot(), job.destination());
            if (!job.delete() && job.items().size() == 1 && isDirectory.test(job.items().get(0))) {
                entries.add(folderEntry(timestamp, job.items().get(0), snapshot, results, outcome));
                continue;
            }
            for (int index = 0; index < job.items().size(); index++) {
                Path item = job.items().get(index);
                SentFile sent = index < results.size() ? results.get(index) : null;
                entries.add(HistoryEntry.ofFile(timestamp, item.toString(), job.delete(), snapshot,
                        sent == null ? null : sent.result(), statusOf(sent, outcome),
                        sent == null ? missingLog(outcome) : sent.log()));
            }
        }
        return entries;
    }

    private static HistoryEntry folderEntry(String timestamp, Path folder, SendSnapshot snapshot, List<SentFile> results,
                                            SendOutcome outcome) {
        List<SendFileResult> files = results.stream().map(SentFile::result).toList();
        double seconds = files.stream().mapToDouble(SendFileResult::seconds).sum();
        String failure = files.isEmpty() ? outcome.failure() : null;
        List<String> log = results.stream().flatMap(sent -> sent.log().stream()).toList();
        SendOutcome folderOutcome = new SendOutcome(files, seconds, outcome.cancelled() && files.isEmpty(), failure);
        return HistoryEntry.of(timestamp, List.of(folder.toString()), snapshot, folderOutcome,
                log.isEmpty() ? missingLog(outcome) : log);
    }

    private static List<SentFile> resultsOf(List<SentFile> files, int command) {
        return files.stream().filter(file -> file.command() == command).toList();
    }

    private static SendSnapshot withDestination(SendSnapshot snapshot, String destination) {
        return new SendSnapshot(snapshot.mode(), destination, snapshot.zipLevel(), snapshot.mcs(),
                snapshot.replaceExisting());
    }

    private static SendStatus statusOf(SentFile sent, SendOutcome outcome) {
        if (sent != null) {
            return sent.result().ok() ? SendStatus.OK : SendStatus.FAILED;
        }
        return outcome.cancelled() ? SendStatus.CANCELLED : SendStatus.FAILED;
    }

    private static List<String> missingLog(SendOutcome outcome) {
        return outcome.failure() == null ? List.of() : List.of(outcome.failure());
    }
}
