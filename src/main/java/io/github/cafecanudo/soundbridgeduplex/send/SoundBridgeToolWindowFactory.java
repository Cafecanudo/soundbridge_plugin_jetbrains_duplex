package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public final class SoundBridgeToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        SendPanel panel = new SendPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel.component(), "", false);
        Disposer.register(content, panel::dispose);
        toolWindow.getContentManager().addContent(content);
    }
}
