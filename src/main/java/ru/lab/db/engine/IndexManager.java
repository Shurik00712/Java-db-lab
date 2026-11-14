package ru.lab.db.engine;

import ru.lab.db.error.ConstraintViolationException;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Database;
import ru.lab.db.model.IndexSpec;
import ru.lab.db.model.Record;
import ru.lab.db.model.Schema;
import ru.lab.db.model.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Creates indexes and keeps them up to date when tables change. */
public final class IndexManager {

    private final Database database;
    private final Map<String, HashIndex> byName = new LinkedHashMap<>();
    private final Map<String, HashIndex> byTableField = new LinkedHashMap<>();

    /**
     * Creates a manager and rebuilds saved indexes.
     *
     * @param database the database
     */
    public IndexManager(Database database) {
        this.database = database;
        for (IndexSpec spec : database.indexes()) {
            build(spec);
        }
    }

    private void build(IndexSpec spec) {
        Table table = database.table(spec.table());
        Schema schema = table.schema();
        HashIndex index = new HashIndex(
            spec.name(), spec.table(), spec.field(), schema.primaryKey());
        index.rebuild(table.records());
        byName.put(spec.name(), index);
        byTableField.put(keyOf(spec.table(), spec.field()), index);
    }

    /**
     * Creates an index and adds its definition to the database.
     *
     * @param name the index name
     * @param tableName the table name
     * @param fieldName the indexed field name
     * @return the created index
     */
    public HashIndex create(String name, String tableName, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new ConstraintViolationException("Index name must not be empty");
        }
        Table table = database.table(tableName);
        Schema schema = table.schema();
        schema.field(fieldName);
        if (byName.containsKey(name)) {
            throw new ConstraintViolationException("Index '" + name + "' already exists");
        }
        String key = keyOf(tableName, fieldName);
        if (byTableField.containsKey(key)) {
            throw new ConstraintViolationException("Index on " + key + " already exists");
        }
        HashIndex index = new HashIndex(name, tableName, fieldName, schema.primaryKey());
        index.rebuild(table.records());
        byName.put(name, index);
        byTableField.put(key, index);
        database.addIndex(new IndexSpec(name, tableName, fieldName));
        return index;
    }

    /**
     * Finds an index by name.
     *
     * @param name the index name
     * @return the index, or {@code null}
     */
    public HashIndex findByName(String name) {
        return byName.get(name);
    }

    /**
     * Finds an index by table and field.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @return the index, or {@code null}
     */
    public HashIndex findByField(String tableName, String fieldName) {
        return byTableField.get(keyOf(tableName, fieldName));
    }

    /**
     * Removes an index and its definition.
     *
     * @param name the index name
     */
    public void drop(String name) {
        HashIndex index = byName.remove(name);
        if (index != null) {
            byTableField.remove(keyOf(index.table(), index.field()));
            database.removeIndex(name);
        }
    }

    /**
     * Removes all indexes for a table.
     *
     * @param tableName the table name
     */
    public void dropTable(String tableName) {
        List<String> toDrop = new ArrayList<>();
        for (HashIndex index : byName.values()) {
            if (index.table().equals(tableName)) toDrop.add(index.name());
        }
        for (String name : toDrop) drop(name);
    }

    /**
     * Updates indexes after inserting records.
     *
     * @param table the table
     * @param records the inserted records
     */
    public void onInsertAll(Table table, List<Record> records) {
        for (HashIndex index : byName.values()) {
            if (index.table().equals(table.schema().name())) {
                for (Record record : records) index.onInsert(record);
            }
        }
    }

    /**
     * Updates indexes after a record changes.
     *
     * @param table the table
     * @param before the original record
     * @param after the updated record
     */
    public void onUpdate(Table table, Record before, Record after) {
        for (HashIndex index : byName.values()) {
            if (index.table().equals(table.schema().name())) index.onUpdate(before, after);
        }
    }

    /**
     * Updates indexes after deleting records.
     *
     * @param table the table
     * @param records the deleted records
     */
    public void onDeleteAll(Table table, Collection<Record> records) {
        for (HashIndex index : byName.values()) {
            if (index.table().equals(table.schema().name())) {
                for (Record record : records) index.onDelete(record);
            }
        }
    }

    /**
     * Returns index candidate keys, or {@code null} if no index is available.
     *
     * @param tableName the table name
     * @param condition the selection condition
     * @return candidate keys, or {@code null} if no matching index exists
     */
    public Set<Cell> candidates(String tableName, Condition condition) {
        if (condition == null) return null;
        HashIndex index = findByField(tableName, condition.field());
        if (index == null) return null;
        return index.candidates(condition.literal(), condition.operator());
    }

    private static String keyOf(String tableName, String fieldName) {
        return tableName + "." + fieldName;
    }
}
