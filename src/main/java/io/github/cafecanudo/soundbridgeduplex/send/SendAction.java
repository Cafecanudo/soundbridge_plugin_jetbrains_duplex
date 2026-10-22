package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.cafecanudo.soundbridgeduplex.memory.MemoryFiles;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.io.IOException;
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
        event.getPresentation().setEnabledAndVisible(event.getProject() != null
                && (selectedText(event) != null || !localFiles(event).isEmpty()));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        String text = selectedText(event);
        if (text != null) {
            sendText(project, text);
            return;
        }
        List<Path> items = localFiles(event);
        if (!items.isEmpty()) {
            send(project, SendSelection.of(items));
        }
    }

    public static void sendText(Project project, String text) {
        try {
            Path file = MemoryFiles.writeText(MemoryFiles.defaultRoot(), text);
            SoundBridgeProjectSettings.getInstance(project).setSendCopyMemory(true);
            send(project, SendSelection.of(List.of(file)));
        } catch (IOException e) {
            SendService.getInstance(project).notifyUser(e.getMessage(), NotificationType.WARNING);
        }
    }

    static String selectedText(AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null || !editor.getSelectionModel().hasSelection()) {
            return null;
        }
        String text = editor.getSelectionModel().getSelectedText();
        return text == null || text.isEmpty() ? null : text;
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
            VirtualFile current = event.getData(CommonDataKeys.VIRTUAL_FILE);
            files = current == null ? new VirtualFile[0] : new VirtualFile[]{current};
        }
        return Arrays.stream(files)
                .filter(VirtualFile::isInLocalFileSystem)
                .map(VirtualFile::toNioPath)
                .toList();
    }
}
