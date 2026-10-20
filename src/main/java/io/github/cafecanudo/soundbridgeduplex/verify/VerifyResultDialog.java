package io.github.cafecanudo.soundbridgeduplex.verify;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class VerifyResultDialog extends DialogWrapper {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int STATUS_WIDTH = 110;
    private static final int CHECK_WIDTH = 32;
    private static final int GAP = 6;

    private final List<VerifyItem> items;
    private final Consumer<List<String>> resend;
    private final boolean[] marked;
    private final JBCheckBox markAll = new JBCheckBox(message("verify.dialog.markAll"));
    private ItemsModel model;

    public VerifyResultDialog(Project project, String when, List<VerifyItem> items, Consumer<List<String>> resend) {
        super(project, false);
        this.items = VerifySummary.problemsFirst(items);
        this.resend = resend;
        this.marked = new boolean[this.items.size()];
        setTitle(message("verify.dialog.title", when));
        init();
        updateOkText();
    }

    @Override
    protected JComponent createCenterPanel() {
        model = new ItemsModel();
        JBTable table = new JBTable(model);
        if (selectable()) {
            table.setDefaultRenderer(Boolean.class, new MarkRenderer(table.getDefaultRenderer(Boolean.class)));
            fixWidth(table, 0, CHECK_WIDTH);
        }
        int status = model.getColumnCount() - 1;
        table.getColumnModel().getColumn(status).setMaxWidth(JBUI.scale(STATUS_WIDTH * 2));
        table.getColumnModel().getColumn(status).setPreferredWidth(JBUI.scale(STATUS_WIDTH));
        JBScrollPane scroll = new JBScrollPane(table);
        scroll.setPreferredSize(new Dimension(JBUI.scale(WIDTH), JBUI.scale(HEIGHT)));
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(GAP)));
        panel.add(header(), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected void doOKAction() {
        List<String> paths = markedPaths();
        super.doOKAction();
        if (!paths.isEmpty()) {
            resend.accept(paths);
        }
    }

    private JComponent header() {
        JBLabel summary = new JBLabel(VerifySummary.text(items));
        if (!selectable()) {
            return summary;
        }
        markAll.addActionListener(e -> markAllProblems(markAll.isSelected()));
        JPanel header = new JPanel(new BorderLayout(JBUI.scale(GAP), 0));
        header.add(summary, BorderLayout.WEST);
        header.add(markAll, BorderLayout.EAST);
        return header;
    }

    private boolean selectable() {
        return resend != null && IntStream.range(0, items.size()).anyMatch(this::isProblem);
    }

    private boolean isProblem(int row) {
        return items.get(row).status() != VerifyStatus.EXISTS;
    }

    private void markAllProblems(boolean value) {
        IntStream.range(0, items.size()).filter(this::isProblem).forEach(row -> marked[row] = value);
        model.fireTableDataChanged();
        updateOkText();
    }

    private List<String> markedPaths() {
        List<String> paths = new ArrayList<>();
        for (int row = 0; row < items.size(); row++) {
            if (marked[row]) {
                paths.add(items.get(row).path());
            }
        }
        return paths;
    }

    private void updateOkText() {
        int count = markedPaths().size();
        setOKButtonText(count == 0 ? message("verify.dialog.ok") : message("verify.dialog.resend", count));
    }

    private static void fixWidth(JTable table, int column, int width) {
        table.getColumnModel().getColumn(column).setMaxWidth(JBUI.scale(width));
        table.getColumnModel().getColumn(column).setMinWidth(JBUI.scale(width));
    }

    private final class ItemsModel extends AbstractTableModel {

        private static final List<String> COLUMNS = List.of("verify.dialog.column.path", "verify.dialog.column.status");

        private int offset() {
            return selectable() ? 1 : 0;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.size() + offset();
        }

        @Override
        public String getColumnName(int column) {
            return column < offset() ? "" : message(COLUMNS.get(column - offset()));
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column < offset() ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column < offset() && isProblem(row);
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column < offset()) {
                return isProblem(row) ? marked[row] : null;
            }
            VerifyItem item = items.get(row);
            return column - offset() == 0 ? item.path() : item.status().label();
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (isCellEditable(row, column) && value instanceof Boolean checked) {
                marked[row] = checked;
                fireTableCellUpdated(row, column);
                updateOkText();
            }
        }
    }

    private static final class MarkRenderer implements TableCellRenderer {

        private final TableCellRenderer checkbox;
        private final DefaultTableCellRenderer blank = new DefaultTableCellRenderer();

        private MarkRenderer(TableCellRenderer checkbox) {
            this.checkbox = checkbox;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            if (value == null) {
                return blank.getTableCellRendererComponent(table, "", selected, focused, row, column);
            }
            return checkbox.getTableCellRendererComponent(table, value, selected, focused, row, column);
        }
    }
}
