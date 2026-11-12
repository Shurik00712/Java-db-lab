package ru.lab.db.engine;

import ru.lab.db.error.ConstraintViolationException;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Database;
import ru.lab.db.model.Field;
import ru.lab.db.model.FieldType;
import ru.lab.db.model.Record;
import ru.lab.db.model.Schema;
import ru.lab.db.model.Table;
import ru.lab.db.store.Store;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Выполняет операции над таблицами и сохраняет изменения. */
public final class Engine {

    private final Database database;
    private final IndexManager indexes;
    private final Store store;

    public Engine(Database database) {
        this(database, null);
    }

    public Engine(Database database, Store store) {
        if (database == null) {
            throw new IllegalArgumentException("Database must not be null");
        }
        this.database = database;
        this.indexes = new IndexManager(database);
        this.store = store;
    }

    public QueryResult select(String tableName, List<String> columns, Condition where) {
        Table table = database.table(tableName);
        Schema schema = table.schema();

        List<String> targetColumns;
        if (columns == null || columns.isEmpty() || columns.contains("*")) {
            targetColumns = columnNames(schema);
        } else {
            targetColumns = new ArrayList<>(columns);
            for (String column : targetColumns) schema.field(column);
        }

        Condition coerced = coerceCondition(where, schema);
        Set<Cell> candidates = indexes.candidates(tableName, coerced);
        List<Record> records = table.select(coerced, candidates);
        return new QueryResult(targetColumns, records);
    }

    public HashIndex createIndex(String name, String tableName, String fieldName) {
        HashIndex index = indexes.create(name, tableName, fieldName);
        persist();
        return index;
    }

    public HashIndex findIndex(String name) {
        return indexes.findByName(name);
    }

    public void dropIndex(String name) {
        if (indexes.findByName(name) == null) {
            throw new ConstraintViolationException("Unknown index '" + name + "'");
        }
        indexes.drop(name);
        persist();
    }

    public void createTable(Schema schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema must not be null");
        }
        Table table = new Table(schema);
        database.checkReferences(table);
        database.add(table);
        persist();
    }

    public void dropTable(String tableName) {
        database.drop(tableName);
        indexes.dropTable(tableName);
        persist();
    }

    public int insert(String tableName, List<String> columnNames,
                      List<List<Cell>> rowsValues) {
        Table table = database.table(tableName);
        Schema schema = table.schema();

        List<String> targetColumns;
        if (columnNames == null || columnNames.isEmpty()) {
            targetColumns = columnNames(schema);
        } else {
            targetColumns = new ArrayList<>(columnNames);
        }
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (int i = 0; i < targetColumns.size(); i++) {
            String column = targetColumns.get(i);
            schema.field(column);
            if (positions.put(column, i) != null) {
                throw new ConstraintViolationException(
                    "Duplicate field '" + column + "' in INSERT");
            }
        }

        List<Record> records = new ArrayList<>(rowsValues.size());
        for (List<Cell> values : rowsValues) {
            if (values.size() != targetColumns.size()) {
                throw new ConstraintViolationException(
                    "INSERT expected " + targetColumns.size() + " values, got " + values.size());
            }
            Map<String, Cell> map = new LinkedHashMap<>();
            for (Field field : schema.fields()) {
                Integer index = positions.get(field.name());
                if (index != null) {
                    map.put(field.name(), coerceValue(values.get(index), field));
                } else if (field.hasDefault()) {
                    map.put(field.name(), field.defaultValue());
                } else if (field.nullable()) {
                    map.put(field.name(), Cell.nullOf(field.type()));
                } else {
                    throw new ConstraintViolationException(
                        "Missing value for NOT NULL field '" + field.name() + "' without default");
                }
            }
            records.add(schema.record(map));
        }

        for (Record record : records) {
            database.checkInsert(table, record);
        }
        table.insertAll(records);
        indexes.onInsertAll(table, records);
        persist();
        return records.size();
    }

    public int update(String tableName, Map<String, Cell> changes, Condition where) {
        Table table = database.table(tableName);
        Schema schema = table.schema();
        if (changes == null || changes.isEmpty()) {
            throw new ConstraintViolationException("UPDATE has no SET assignments");
        }
        Map<String, Cell> coerced = new LinkedHashMap<>();
        for (Map.Entry<String, Cell> change : changes.entrySet()) {
            coerced.put(change.getKey(),
                coerceValue(change.getValue(), schema.field(change.getKey())));
        }

        Condition coercedWhere = coerceCondition(where, schema);
        List<Record> matched = table.select(coercedWhere);
        if (matched.isEmpty()) return 0;

        List<Record> updated = new ArrayList<>(matched.size());
        for (Record before : matched) {
            Map<String, Cell> merged = new LinkedHashMap<>(before.values());
            merged.putAll(coerced);
            Record after = schema.record(merged);
            database.checkUpdate(table, before, after);
            updated.add(after);
        }

        table.replaceAll(matched, updated);
        for (int i = 0; i < matched.size(); i++) {
            indexes.onUpdate(table, matched.get(i), updated.get(i));
        }
        persist();
        return matched.size();
    }

    public int delete(String tableName, Condition where) {
        Table table = database.table(tableName);
        Schema schema = table.schema();
        Condition coerced = coerceCondition(where, schema);
        List<Record> matched = table.select(coerced);
        if (matched.isEmpty()) return 0;

        database.checkDelete(table, matched);
        table.deleteAll(matched);
        indexes.onDeleteAll(table, matched);
        persist();
        return matched.size();
    }

    private Cell coerceValue(Cell value, Field field) {
        if (value.isNull()) return Cell.nullOf(field.type());
        if (value.type() == field.type()) return value;
        if (field.type() == FieldType.DOUBLE && value.type() == FieldType.INTEGER) {
            return Cell.ofDouble(value.asInt());
        }
        if (field.type() == FieldType.DATE && value.type() == FieldType.STRING) {
            try {
                return Cell.ofDate(LocalDate.parse(value.asString()));
            } catch (DateTimeParseException e) {
                throw new ConstraintViolationException(
                    "Invalid DATE literal: '" + value.asString() + "' (expected yyyy-MM-dd)");
            }
        }
        return value;
    }

    private Condition coerceCondition(Condition condition, Schema schema) {
        if (condition == null) return null;
        Field field = schema.field(condition.field());
        return new Condition(condition.field(), condition.operator(),
            coerceValue(condition.literal(), field));
    }

    private static List<String> columnNames(Schema schema) {
        List<String> names = new ArrayList<>();
        for (Field field : schema.fields()) names.add(field.name());
        return names;
    }

    private void persist() {
        if (store != null) store.save(database);
    }
}
