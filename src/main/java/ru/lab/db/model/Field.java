package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.io.Serializable;
import java.util.Objects;

/** Описание одного поля таблицы и его ограничений. */
public final class Field implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final FieldType type;
    private final boolean nullable;
    private final boolean primaryKey;
    private final Cell defaultValue;
    private final Double minimum;

    public Field(String name, FieldType type, boolean nullable,
                 boolean primaryKey, Cell defaultValue) {
        this(name, type, nullable, primaryKey, defaultValue, null);
    }

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

    /** Проверяет тип значения и допустимость NULL. */
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

    public String name() { return name; }
    public FieldType type() { return type; }
    public boolean nullable() { return nullable; }
    public boolean primaryKey() { return primaryKey; }
    public Cell defaultValue() { return defaultValue; }
    public boolean hasDefault() { return defaultValue != null; }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(name).append(' ').append(type);
        if (primaryKey) out.append(" PRIMARY KEY");
        if (!nullable) out.append(" NOT NULL");
        if (defaultValue != null) out.append(" DEFAULT ").append(defaultValue.display());
        return out.toString();
    }
}
