package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;

public final class SoundBridgeToolWindowFactory implements ToolWindowFactory, DumbAware {

    public static void show(Project project) {
        ToolWindows.show(project, ToolWindows.SEND);
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        SendPanel sendPanel = new SendPanel(project);
        ToolWindows.install(toolWindow, new JBScrollPane(sendPanel.component()), sendPanel::dispose);
    }
}
