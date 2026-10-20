package io.github.cafecanudo.soundbridgeduplex.ignore;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class IgnoreAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        Project project = event.getProject();
        List<Path> items = localFiles(event);
        boolean visible = project != null && !items.isEmpty();
        event.getPresentation().setEnabledAndVisible(visible);
        if (!visible) {
            return;
        }
        IgnoreList list = IgnoreService.getInstance(project).list();
        if (items.stream().allMatch(list::isEntry)) {
            event.getPresentation().setText(message("ignore.action.remove"));
        } else if (items.stream().allMatch(list::isIgnored)) {
            event.getPresentation().setText(message("ignore.action.byParent"));
            event.getPresentation().setEnabled(false);
        } else {
            event.getPresentation().setText(message("ignore.action.add"));
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        List<Path> items = localFiles(event);
        if (project == null || items.isEmpty()) {
            return;
        }
        IgnoreService service = IgnoreService.getInstance(project);
        if (items.stream().allMatch(service.list()::isEntry)) {
            service.unignorePaths(items);
        } else {
            service.ignore(items);
        }
    }

    private static List<Path> localFiles(AnActionEvent event) {
        VirtualFile[] files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null) {
            VirtualFile current = event.getData(CommonDataKeys.VIRTUAL_FILE);
            files = current == null ? new VirtualFile[0] : new VirtualFile[]{current};
        }
        return Arrays.stream(files)
                .filter(VirtualFile::isInLocalFileSystem)
                .map(VirtualFile::toNioPath)
                .toList();
    }
}
