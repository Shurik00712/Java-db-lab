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

/** Создаёт индексы и поддерживает их актуальность при изменении таблиц. */
public final class IndexManager {

    private final Database database;
    private final Map<String, HashIndex> byName = new LinkedHashMap<>();
    private final Map<String, HashIndex> byTableField = new LinkedHashMap<>();

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

    public HashIndex findByName(String name) {
        return byName.get(name);
    }

    public HashIndex findByField(String tableName, String fieldName) {
        return byTableField.get(keyOf(tableName, fieldName));
    }

    public void drop(String name) {
        HashIndex index = byName.remove(name);
        if (index != null) {
            byTableField.remove(keyOf(index.table(), index.field()));
            database.removeIndex(name);
        }
    }

    public void dropTable(String tableName) {
        List<String> toDrop = new ArrayList<>();
        for (HashIndex index : byName.values()) {
            if (index.table().equals(tableName)) toDrop.add(index.name());
        }
        for (String name : toDrop) drop(name);
    }

    public void onInsertAll(Table table, List<Record> records) {
        for (HashIndex index : byName.values()) {
            if (index.table().equals(table.schema().name())) {
                for (Record record : records) index.onInsert(record);
            }
        }
    }

    public void onUpdate(Table table, Record before, Record after) {
        for (HashIndex index : byName.values()) {
            if (index.table().equals(table.schema().name())) index.onUpdate(before, after);
        }
    }

    public void onDeleteAll(Table table, Collection<Record> records) {
        for (HashIndex index : byName.values()) {
            if (index.table().equals(table.schema().name())) {
                for (Record record : records) index.onDelete(record);
            }
        }
    }

    /** Возвращает ключи-кандидаты по индексу или null, если индекса нет. */
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
