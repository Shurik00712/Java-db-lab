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

/** Таблица, хранящая записи по первичному ключу. */
public final class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Schema schema;
    private final Map<Cell, Record> rows;

    public Table(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.rows = new LinkedHashMap<>();
    }

    public Schema schema() { return schema; }

    private Cell primaryKeyOf(Record record) {
        return record.get(schema.primaryKey());
    }

    public Record findByPk(Cell pk) {
        return rows.get(pk);
    }

    public boolean containsValue(String field, Cell value) {
        for (Record record : rows.values()) {
            if (record.get(field).equals(value)) return true;
        }
        return false;
    }

    public List<Record> select(Condition where) {
        return select(where, null);
    }

    /** Выбирает записи по условию, при необходимости ограничивая поиск ключами индекса. */
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

    /** Добавляет записи, отклоняя повторные первичные ключи внутри пакета. */
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

    /** Заменяет записи, предварительно проверяя уникальность новых ключей. */
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

    public void deleteAll(Collection<Record> records) {
        for (Record record : records) {
            rows.remove(primaryKeyOf(record));
        }
    }

    public List<Record> records() {
        return new ArrayList<>(rows.values());
    }

    public int size() {
        return rows.size();
    }

    private String duplicateKey(Cell key) {
        return "Duplicate primary key " + key.display() + " in table '" + schema.name() + "'";
    }
}
