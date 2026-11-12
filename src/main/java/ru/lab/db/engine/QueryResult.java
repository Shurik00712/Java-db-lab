package ru.lab.db.engine;

import ru.lab.db.model.Record;

import java.util.List;

/** Столбцы и записи результата SELECT. */
public final class QueryResult {

    private final List<String> columns;
    private final List<Record> records;

    public QueryResult(List<String> columns, List<Record> records) {
        this.columns = columns;
        this.records = records;
    }

    public List<String> columns() { return columns; }
    public List<Record> records() { return records; }
}
