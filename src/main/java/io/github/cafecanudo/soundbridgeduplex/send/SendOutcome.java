package io.github.cafecanudo.soundbridgeduplex.send;

import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public record SendOutcome(List<SendFileResult> files, double seconds, boolean cancelled, String failure) {

    public SendOutcome {
        files = List.copyOf(files);
    }

    public long okCount() {
        return files.stream().filter(SendFileResult::ok).count();
    }

    public boolean succeeded() {
        return !cancelled && failure == null && !files.isEmpty() && okCount() == files.size();
    }

    public String summary() {
        String time = SendEventReader.formatTime(seconds);
        if (cancelled) {
            return message("send.notify.cancelled", okCount(), files.size(), time);
        }
        if (failure != null) {
            return message("send.notify.failed", okCount(), files.size(), failure);
        }
        return message(succeeded() ? "send.notify.done" : "send.notify.partial", okCount(), files.size(), time);
    }
}
