package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import io.github.cafecanudo.soundbridgeduplex.send.SendAction;
import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;

import java.nio.file.Path;
import java.util.ArrayList;
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
        if (project == null || selection(event).isEmpty()) {
            return;
        }
        List<Change> selected = new ArrayList<>(selectedChanges(event));
        selected.addAll(unversioned(project, event));
        List<Change> all = ProjectChanges.all(project, selected);
        ChangesSendDialog dialog = new ChangesSendDialog(project, ChangesSendDialog.Mode.MENU, all,
                ProjectChanges.matching(all, selected));
        if (dialog.showAndGet() && !dialog.selection().isEmpty()) {
            SendAction.send(project, dialog.selection());
        }
    }

    private static SendSelection selection(AnActionEvent event) {
        return SendSelection.fromChanges(ChangePaths.of(selectedChanges(event)), localFiles(event));
    }

    private static List<Change> selectedChanges(AnActionEvent event) {
        Change[] changes = event.getData(VcsDataKeys.CHANGES);
        return changes == null ? List.of() : Arrays.asList(changes);
    }

    private static List<Change> unversioned(Project project, AnActionEvent event) {
        VirtualFile[] files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null) {
            return List.of();
        }
        ChangeListManager manager = ChangeListManager.getInstance(project);
        return Arrays.stream(files)
                .filter(VirtualFile::isInLocalFileSystem)
                .filter(file -> manager.getChange(file) == null)
                .map(file -> new Change(null, CurrentContentRevision.create(VcsUtil.getFilePath(file))))
                .toList();
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
