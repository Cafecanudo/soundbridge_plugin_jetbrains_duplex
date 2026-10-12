package io.github.cafecanudo.soundbridgeduplex.history;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Optional;
import java.util.function.Consumer;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class HistoryPanel {

    private static final int VISIBLE_ROWS = 6;
    private static final int BUTTON_GAP = 6;
    private static final int ITEMS_MIN_WIDTH = 200;
    private static final int COLUMN_MIN_WIDTH = 24;
    private static final int COLUMN_MAX_FACTOR = 3;
    private static final int[] COLUMN_WIDTHS = {32, 0, 50, 50, 74, 54, 78, 82};

    private final Project project;
    private final HistoryTableModel model = new HistoryTableModel();
    private final JBTable table = new JBTable(model);
    private final JButton resendButton = new JButton(message("history.resend"));
    private final JButton logButton = new JButton(message("history.viewLog"));
    private final JButton clearButton = new JButton(message("history.clear"));
    private final Runnable historyListener = this::reload;
    private final JPanel root;

    public HistoryPanel(Project project, Consumer<HistoryEntry> resend) {
        this.project = project;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sizeColumns();
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        resendButton.addActionListener(e -> selected().ifPresent(resend));
        logButton.addActionListener(e -> selected().ifPresent(entry -> new HistoryLogDialog(project, entry).show()));
        clearButton.addActionListener(e -> confirmClear());
        history().addListener(historyListener);
        root = buildLayout();
        reload();
    }

    public JComponent component() {
        return root;
    }

    public void dispose() {
        history().removeListener(historyListener);
    }

    private void sizeColumns() {
        for (int column = 0; column < COLUMN_WIDTHS.length; column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            if (column == HistoryTableModel.ITEMS) {
                tableColumn.setMinWidth(JBUI.scale(ITEMS_MIN_WIDTH));
                continue;
            }
            int width = JBUI.scale(COLUMN_WIDTHS[column]);
            tableColumn.setMinWidth(JBUI.scale(COLUMN_MIN_WIDTH));
            tableColumn.setMaxWidth(width * COLUMN_MAX_FACTOR);
            tableColumn.setPreferredWidth(width);
        }
    }

    private void reload() {
        model.setEntries(history().entries());
        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = selected().isPresent();
        resendButton.setEnabled(selected().filter(entry -> entry.direction() == Direction.SENT).isPresent());
        logButton.setEnabled(hasSelection);
        clearButton.setEnabled(model.getRowCount() > 0);
    }

    private Optional<HistoryEntry> selected() {
        int row = table.getSelectedRow();
        return row < 0 ? Optional.empty() : Optional.of(model.entryAt(table.convertRowIndexToModel(row)));
    }

    private void confirmClear() {
        int answer = Messages.showYesNoDialog(project, message("history.clear.question"), message("history.clear.title"),
                Messages.getQuestionIcon());
        if (answer == Messages.YES) {
            history().clear();
        }
    }

    private JPanel buildLayout() {
        JBScrollPane scroll = new JBScrollPane(table);
        int rowHeight = table.getRowHeight();
        scroll.setPreferredSize(new Dimension(JBUI.scale(400), rowHeight * (VISIBLE_ROWS + 1) + JBUI.scale(4)));
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(resendButton);
        buttons.add(Box.createHorizontalStrut(BUTTON_GAP));
        buttons.add(logButton);
        buttons.add(Box.createHorizontalStrut(BUTTON_GAP));
        buttons.add(clearButton);
        buttons.add(Box.createHorizontalGlue());
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private SendHistory history() {
        return SendHistory.getInstance(project);
    }
}
