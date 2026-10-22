package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

public final class ToggleReceiveAction extends AnAction implements DumbAware {

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
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        ReceiveService service = ReceiveService.getInstance(project);
        if (service.state() == ReceiveState.STOPPED) {
            service.start(savedOptions(SoundBridgeProjectSettings.getInstance(project)));
        } else {
            service.stop();
        }
        ToolWindows.show(project, ToolWindows.RECEIVE);
    }

    static ReceiveOptions savedOptions(SoundBridgeProjectSettings settings) {
        boolean bidirectional = settings.receiveBidirectional();
        return new ReceiveOptions(bidirectional, settings.config().receiveDirectory(),
                bidirectional && settings.serveRequests());
    }
}
