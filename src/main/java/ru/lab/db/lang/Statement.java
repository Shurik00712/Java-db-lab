package ru.lab.db.lang;

import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Разобранная SQL-команда; поля зависят от {@link #kind()}. */
public final class Statement {

    public enum Kind {
        INSERT, SELECT, UPDATE, DELETE,
        CREATE_TABLE, DROP_TABLE, CREATE_INDEX, DROP_INDEX
    }

    private final Kind kind;
    private final String table;
    private final List<String> columns;
    private final List<List<Cell>> rows;
    private final Map<String, Cell> assignments;
    private final Condition where;
    private final Schema schema;
    private final String indexName;
    private final String indexField;

    private Statement(Kind kind, String table, List<String> columns,
                      List<List<Cell>> rows, Map<String, Cell> assignments,
                      Condition where, Schema schema, String indexName, String indexField) {
        this.kind = kind;
        this.table = table;
        this.columns = columns == null ? null : new ArrayList<>(columns);
        this.rows = copyRows(rows);
        this.assignments = assignments == null ? null
            : new LinkedHashMap<>(assignments);
        this.where = where;
        this.schema = schema;
        this.indexName = indexName;
        this.indexField = indexField;
    }

    public static Statement insert(String table, List<String> columns, List<List<Cell>> rows) {
        return new Statement(Kind.INSERT, table, columns, rows, null, null, null, null, null);
    }

    public static Statement select(String table, List<String> columns, Condition where) {
        return new Statement(Kind.SELECT, table, columns, null, null, where, null, null, null);
    }

    public static Statement update(String table, Map<String, Cell> assignments, Condition where) {
        return new Statement(Kind.UPDATE, table, null, null, assignments, where, null, null, null);
    }

    public static Statement delete(String table, Condition where) {
        return new Statement(Kind.DELETE, table, null, null, null, where, null, null, null);
    }

    public static Statement createTable(Schema schema) {
        return new Statement(Kind.CREATE_TABLE, null, null, null, null, null, schema, null, null);
    }

    public static Statement dropTable(String table) {
        return new Statement(Kind.DROP_TABLE, table, null, null, null, null, null, null, null);
    }

    public static Statement createIndex(String name, String table, String field) {
        return new Statement(Kind.CREATE_INDEX, table, null, null, null, null, null, name, field);
    }

    public static Statement dropIndex(String name) {
        return new Statement(Kind.DROP_INDEX, null, null, null, null, null, null, name, null);
    }

    public Kind kind() { return kind; }
    public String table() { return table; }
    public List<String> columns() {
        return columns == null ? null : new ArrayList<>(columns);
    }
    public List<List<Cell>> rows() { return copyRows(rows); }
    public Map<String, Cell> assignments() {
        return assignments == null ? null : new LinkedHashMap<>(assignments);
    }
    public Condition where() { return where; }
    public Schema schema() { return schema; }
    public String indexName() { return indexName; }
    public String indexField() { return indexField; }

    private static List<List<Cell>> copyRows(List<List<Cell>> rows) {
        if (rows == null) return null;
        List<List<Cell>> copy = new ArrayList<>(rows.size());
        for (List<Cell> row : rows) {
            copy.add(row == null ? null : new ArrayList<>(row));
        }
        return copy;
    }
}
