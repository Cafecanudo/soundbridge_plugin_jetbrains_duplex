package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendSelectionTest {

    private static final Path MODIFIED = Path.of("C:/p/src/a.java");
    private static final Path DELETED = Path.of("C:/p/src/old.java");
    private static final Path RENAMED_FROM = Path.of("C:/p/src/b.java");
    private static final Path RENAMED_TO = Path.of("C:/p/lib/b.java");
    private static final Path UNVERSIONED = Path.of("C:/p/new.txt");

    @Test
    void classifiesModifiedDeletedRenamedAndUnversioned() {
        SendSelection selection = SendSelection.fromChanges(List.of(
                new PathChange(MODIFIED, MODIFIED),
                new PathChange(DELETED, null),
                new PathChange(RENAMED_FROM, RENAMED_TO),
                new PathChange(null, UNVERSIONED)), List.of(MODIFIED, UNVERSIONED));

        assertEquals(List.of(MODIFIED, RENAMED_TO, UNVERSIONED), selection.sends());
        assertEquals(List.of(DELETED, RENAMED_FROM), selection.deletions());
        assertTrue(selection.hasDeletions());
    }

    @Test
    void caseOnlyRenameDeletesTheOldName() {
        Path upper = Path.of("C:/p/src/Main.java");
        Path lower = Path.of("C:/p/src/main.java");

        SendSelection selection = SendSelection.fromChanges(List.of(new PathChange(upper, lower)), List.of());

        assertEquals(List.of(lower), selection.sends());
        assertEquals(List.of(upper), selection.deletions());
    }

    @Test
    void plainFilesHaveNoDeletions() {
        SendSelection selection = SendSelection.fromChanges(List.of(), List.of(UNVERSIONED));

        assertEquals(SendSelection.of(List.of(UNVERSIONED)), selection);
        assertFalse(selection.hasDeletions());
        assertTrue(SendSelection.EMPTY.isEmpty());
    }
}
