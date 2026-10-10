package io.github.cafecanudo.soundbridgeduplex.history;

import javax.swing.table.AbstractTableModel;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class HistoryTableModel extends AbstractTableModel {

    private static final List<String> COLUMNS = List.of(
            "history.column.direction", "history.column.date", "history.column.items", "history.column.mode",
            "history.column.files", "history.column.size", "history.column.mcs", "history.column.duration",
            "history.column.status");

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
    public String getColumnName(int column) {
        return message(COLUMNS.get(column));
    }

    @Override
    public Object getValueAt(int row, int column) {
        HistoryEntry entry = entries.get(row);
        return switch (column) {
            case 0 -> entry.direction().label();
            case 1 -> HistoryFormat.date(entry.timestamp());
            case 2 -> HistoryFormat.items(entry.items());
            case 3 -> HistoryFormat.mode(entry.snapshot().mode());
            case 4 -> HistoryFormat.files(entry);
            case 5 -> HistoryFormat.size(entry.size());
            case 6 -> HistoryFormat.mcs(entry.finalMcs());
            case 7 -> HistoryFormat.duration(entry.seconds());
            default -> entry.status().label();
        };
    }
}
