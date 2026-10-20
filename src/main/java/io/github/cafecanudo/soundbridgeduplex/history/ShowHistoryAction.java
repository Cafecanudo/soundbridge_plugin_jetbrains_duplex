package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;

public final class ShowHistoryAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        if (event.getProject() != null) {
            ToolWindows.show(event.getProject(), ToolWindows.HISTORY);
        }
    }
}
