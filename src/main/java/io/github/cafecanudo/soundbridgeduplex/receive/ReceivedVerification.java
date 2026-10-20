package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.verify.VerifyItem;

import java.util.List;

public record ReceivedVerification(List<VerifyItem> items, double seconds, boolean ok, boolean bidirectional,
                                   List<String> log) {

    public ReceivedVerification {
        items = List.copyOf(items);
        log = List.copyOf(log);
    }
}
