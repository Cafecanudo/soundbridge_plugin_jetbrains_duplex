package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreService;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class VerifyAction extends AnAction implements DumbAware {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null
                && !SendAction.localFiles(event).isEmpty());
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        List<Path> items = SendAction.localFiles(event);
        if (project == null || items.isEmpty()) {
            return;
        }
        verify(project, SendSelection.of(items));
    }

    public static void verify(Project project, SendSelection selection) {
        if (!SoundBridgeProjectSettings.getInstance(project).showWindowOnSend()) {
            SendDraft.getInstance(project).setSelection(selection);
            quickVerify(project, selection.sends());
            return;
        }
        SendDraft.getInstance(project).selectForVerify(selection);
        SoundBridgeToolWindowFactory.show(project);
    }

    private static void quickVerify(Project project, List<Path> items) {
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        ProjectConfig config = settings.config();
        Optional<SendMode> mode = SendMode.preferred(config,
                SendMode.calibrated(config, settings.uniProfile(), settings.calibrationProfile()), settings.sendMode());
        SendService service = SendService.getInstance(project);
        if (mode.isEmpty()) {
            service.notifyUser(message("verify.notify.notStarted"), NotificationType.WARNING);
            return;
        }
        UniProfile parameters = SendCommandBuilder.txParameters(mode.get().applyTo(config), settings.uniProfile(),
                settings.calibrationProfile());
        SendOptions options = new SendOptions("", settings.zipLevel(), McsSelection.of(parameters.mcs()), false);
        service.startVerify(VerifyRequest.of(settings, IgnoreService.getInstance(project).list(), mode.get(),
                options, items));
    }
}
