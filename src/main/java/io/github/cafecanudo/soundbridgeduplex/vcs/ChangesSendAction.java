package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.cafecanudo.soundbridgeduplex.send.PathChange;
import io.github.cafecanudo.soundbridgeduplex.send.SendAction;
import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class ChangesSendAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null && !selection(event).isEmpty());
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        SendSelection selection = selection(event);
        if (project != null && !selection.isEmpty()) {
            SendAction.send(project, selection);
        }
    }

    private static SendSelection selection(AnActionEvent event) {
        Change[] changes = event.getData(VcsDataKeys.CHANGES);
        List<PathChange> paths = changes == null ? List.of()
                : Arrays.stream(changes).map(ChangesSendAction::paths).toList();
        return SendSelection.fromChanges(paths, localFiles(event));
    }

    private static PathChange paths(Change change) {
        return new PathChange(path(change.getBeforeRevision()), path(change.getAfterRevision()));
    }

    private static Path path(ContentRevision revision) {
        return revision == null ? null : Path.of(revision.getFile().getPath());
    }

    private static List<Path> localFiles(AnActionEvent event) {
        VirtualFile[] files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files)
                .filter(VirtualFile::isInLocalFileSystem)
                .map(VirtualFile::toNioPath)
                .toList();
    }
}
