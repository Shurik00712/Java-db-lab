package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A table that stores records by primary key. */
public final class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Table schema. */
    private final Schema schema;
    /** Table records keyed by primary key. */
    private final Map<Cell, Record> rows;

    /**
     * Creates an empty table with the given schema.
     *
     * @param schema the table schema
     */
    public Table(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.rows = new LinkedHashMap<>();
    }

    /**
     * Returns the table schema.
     *
     * @return the table schema
     */
    public Schema schema() { return schema; }

    private Cell primaryKeyOf(Record record) {
        return record.get(schema.primaryKey());
    }

    /**
     * Finds a record by primary key.
     *
     * @param pk the primary key value
     * @return the record, or {@code null} if it is not found
     */
    public Record findByPk(Cell pk) {
        return rows.get(pk);
    }

    /**
     * Checks whether the table contains the given field value.
     *
     * @param field the field name
     * @param value the value to find
     * @return {@code true} if the value is found
     */
    public boolean containsValue(String field, Cell value) {
        for (Record record : rows.values()) {
            if (record.get(field).equals(value)) return true;
        }
        return false;
    }

    /**
     * Selects records that match a condition.
     *
     * @param where the condition, or {@code null} to select all records
     * @return the matching records
     */
    public List<Record> select(Condition where) {
        return select(where, null);
    }

    /**
     * Selects records, optionally limiting the search to indexed keys.
     *
     * @param where the condition, or {@code null} to select all records
     * @param candidatePks candidate keys, or {@code null} for a full scan
     * @return the matching records
     */
    public List<Record> select(Condition where, Set<Cell> candidatePks) {
        if (where != null) {
            where.check(schema.field(where.field()));
        }
        Collection<Record> candidates;
        if (candidatePks == null) {
            candidates = rows.values();
        } else {
            List<Record> indexed = new ArrayList<>(candidatePks.size());
            for (Cell pk : candidatePks) {
                Record record = rows.get(pk);
                if (record != null) indexed.add(record);
            }
            candidates = indexed;
        }
        List<Record> result = new ArrayList<>();
        for (Record record : candidates) {
            if (where == null || where.test(record)) result.add(record);
        }
        return result;
    }

    /**
     * Adds records and rejects duplicate primary keys.
     *
     * @param records the records to add
     */
    public void insertAll(List<Record> records) {
        Set<Cell> batch = new LinkedHashSet<>();
        for (Record record : records) {
            Cell pk = primaryKeyOf(record);
            if (rows.containsKey(pk) || !batch.add(pk)) {
                throw new ConstraintViolationException(duplicateKey(pk));
            }
        }
        for (Record record : records) {
            rows.put(primaryKeyOf(record), record);
        }
    }

    /**
     * Replaces records after validating their new primary keys.
     *
     * @param before the original records
     * @param updated the replacement records
     */
    public void replaceAll(Collection<Record> before, Collection<Record> updated) {
        if (before.size() != updated.size()) {
            throw new IllegalArgumentException("Before and after record counts differ");
        }
        Set<Cell> oldKeys = new LinkedHashSet<>();
        for (Record record : before) {
            Cell key = primaryKeyOf(record);
            if (!rows.containsKey(key) || !oldKeys.add(key)) {
                throw new ConstraintViolationException(
                    "Cannot replace missing or duplicate record in table '" + schema.name() + "'");
            }
        }
        Set<Cell> newKeys = new LinkedHashSet<>();
        for (Record record : updated) {
            Cell key = primaryKeyOf(record);
            if (!newKeys.add(key) || (rows.containsKey(key) && !oldKeys.contains(key))) {
                throw new ConstraintViolationException(duplicateKey(key));
            }
        }
        for (Cell key : oldKeys) {
            rows.remove(key);
        }
        for (Record record : updated) {
            rows.put(primaryKeyOf(record), record);
        }
    }

    /**
     * Deletes records from the table.
     *
     * @param records the records to delete
     */
    public void deleteAll(Collection<Record> records) {
        for (Record record : records) {
            rows.remove(primaryKeyOf(record));
        }
    }

    /**
     * Returns a copy of the record list.
     *
     * @return the table records
     */
    public List<Record> records() {
        return new ArrayList<>(rows.values());
    }

    /**
     * Returns the number of records.
     *
     * @return the record count
     */
    public int size() {
        return rows.size();
    }

    private String duplicateKey(Cell key) {
        return "Duplicate primary key " + key.display() + " in table '" + schema.name() + "'";
    }
}
