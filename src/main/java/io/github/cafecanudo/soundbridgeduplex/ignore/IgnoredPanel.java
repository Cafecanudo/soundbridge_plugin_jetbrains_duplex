package io.github.cafecanudo.soundbridgeduplex.ignore;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.nio.file.Files;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class IgnoredPanel {

    private static final int GAP = 6;
    private static final String FOLDER_SUFFIX = "/";

    private final Project project;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JBList<String> list = new JBList<>(model);
    private final JButton removeButton = new JButton(message("ignore.panel.remove"));
    private final JButton clearButton = new JButton(message("ignore.panel.clear"));
    private final Runnable listener = this::reload;
    private final JPanel root;

    public IgnoredPanel(Project project) {
        this.project = project;
        list.getEmptyText().setText(message("ignore.panel.empty"));
        list.setCellRenderer(SimpleListCellRenderer.create((label, entry, index) -> label.setText(label(entry))));
        list.addListSelectionListener(e -> updateButtons());
        removeButton.addActionListener(e -> service().unignore(list.getSelectedValuesList()));
        clearButton.addActionListener(e -> confirmClear());
        service().addListener(listener);
        root = buildLayout();
        reload();
    }

    public JComponent component() {
        return root;
    }

    public void dispose() {
        service().removeListener(listener);
    }

    private String label(String entry) {
        boolean folder = Files.isDirectory(service().list().resolve(entry));
        return folder ? entry + FOLDER_SUFFIX : entry;
    }

    private void reload() {
        model.clear();
        service().list().entries().forEach(model::addElement);
        updateButtons();
    }

    private void updateButtons() {
        removeButton.setEnabled(!list.isSelectionEmpty());
        clearButton.setEnabled(!model.isEmpty());
    }

    private void confirmClear() {
        int answer = Messages.showYesNoDialog(project, message("ignore.panel.clear.question"),
                message("ignore.panel.clear"), Messages.getQuestionIcon());
        if (answer == Messages.YES) {
            service().clear();
        }
    }

    private JPanel buildLayout() {
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(removeButton);
        buttons.add(Box.createHorizontalStrut(GAP));
        buttons.add(clearButton);
        buttons.add(Box.createHorizontalGlue());
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.add(new JBLabel(message("ignore.panel.hint")), BorderLayout.NORTH);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private IgnoreService service() {
        return IgnoreService.getInstance(project);
    }
}
