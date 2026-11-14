package ru.lab.db.engine;

import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Record;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Hash index on one field for faster equality searches. */
public final class HashIndex {

    private final String name;
    private final String table;
    private final String field;
    private final String primaryKeyField;
    private final Map<Cell, Set<Cell>> buckets = new LinkedHashMap<>();

    /**
     * Creates an index for a field value and the table primary key.
     *
     * @param name the index name
     * @param table the table name
     * @param field the indexed field
     * @param primaryKeyField the primary key field
     */
    public HashIndex(String name, String table, String field, String primaryKeyField) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("index name");
        if (table == null || table.isEmpty()) throw new IllegalArgumentException("table name");
        if (field == null || field.isEmpty()) throw new IllegalArgumentException("field name");
        if (primaryKeyField == null || primaryKeyField.isEmpty()) {
            throw new IllegalArgumentException("primary key field");
        }
        this.name = name;
        this.table = table;
        this.field = field;
        this.primaryKeyField = primaryKeyField;
    }

    /**
     * Returns the index name.
     *
     * @return the index name
     */
    public String name() { return name; }

    /**
     * Returns the table name.
     *
     * @return the table name
     */
    public String table() { return table; }

    /**
     * Returns the indexed field name.
     *
     * @return the field name
     */
    public String field() { return field; }

    /**
     * Adds a record to the index.
     *
     * @param record the record to add
     */
    public void onInsert(Record record) {
        add(record.get(field), record.get(primaryKeyField));
    }

    /**
     * Updates a record's key in the index.
     *
     * @param before the original record
     * @param after the updated record
     */
    public void onUpdate(Record before, Record after) {
        Cell oldKey = before.get(field);
        Cell newKey = after.get(field);
        Cell oldPk = before.get(primaryKeyField);
        Cell newPk = after.get(primaryKeyField);
        if (!oldPk.equals(newPk)) {
            remove(oldKey, oldPk);
            add(newKey, newPk);
        } else if (!oldKey.equals(newKey)) {
            remove(oldKey, oldPk);
            add(newKey, oldPk);
        }
    }

    /**
     * Removes a record from the index.
     *
     * @param record the record to remove
     */
    public void onDelete(Record record) {
        remove(record.get(field), record.get(primaryKeyField));
    }

    /**
     * Rebuilds the index from all table records.
     *
     * @param records the table records
     */
    public void rebuild(Collection<Record> records) {
        buckets.clear();
        for (Record record : records) {
            onInsert(record);
        }
    }

    /**
     * Returns primary keys for a supported equality condition.
     *
     * @param literal the value to search for
     * @param operator the comparison operator
     * @return the matching keys, or {@code null} if the operator is unsupported
     */
    public Set<Cell> candidates(Cell literal, String operator) {
        if (!Condition.EQ.equals(operator)) return null;
        Set<Cell> bucket = buckets.get(literal);
        return bucket == null ? Collections.<Cell>emptySet() : new LinkedHashSet<>(bucket);
    }

    private void add(Cell key, Cell pk) {
        Set<Cell> bucket = buckets.get(key);
        if (bucket == null) {
            bucket = new LinkedHashSet<>();
            buckets.put(key, bucket);
        }
        bucket.add(pk);
    }

    private void remove(Cell key, Cell pk) {
        Set<Cell> bucket = buckets.get(key);
        if (bucket == null) return;
        bucket.remove(pk);
        if (bucket.isEmpty()) buckets.remove(key);
    }
}
