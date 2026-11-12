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

/** Hash-индекс по одному полю, ускоряющий поиск по равенству. */
public final class HashIndex {

    private final String name;
    private final String table;
    private final String field;
    private final String primaryKeyField;
    private final Map<Cell, Set<Cell>> buckets = new LinkedHashMap<>();

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

    public String name() { return name; }
    public String table() { return table; }
    public String field() { return field; }

    public void onInsert(Record record) {
        add(record.get(field), record.get(primaryKeyField));
    }

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

    public void onDelete(Record record) {
        remove(record.get(field), record.get(primaryKeyField));
    }

    public void rebuild(Collection<Record> records) {
        buckets.clear();
        for (Record record : records) {
            onInsert(record);
        }
    }

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
