package io.github.cafecanudo.soundbridgeduplex.verify;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class VerifyResultDialog extends DialogWrapper {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int STATUS_WIDTH = 110;
    private static final int GAP = 6;

    private final List<VerifyItem> items;

    public VerifyResultDialog(Project project, String when, List<VerifyItem> items) {
        super(project, false);
        this.items = VerifySummary.problemsFirst(items);
        setTitle(message("verify.dialog.title", when));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JBTable table = new JBTable(new ItemsModel(items));
        table.getColumnModel().getColumn(1).setMaxWidth(JBUI.scale(STATUS_WIDTH * 2));
        table.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(STATUS_WIDTH));
        JBScrollPane scroll = new JBScrollPane(table);
        scroll.setPreferredSize(new Dimension(JBUI.scale(WIDTH), JBUI.scale(HEIGHT)));
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(GAP)));
        panel.add(new JBLabel(VerifySummary.text(items)), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    private static final class ItemsModel extends AbstractTableModel {

        private static final List<String> COLUMNS = List.of("verify.dialog.column.path", "verify.dialog.column.status");

        private final List<VerifyItem> items;

        private ItemsModel(List<VerifyItem> items) {
            this.items = items;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.size();
        }

        @Override
        public String getColumnName(int column) {
            return message(COLUMNS.get(column));
        }

        @Override
        public Object getValueAt(int row, int column) {
            VerifyItem item = items.get(row);
            return column == 0 ? item.path() : item.status().label();
        }
    }
}
