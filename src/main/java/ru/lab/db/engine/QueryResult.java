package ru.lab.db.engine;

import ru.lab.db.model.Record;

import java.util.List;

/** Columns and records returned by a SELECT query. */
public final class QueryResult {

    private final List<String> columns;
    private final List<Record> records;

    /**
     * Creates a query result.
     *
     * @param columns the result columns
     * @param records the selected records
     */
    public QueryResult(List<String> columns, List<Record> records) {
        this.columns = columns;
        this.records = records;
    }

    /**
     * Returns the result columns.
     *
     * @return the result columns
     */
    public List<String> columns() { return columns; }

    /**
     * Returns the result records.
     *
     * @return the result records
     */
    public List<Record> records() { return records; }
}
