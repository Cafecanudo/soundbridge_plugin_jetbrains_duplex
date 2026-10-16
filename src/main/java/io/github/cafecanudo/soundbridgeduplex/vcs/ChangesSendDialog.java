package io.github.cafecanudo.soundbridgeduplex.vcs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.SimpleAsyncChangesBrowser;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class ChangesSendDialog extends DialogWrapper {

    static final int COMMIT_ANYWAY_EXIT_CODE = NEXT_USER_EXIT_CODE;

    private static final int WIDTH = 640;
    private static final int HEIGHT = 420;
    private static final int GAP = 8;

    enum Mode { MENU, COMMIT }

    private final Mode mode;
    private final SimpleAsyncChangesBrowser browser;
    private final JBLabel header = new JBLabel();
    private final List<Change> initial;
    private boolean touched;

    ChangesSendDialog(Project project, Mode mode, List<Change> changes, Collection<Change> included) {
        super(project, true);
        this.mode = mode;
        this.initial = List.copyOf(included);
        browser = new SimpleAsyncChangesBrowser(project, true, false);
        browser.setChangesToDisplay(changes);
        browser.setIncludedChanges(included);
        browser.setInclusionChangedListener(this::inclusionChanged);
        setTitle(message(mode == Mode.COMMIT ? "changes.dialog.title.commit" : "changes.dialog.title.menu"));
        setOKButtonText(message(mode == Mode.COMMIT ? "changes.dialog.sendAndCommit" : "changes.dialog.send"));
        init();
        updateState();
    }

    SendSelection selection() {
        return SendSelection.fromChanges(ChangePaths.of(included()), List.of());
    }

    @Override
    protected JComponent createCenterPanel() {
        browser.setPreferredSize(JBUI.size(WIDTH, HEIGHT));
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(GAP)));
        panel.add(header, BorderLayout.NORTH);
        panel.add(browser, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void dispose() {
        browser.shutdown();
        super.dispose();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return browser.getPreferredFocusedComponent();
    }

    @Override
    protected Action[] createActions() {
        if (mode == Mode.MENU) {
            return new Action[]{getOKAction(), getCancelAction()};
        }
        return new Action[]{getOKAction(), new CommitAnywayAction(), getCancelAction()};
    }

    private void inclusionChanged() {
        touched = true;
        updateState();
    }

    private List<Change> included() {
        return touched ? browser.getIncludedChanges() : initial;
    }

    private void updateState() {
        int count = included().size();
        header.setText(message(mode == Mode.COMMIT ? "changes.dialog.header.commit" : "changes.dialog.header.menu",
                count));
        setOKActionEnabled(count > 0);
    }

    private final class CommitAnywayAction extends DialogWrapperAction {

        private CommitAnywayAction() {
            super(message("commit.unsent.commit"));
        }

        @Override
        protected void doAction(ActionEvent event) {
            close(COMMIT_ANYWAY_EXIT_CODE);
        }
    }
}
