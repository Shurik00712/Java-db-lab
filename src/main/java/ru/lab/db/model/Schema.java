package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;
import ru.lab.db.error.DbException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Неизменяемое описание полей, первичного ключа и связей таблицы. */
public final class Schema implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<Field> fields;
    private final Map<String, Integer> order;
    private final String primaryKey;
    private final List<Reference> references;

    public Schema(String name, List<Field> fields, String primaryKey,
                  List<Reference> references) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name must not be empty");
        }
        this.name = name;
        this.fields = new ArrayList<>(Objects.requireNonNull(fields, "fields"));
        if (this.fields.isEmpty()) {
            throw new IllegalArgumentException("Table '" + name + "' has no fields");
        }
        this.primaryKey = Objects.requireNonNull(primaryKey, "primaryKey");
        this.references = new ArrayList<>(
            references == null ? Collections.<Reference>emptyList() : references);

        Map<String, Integer> positions = new LinkedHashMap<>();
        for (int i = 0; i < this.fields.size(); i++) {
            String fieldName = this.fields.get(i).name();
            if (positions.containsKey(fieldName)) {
                throw new IllegalArgumentException(
                    "Duplicate field '" + fieldName + "' in table '" + name + "'");
            }
            positions.put(fieldName, i);
        }
        this.order = positions;
        validate();
    }

    public Field findField(String fieldName) {
        Integer index = order.get(fieldName);
        return index == null ? null : fields.get(index);
    }

    public boolean hasField(String fieldName) {
        return order.containsKey(fieldName);
    }

    public Field field(String fieldName) {
        Field found = findField(fieldName);
        if (found == null) {
            throw new DbException("Unknown field '" + fieldName + "' in table '" + name + "'");
        }
        return found;
    }

    /** Собирает и проверяет запись по значениям полей. */
    public Record record(Map<String, Cell> values) {
        Map<String, Cell> full = new LinkedHashMap<>();
        for (Field field : fields) {
            Cell value = values.get(field.name());
            if (value == null) {
                throw new ConstraintViolationException(
                    "Missing value for field '" + field.name() + "'");
            }
            field.check(value);
            full.put(field.name(), value);
        }
        for (String key : values.keySet()) {
            if (!hasField(key)) {
                throw new ConstraintViolationException(
                    "Unknown field '" + key + "' in table '" + name + "'");
            }
        }
        return new Record(full);
    }

    private void validate() {
        Field pk = findField(primaryKey);
        if (pk == null) {
            throw new IllegalStateException(
                "Primary key field '" + primaryKey + "' not found in table '" + name + "'");
        }
        if (pk.nullable() || !pk.primaryKey()) {
            throw new IllegalStateException(
                "Primary key field '" + primaryKey + "' must be NOT NULL PRIMARY KEY");
        }
        for (Field field : fields) {
            if (field.primaryKey() && !field.name().equals(primaryKey)) {
                throw new IllegalStateException(
                    "Field '" + field.name() + "' is marked PRIMARY KEY but is not the primary key");
            }
        }
        for (Reference reference : references) {
            if (!hasField(reference.field())) {
                throw new IllegalStateException(
                    "Reference field '" + reference.field() + "' not found in table '" + name + "'");
            }
        }
    }

    public String name() { return name; }
    public List<Field> fields() { return new ArrayList<>(fields); }
    public String primaryKey() { return primaryKey; }
    public List<Reference> references() { return new ArrayList<>(references); }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("schema ").append(name).append("(\n");
        for (Field field : fields) out.append("  ").append(field).append('\n');
        out.append("  PRIMARY KEY ").append(primaryKey);
        for (Reference reference : references) out.append("\n  REFERENCE ").append(reference);
        return out.append("\n)").toString();
    }
}
