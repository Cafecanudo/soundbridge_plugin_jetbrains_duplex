package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class SendAction extends AnAction implements DumbAware {

    public static final String TOOL_WINDOW_ID = "SoundBridge";

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
        if (!SoundBridgeProjectSettings.getInstance(project).showWindowOnSend()) {
            SendDraft.getInstance(project).setItems(items);
            QuickSend.run(project, items);
            return;
        }
        SendDraft.getInstance(project).selectFromMenu(items);
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (window != null) {
            window.activate(null);
        }
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
