package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class ProjectChanges {

    private ProjectChanges() {
    }

    static List<Change> all(Project project, Collection<Change> extra) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        Map<String, Change> changes = new LinkedHashMap<>();
        manager.getAllChanges().forEach(change -> changes.putIfAbsent(key(change), change));
        manager.getUnversionedFilesPaths().stream()
                .map(path -> new Change(null, CurrentContentRevision.create(path)))
                .forEach(change -> changes.putIfAbsent(key(change), change));
        extra.forEach(change -> changes.putIfAbsent(key(change), change));
        return new ArrayList<>(changes.values());
    }

    static List<Change> matching(List<Change> all, Collection<Change> wanted) {
        Set<String> keys = wanted.stream().map(ProjectChanges::key).collect(Collectors.toSet());
        return all.stream().filter(change -> keys.contains(key(change))).toList();
    }

    private static String key(Change change) {
        ContentRevision revision = change.getAfterRevision() != null ? change.getAfterRevision()
                : change.getBeforeRevision();
        return revision == null ? "" : revision.getFile().getPath();
    }
}
