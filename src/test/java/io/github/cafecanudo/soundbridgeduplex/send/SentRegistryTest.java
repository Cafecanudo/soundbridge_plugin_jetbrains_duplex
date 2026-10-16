package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentRegistryTest {

    @TempDir
    Path folder;

    @Test
    void fingerprintHashesFilesInsideFoldersAndMarksDeletions() throws IOException {
        Path docs = Files.createDirectories(folder.resolve("docs"));
        Path inner = Files.writeString(docs.resolve("a.txt"), "a");
        Path gone = folder.resolve("old.txt");

        Map<String, String> sent = SentRegistry.fingerprint(new SendJob(List.of(docs), "docs"));
        Map<String, String> deleted = SentRegistry.fingerprint(new SendJob(List.of(gone), "", true));

        assertEquals(Map.of(SentRegistry.key(inner), SentRegistry.state(inner).orElseThrow()), sent);
        assertEquals(Map.of(SentRegistry.key(gone), SentRegistry.DELETED), deleted);
    }

    @Test
    void unsentListsNeverSentChangedAndDeletionsNotRecorded() throws IOException {
        Path same = Files.writeString(folder.resolve("same.txt"), "v1");
        Path changed = Files.writeString(folder.resolve("changed.txt"), "v1");
        Path never = Files.writeString(folder.resolve("never.txt"), "v1");
        Path removed = folder.resolve("removed.txt");
        Path removedUnsent = folder.resolve("removed2.txt");
        Map<String, String> sent = new HashMap<>(SentRegistry.fingerprint(new SendJob(List.of(same, changed), "")));
        sent.putAll(SentRegistry.fingerprint(new SendJob(List.of(removed), "", true)));
        Files.writeString(changed, "v2");

        SendSelection unsent = SentRegistry.unsent(
                new SendSelection(List.of(same, changed, never), List.of(removed, removedUnsent)), sent);

        assertEquals(List.of(changed, never), unsent.sends());
        assertEquals(List.of(removedUnsent), unsent.deletions());
    }

    @Test
    void allSentMeansNothingToWarn() throws IOException {
        Path file = Files.writeString(folder.resolve("a.txt"), "a");

        SendSelection unsent = SentRegistry.unsent(SendSelection.of(List.of(file)),
                SentRegistry.fingerprint(new SendJob(List.of(file), "")));

        assertTrue(unsent.isEmpty());
    }
}
