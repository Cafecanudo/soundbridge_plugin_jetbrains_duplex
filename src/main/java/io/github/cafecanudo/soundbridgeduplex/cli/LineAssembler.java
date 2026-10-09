package io.github.cafecanudo.soundbridgeduplex.cli;

import java.util.function.Consumer;

public final class LineAssembler {

    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';

    private final StringBuilder pending = new StringBuilder();
    private final Consumer<String> sink;

    public LineAssembler(Consumer<String> sink) {
        this.sink = sink;
    }

    public synchronized void append(String chunk) {
        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);
            if (c == LINE_FEED) {
                emit();
            } else {
                pending.append(c);
            }
        }
    }

    public synchronized void flush() {
        if (!pending.isEmpty()) {
            emit();
        }
    }

    private void emit() {
        int end = pending.length();
        if (end > 0 && pending.charAt(end - 1) == CARRIAGE_RETURN) {
            pending.setLength(end - 1);
        }
        String line = pending.toString();
        pending.setLength(0);
        sink.accept(line);
    }
}
