package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import io.github.cafecanudo.soundbridgeduplex.send.PathChange;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

final class ChangePaths {

    private ChangePaths() {
    }

    static List<PathChange> of(Collection<Change> changes) {
        return changes.stream()
                .map(change -> new PathChange(path(change.getBeforeRevision()), path(change.getAfterRevision())))
                .toList();
    }

    private static Path path(ContentRevision revision) {
        return revision == null ? null : Path.of(revision.getFile().getPath());
    }
}
