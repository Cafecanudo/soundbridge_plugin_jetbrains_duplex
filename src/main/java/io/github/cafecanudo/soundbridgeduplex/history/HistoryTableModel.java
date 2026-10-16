package io.github.cafecanudo.soundbridgeduplex.history;

import javax.swing.Icon;
import javax.swing.table.AbstractTableModel;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class HistoryTableModel extends AbstractTableModel {

    public static final int NUMBER = 0;
    public static final int DIRECTION = 1;
    public static final int ITEMS = 2;

    private static final List<String> COLUMNS = List.of(
            "history.column.number", "history.column.direction", "history.column.items", "history.column.mode",
            "history.column.size", "history.column.mcs", "history.column.duration", "history.column.status");

    private List<HistoryEntry> entries = List.of();

    public void setEntries(List<HistoryEntry> values) {
        entries = List.copyOf(values);
        fireTableDataChanged();
    }

    public HistoryEntry entryAt(int row) {
        return entries.get(row);
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.size();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == NUMBER) {
            return Integer.class;
        }
        return column == DIRECTION ? Icon.class : String.class;
    }

    @Override
    public String getColumnName(int column) {
        return message(COLUMNS.get(column));
    }

    @Override
    public Object getValueAt(int row, int column) {
        HistoryEntry entry = entries.get(row);
        return switch (column) {
            case NUMBER -> row + 1;
            case DIRECTION -> entry.direction().icon();
            case ITEMS -> HistoryFormat.items(entry.items(), entry.deleted());
            case 3 -> HistoryFormat.mode(entry.snapshot().mode());
            case 4 -> HistoryFormat.size(entry.size());
            case 5 -> HistoryFormat.mcs(entry.finalMcs());
            case 6 -> HistoryFormat.duration(entry.seconds());
            default -> entry.status().label();
        };
    }
}
