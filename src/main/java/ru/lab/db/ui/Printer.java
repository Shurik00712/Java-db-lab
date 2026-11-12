package ru.lab.db.ui;

import ru.lab.db.model.Record;

import java.util.List;

/** Выводит результаты запросов в табличном виде. */
public final class Printer {

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
        System.out.println(records.size() + " "
            + plural(records.size(), "строка", "строки", "строк"));
    }

    public void modified(String action, int count) {
        System.out.println(message(count, action));
    }

    public void error(String message) {
        System.out.println("Ошибка: " + message);
    }

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

    private String message(int n, String action) {
        return n + " " + plural(n, "строка", "строки", "строк")
            + " " + action + plural(n, "а", "ы", "о");
    }

    private String plural(int n, String one, String few, String many) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return one;
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
        return many;
    }
}
