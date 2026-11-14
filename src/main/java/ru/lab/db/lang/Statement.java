package ru.lab.db.lang;

import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A parsed SQL command; available fields depend on {@link #kind()}. */
public final class Statement {

    /** Supported command kinds. */
    public enum Kind {
        /** Inserts records. */
        INSERT,
        /** Selects records. */
        SELECT,
        /** Updates records. */
        UPDATE,
        /** Deletes records. */
        DELETE,
        /** Creates a table. */
        CREATE_TABLE,
        /** Drops a table. */
        DROP_TABLE,
        /** Creates an index. */
        CREATE_INDEX,
        /** Drops an index. */
        DROP_INDEX
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

    /**
     * Creates an INSERT command.
     *
     * @param table the table name
     * @param columns the field names, or {@code null} if all fields are used
     * @param rows the record values
     * @return the INSERT command
     */
    public static Statement insert(String table, List<String> columns, List<List<Cell>> rows) {
        return new Statement(Kind.INSERT, table, columns, rows, null, null, null, null, null);
    }

    /**
     * Creates a SELECT command.
     *
     * @param table the table name
     * @param columns the selected fields, or an empty list for all fields
     * @param where the condition, or {@code null}
     * @return the SELECT command
     */
    public static Statement select(String table, List<String> columns, Condition where) {
        return new Statement(Kind.SELECT, table, columns, null, null, where, null, null, null);
    }

    /**
     * Creates an UPDATE command.
     *
     * @param table the table name
     * @param assignments the new field values
     * @param where the condition, or {@code null}
     * @return the UPDATE command
     */
    public static Statement update(String table, Map<String, Cell> assignments, Condition where) {
        return new Statement(Kind.UPDATE, table, null, null, assignments, where, null, null, null);
    }

    /**
     * Creates a DELETE command.
     *
     * @param table the table name
     * @param where the condition, or {@code null}
     * @return the DELETE command
     */
    public static Statement delete(String table, Condition where) {
        return new Statement(Kind.DELETE, table, null, null, null, where, null, null, null);
    }

    /**
     * Creates a CREATE TABLE command.
     *
     * @param schema the new table schema
     * @return the CREATE TABLE command
     */
    public static Statement createTable(Schema schema) {
        return new Statement(Kind.CREATE_TABLE, null, null, null, null, null, schema, null, null);
    }

    /**
     * Creates a DROP TABLE command.
     *
     * @param table the table name
     * @return the DROP TABLE command
     */
    public static Statement dropTable(String table) {
        return new Statement(Kind.DROP_TABLE, table, null, null, null, null, null, null, null);
    }

    /**
     * Creates a CREATE INDEX command.
     *
     * @param name the index name
     * @param table the table name
     * @param field the indexed field name
     * @return the CREATE INDEX command
     */
    public static Statement createIndex(String name, String table, String field) {
        return new Statement(Kind.CREATE_INDEX, table, null, null, null, null, null, name, field);
    }

    /**
     * Creates a DROP INDEX command.
     *
     * @param name the index name
     * @return the DROP INDEX command
     */
    public static Statement dropIndex(String name) {
        return new Statement(Kind.DROP_INDEX, null, null, null, null, null, null, name, null);
    }

    /**
     * Returns the command kind.
     *
     * @return the command kind
     */
    public Kind kind() { return kind; }

    /**
     * Returns the table name.
     *
     * @return the table name, or {@code null} if the command has no table
     */
    public String table() { return table; }

    /**
     * Returns the command column list.
     *
     * @return a copy of the column list, or {@code null} if it is not set
     */
    public List<String> columns() {
        return columns == null ? null : new ArrayList<>(columns);
    }

    /**
     * Returns the values of the inserted records.
     *
     * @return a copy of the row list
     */
    public List<List<Cell>> rows() { return copyRows(rows); }

    /**
     * Returns the UPDATE assignments.
     *
     * @return a copy of the assignments, or {@code null} if they are not set
     */
    public Map<String, Cell> assignments() {
        return assignments == null ? null : new LinkedHashMap<>(assignments);
    }

    /**
     * Returns the selection condition.
     *
     * @return the condition or {@code null}
     */
    public Condition where() { return where; }

    /**
     * Returns the schema of the new table.
     *
     * @return the schema or {@code null}
     */
    public Schema schema() { return schema; }

    /**
     * Returns the index name.
     *
     * @return the index name or {@code null}
     */
    public String indexName() { return indexName; }

    /**
     * Returns the indexed field name.
     *
     * @return the field name or {@code null}
     */
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
