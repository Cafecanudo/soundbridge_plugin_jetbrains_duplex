package io.github.cafecanudo.soundbridgeduplex.verify;

import io.github.cafecanudo.soundbridgeduplex.history.SendStatus;

import java.util.List;
import java.util.Objects;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public record VerifyOutcome(SendStatus status, List<VerifyItem> items, int files, double seconds, String failure) {

    public VerifyOutcome {
        items = List.copyOf(items);
        failure = Objects.requireNonNullElse(failure, "");
    }

    public static VerifyOutcome of(List<VerifyEventReader.Batch> batches, int commands, int files, double seconds,
                                   boolean cancelled, String failure) {
        List<VerifyItem> items = batches.stream().flatMap(batch -> batch.items().stream()).toList();
        return new VerifyOutcome(status(batches, commands, cancelled, failure), items, files, seconds, failure);
    }

    public boolean answered() {
        return status == SendStatus.OK;
    }

    public boolean succeeded() {
        return status == SendStatus.OK || status == SendStatus.REQUESTED;
    }

    public String summary() {
        return switch (status) {
            case OK -> VerifySummary.text(items);
            case REQUESTED -> message("verify.notify.requested", files);
            case NO_REPLY -> message("verify.notify.noReply");
            case CANCELLED -> message("verify.notify.cancelled");
            default -> message("verify.notify.failed", failure);
        };
    }

    private static SendStatus status(List<VerifyEventReader.Batch> batches, int commands, boolean cancelled,
                                     String failure) {
        if (cancelled) {
            return SendStatus.CANCELLED;
        }
        if (batches.stream().anyMatch(batch -> VerifyEventReader.NO_REPLY.equals(batch.status()))) {
            return SendStatus.NO_REPLY;
        }
        if (failure != null || batches.size() < commands || batches.isEmpty()) {
            return SendStatus.FAILED;
        }
        if (batches.stream().allMatch(batch -> VerifyEventReader.ANSWERED.equals(batch.status()))) {
            return SendStatus.OK;
        }
        if (batches.stream().allMatch(batch -> VerifyEventReader.REQUESTED.equals(batch.status()))) {
            return SendStatus.REQUESTED;
        }
        return SendStatus.FAILED;
    }
}
