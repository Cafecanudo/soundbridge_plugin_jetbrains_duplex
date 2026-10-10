package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;

public final class ReceiveToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ReceivePanel panel = new ReceivePanel(project);
        ToolWindows.install(toolWindow, new JBScrollPane(panel.component()), panel::dispose);
    }
}
