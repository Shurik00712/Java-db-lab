package ru.lab.db.ui;

import ru.lab.db.model.Record;

import java.util.List;

/** Prints query results as tables. */
public final class Printer {

    /** Creates a console output formatter. */
    public Printer() {
    }

    /**
     * Prints records as a table.
     *
     * @param columns the columns to print
     * @param records the result records
     */
    public void select(List<String> columns, List<Record> records) {
        int[] widths = widths(columns, records);
        separator(widths);
        printRow(columns.toArray(new String[0]), widths);
        separator(widths);
        for (Record record : records) {
            String[] cells = new String[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                cells[i] = record.get(columns.get(i)).display();
            }
            printRow(cells, widths);
        }
        separator(widths);
        System.out.println(records.size() + " " + plural(records.size(), "row", "rows"));
    }

    /**
     * Prints the number of modified records.
     *
     * @param action the action description
     * @param count the number of records
     */
    public void modified(String action, int count) {
        System.out.println(count + " " + plural(count, "row", "rows") + " " + action);
    }

    /**
     * Prints an error message.
     *
     * @param message the error message
     */
    public void error(String message) {
        System.out.println("Error: " + message);
    }

    /**
     * Prints an informational message.
     *
     * @param message the message text
     */
    public void info(String message) {
        System.out.println(message);
    }

    private int[] widths(List<String> columns, List<Record> records) {
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }
        for (Record record : records) {
            for (int i = 0; i < columns.size(); i++) {
                int length = record.get(columns.get(i)).display().length();
                if (length > widths[i]) widths[i] = length;
            }
        }
        return widths;
    }

    private void separator(int[] widths) {
        StringBuilder out = new StringBuilder("+");
        for (int width : widths) {
            for (int i = 0; i < width + 2; i++) out.append('-');
            out.append('+');
        }
        System.out.println(out);
    }

    private void printRow(String[] cells, int[] widths) {
        StringBuilder out = new StringBuilder("|");
        for (int i = 0; i < cells.length; i++) {
            out.append(' ').append(cells[i]);
            for (int p = cells[i].length(); p < widths[i]; p++) out.append(' ');
            out.append(" |");
        }
        System.out.println(out);
    }

    private String plural(int n, String one, String many) {
        return n == 1 ? one : many;
    }
}
