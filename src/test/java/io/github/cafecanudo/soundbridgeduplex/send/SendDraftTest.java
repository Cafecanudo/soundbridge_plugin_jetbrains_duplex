package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendDraftTest {

    private static final List<Path> ITEMS = List.of(Path.of("C:/p/a.txt"));

    @Test
    void menuSelectionRequestsAutoSendOnce() {
        SendDraft draft = new SendDraft();

        draft.selectFromMenu(ITEMS);

        assertTrue(draft.consumeAutoSendRequest());
        assertFalse(draft.consumeAutoSendRequest());
    }

    @Test
    void manualChangeDoesNotRequestAutoSend() {
        SendDraft draft = new SendDraft();
        draft.selectFromMenu(ITEMS);

        draft.setItems(List.of());

        assertFalse(draft.consumeAutoSendRequest());
    }

    @Test
    void listenersSeeTheRequestBeforeItIsConsumed() {
        SendDraft draft = new SendDraft();
        List<Boolean> seen = new ArrayList<>();
        draft.addListener(items -> seen.add(draft.consumeAutoSendRequest()));

        draft.selectFromMenu(ITEMS);

        assertEquals(List.of(true), seen);
        assertEquals(ITEMS, draft.items());
    }
}
