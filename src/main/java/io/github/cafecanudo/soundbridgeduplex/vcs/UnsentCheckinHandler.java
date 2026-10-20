package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import io.github.cafecanudo.soundbridgeduplex.send.SendAction;
import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreFilter;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreList;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreService;
import io.github.cafecanudo.soundbridgeduplex.send.SentRegistry;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.util.List;
import java.util.Map;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class UnsentCheckinHandler extends CheckinHandler {

    private final CheckinProjectPanel panel;

    UnsentCheckinHandler(CheckinProjectPanel panel) {
        this.panel = panel;
    }

    @Override
    public ReturnResult beforeCheckin() {
        Project project = panel.getProject();
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        if (!settings.warnUnsentOnCommit() || settings.config().output().isBlank()) {
            return ReturnResult.COMMIT;
        }
        List<Change> changes = List.copyOf(panel.getSelectedChanges());
        Map<String, String> sent = settings.sent();
        IgnoreList ignore = IgnoreService.getInstance(project).list();
        List<Change> unsent = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                () -> unsent(changes, sent, ignore), message("commit.unsent.checking"), false, project);
        if (unsent.isEmpty()) {
            return ReturnResult.COMMIT;
        }
        List<Change> all = ProjectChanges.all(project, changes);
        ChangesSendDialog dialog = new ChangesSendDialog(project, ChangesSendDialog.Mode.COMMIT, all,
                ProjectChanges.matching(all, unsent));
        dialog.show();
        if (dialog.dontShowAgain() && dialog.getExitCode() != ChangesSendDialog.CANCEL_EXIT_CODE) {
            settings.setWarnUnsentOnCommit(false);
        }
        if (dialog.isOK()) {
            SendAction.send(project, dialog.selection());
            return ReturnResult.COMMIT;
        }
        return dialog.getExitCode() == ChangesSendDialog.COMMIT_ANYWAY_EXIT_CODE ? ReturnResult.COMMIT
                : ReturnResult.CANCEL;
    }

    private static List<Change> unsent(List<Change> changes, Map<String, String> sent, IgnoreList ignore) {
        return changes.stream()
                .filter(change -> !SentRegistry.unsent(IgnoreFilter.apply(
                        SendSelection.fromChanges(ChangePaths.of(List.of(change)), List.of()), ignore,
                        path -> false).kept(), sent).isEmpty())
                .toList();
    }
}
