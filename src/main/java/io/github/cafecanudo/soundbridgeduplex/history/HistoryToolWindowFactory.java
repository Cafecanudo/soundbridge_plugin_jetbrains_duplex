package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;
import io.github.cafecanudo.soundbridgeduplex.send.SendDraft;

public final class HistoryToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void init(ToolWindow toolWindow) {
        toolWindow.setShowStripeButton(true);
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        HistoryPanel panel = new HistoryPanel(project, entry -> {
            SendDraft.getInstance(project).resend(entry);
            ToolWindows.show(project, ToolWindows.SEND);
        });
        ToolWindows.install(toolWindow, panel.component(), panel::dispose);
    }
}
