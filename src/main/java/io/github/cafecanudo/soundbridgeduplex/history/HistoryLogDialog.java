package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.Dimension;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class HistoryLogDialog extends DialogWrapper {

    private static final int WIDTH = 720;
    private static final int HEIGHT = 420;

    private final HistoryEntry entry;

    HistoryLogDialog(Project project, HistoryEntry entry) {
        super(project, false);
        this.entry = entry;
        setTitle(message("history.log.title", HistoryFormat.date(entry.timestamp()), HistoryFormat.items(entry.items(), entry.deleted())));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JBTextArea area = new JBTextArea(String.join("\n", entry.log()));
        area.setEditable(false);
        JBScrollPane scroll = new JBScrollPane(area);
        scroll.setPreferredSize(new Dimension(JBUI.scale(WIDTH), JBUI.scale(HEIGHT)));
        return scroll;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}
