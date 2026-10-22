package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;

import java.util.ArrayList;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceiveSequence {

    public record Notice(String text, boolean warning) {
    }

    private int received;
    private int failed;
    private double seconds;
    private ReceivedFile last;
    private boolean open;

    public List<Notice> add(ReceivedFile file) {
        if (!open) {
            received = 0;
            failed = 0;
            seconds = 0;
            open = true;
        }
        if (file.ok()) {
            received++;
        } else {
            failed++;
        }
        seconds += file.seconds();
        last = file;
        List<Notice> notices = new ArrayList<>();
        if (!file.ok()) {
            notices.add(new Notice(message("receive.notify.incomplete", file.name(),
                    SendEventReader.formatTime(file.seconds())), true));
        }
        if (!file.more()) {
            notices.addAll(end());
        }
        return notices;
    }

    public List<Notice> end() {
        if (!open) {
            return List.of();
        }
        open = false;
        int total = received + failed;
        if (total == 1 && failed == 0) {
            String text = last.inMemory() ? message("receive.notify.memory." + last.memory(), last.name())
                    : message("receive.notify.received", last.name(), SendEventReader.formatTime(last.seconds()));
            return List.of(new Notice(text, false));
        }
        if (total > 1) {
            return List.of(new Notice(message("receive.notify.sequence", received, failed,
                    SendEventReader.formatTime(seconds)), failed > 0));
        }
        return List.of();
    }

    public int received() {
        return received;
    }

    public int failed() {
        return failed;
    }

    public boolean isOpen() {
        return open;
    }
}
