package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class SendAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null && !localFiles(event).isEmpty());
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        List<Path> items = localFiles(event);
        if (project == null || items.isEmpty()) {
            return;
        }
        send(project, SendSelection.of(items));
    }

    public static void send(Project project, SendSelection selection) {
        if (!SoundBridgeProjectSettings.getInstance(project).showWindowOnSend()) {
            SendDraft.getInstance(project).setSelection(selection);
            QuickSend.run(project, selection);
            return;
        }
        SendDraft.getInstance(project).selectFromMenu(selection);
        SoundBridgeToolWindowFactory.show(project);
    }

    static List<Path> localFiles(AnActionEvent event) {
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
