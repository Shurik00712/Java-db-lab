package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.io.Serializable;
import java.util.Objects;

/** Defines a table field and its constraints. */
public final class Field implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Field name. */
    private final String name;
    /** Field type. */
    private final FieldType type;
    /** Whether NULL is allowed. */
    private final boolean nullable;
    /** Whether the field is the primary key. */
    private final boolean primaryKey;
    /** Default value or {@code null}. */
    private final Cell defaultValue;
    /** Minimum numeric value or {@code null}. */
    private final Double minimum;

    /**
     * Creates a field definition without a minimum numeric value.
     *
     * @param name the field name
     * @param type the value type
     * @param nullable whether {@code NULL} is allowed
     * @param primaryKey whether this field is the primary key
     * @param defaultValue the default value, or {@code null}
     */
    public Field(String name, FieldType type, boolean nullable,
                 boolean primaryKey, Cell defaultValue) {
        this(name, type, nullable, primaryKey, defaultValue, null);
    }

    /**
     * Creates a field definition with constraints.
     *
     * @param name the field name
     * @param type the value type
     * @param nullable whether {@code NULL} is allowed
     * @param primaryKey whether this field is the primary key
     * @param defaultValue the default value, or {@code null}
     * @param minimum the minimum numeric value, or {@code null}
     */
    public Field(String name, FieldType type, boolean nullable,
                 boolean primaryKey, Cell defaultValue, Double minimum) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name must not be empty");
        }
        if (primaryKey && nullable) {
            throw new IllegalArgumentException("Primary key field '" + name + "' cannot be nullable");
        }
        if (defaultValue != null && defaultValue.type() != type) {
            throw new IllegalArgumentException(
                "Default value type does not match field '" + name + "'");
        }
        this.name = name;
        this.type = Objects.requireNonNull(type, "type");
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.defaultValue = defaultValue;
        if (minimum != null && type != FieldType.INTEGER && type != FieldType.DOUBLE) {
            throw new IllegalArgumentException("Minimum is supported only for numeric fields");
        }
        this.minimum = minimum;
        if (defaultValue != null) check(defaultValue);
    }

    /**
     * Checks the value type, NULL constraint, and minimum value.
     *
     * @param value the value to check
     */
    public void check(Cell value) {
        Objects.requireNonNull(value, "value");
        if (value.isNull()) {
            if (!nullable) {
                throw new ConstraintViolationException("Field '" + name + "' is NOT NULL");
            }
            return;
        }
        if (value.type() != type) {
            throw new ConstraintViolationException(
                "Field '" + name + "' expects " + type + ", got " + value.type());
        }
        if (minimum != null && (type == FieldType.DOUBLE
                ? value.asDouble() < minimum : value.asInt() < minimum)) {
            throw new ConstraintViolationException(
                "Field '" + name + "' must be at least " + minimum);
        }
    }

    /**
     * Returns the field name.
     *
     * @return the field name
     */
    public String name() { return name; }

    /**
     * Returns the field type.
     *
     * @return the field type
     */
    public FieldType type() { return type; }

    /**
     * Checks whether the field allows NULL.
     *
     * @return {@code true} if the value may be NULL
     */
    public boolean nullable() { return nullable; }

    /**
     * Checks whether the field is the primary key.
     *
     * @return {@code true} if the field is the primary key
     */
    public boolean primaryKey() { return primaryKey; }

    /**
     * Returns the default value.
     *
     * @return the default value or {@code null}
     */
    public Cell defaultValue() { return defaultValue; }

    /**
     * Checks whether a default value is set.
     *
     * @return {@code true} if a default value is set
     */
    public boolean hasDefault() { return defaultValue != null; }

    /** Returns a text description of the field and its constraints. */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(name).append(' ').append(type);
        if (primaryKey) out.append(" PRIMARY KEY");
        if (!nullable) out.append(" NOT NULL");
        if (defaultValue != null) out.append(" DEFAULT ").append(defaultValue.display());
        return out.toString();
    }
}
