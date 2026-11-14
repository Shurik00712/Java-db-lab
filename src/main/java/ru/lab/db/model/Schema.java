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

/** Immutable definition of a table's fields, primary key, and references. */
public final class Schema implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Table name. */
    private final String name;
    /** Table fields in declaration order. */
    private final List<Field> fields;
    /** Field positions by name. */
    private final Map<String, Integer> order;
    /** Primary key field name. */
    private final String primaryKey;
    /** Table references to other tables. */
    private final List<Reference> references;

    /**
     * Creates and validates a table schema.
     *
     * @param name the table name
     * @param fields the table fields
     * @param primaryKey the primary key field name
     * @param references the table references
     */
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

    /**
     * Finds a field by name.
     *
     * @param fieldName the field name
     * @return the field, or {@code null} if it is not found
     */
    public Field findField(String fieldName) {
        Integer index = order.get(fieldName);
        return index == null ? null : fields.get(index);
    }

    /**
     * Checks whether a field exists.
     *
     * @param fieldName the field name
     * @return {@code true} if the field exists
     */
    public boolean hasField(String fieldName) {
        return order.containsKey(fieldName);
    }

    /**
     * Returns a field by name.
     *
     * @param fieldName the field name
     * @return the field definition
     */
    public Field field(String fieldName) {
        Field found = findField(fieldName);
        if (found == null) {
            throw new DbException("Unknown field '" + fieldName + "' in table '" + name + "'");
        }
        return found;
    }

    /**
     * Builds and validates a record from field values.
     *
     * @param values the field values
     * @return the validated record
     */
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

    /**
     * Returns the table name.
     *
     * @return the table name
     */
    public String name() { return name; }

    /**
     * Returns a copy of the field list.
     *
     * @return a copy of the field list
     */
    public List<Field> fields() { return new ArrayList<>(fields); }

    /**
     * Returns the primary key field name.
     *
     * @return the primary key field name
     */
    public String primaryKey() { return primaryKey; }

    /**
     * Returns a copy of the reference list.
     *
     * @return a copy of the reference list
     */
    public List<Reference> references() { return new ArrayList<>(references); }

    /** Returns a text description of the schema. */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("schema ").append(name).append("(\n");
        for (Field field : fields) out.append("  ").append(field).append('\n');
        out.append("  PRIMARY KEY ").append(primaryKey);
        for (Reference reference : references) out.append("\n  REFERENCE ").append(reference);
        return out.append("\n)").toString();
    }
}
