package ru.lab.db.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Типизированное значение поля, включая типизированный NULL. */
public final class Cell implements Comparable<Cell>, Serializable {
    private static final long serialVersionUID = 1L;

    private final FieldType type;
    private final Object raw;

    private Cell(FieldType type, Object raw) {
        this.type = Objects.requireNonNull(type, "type");
        this.raw = raw;
    }

    public static Cell ofInt(int value) {
        return new Cell(FieldType.INTEGER, value);
    }

    public static Cell ofDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("DOUBLE must be finite: " + value);
        }
        return new Cell(FieldType.DOUBLE, value);
    }

    public static Cell ofString(String value) {
        return new Cell(FieldType.STRING, Objects.requireNonNull(value, "value"));
    }

    public static Cell ofDate(LocalDate value) {
        return new Cell(FieldType.DATE, Objects.requireNonNull(value, "value"));
    }

    public static Cell ofBoolean(boolean value) {
        return new Cell(FieldType.BOOLEAN, value);
    }

    public static Cell nullOf(FieldType type) {
        return new Cell(type, null);
    }

    public FieldType type() { return type; }
    public boolean isNull() { return raw == null; }

    public int asInt() {
        requireType(FieldType.INTEGER);
        return (Integer) raw;
    }

    public double asDouble() {
        requireType(FieldType.DOUBLE);
        return (Double) raw;
    }

    public String asString() {
        requireType(FieldType.STRING);
        return (String) raw;
    }

    public LocalDate asDate() {
        requireType(FieldType.DATE);
        return (LocalDate) raw;
    }

    public boolean asBoolean() {
        requireType(FieldType.BOOLEAN);
        return (Boolean) raw;
    }

    private void requireType(FieldType expected) {
        if (isNull()) {
            throw new IllegalStateException("Cannot read NULL as " + expected);
        }
        if (type != expected) {
            throw new IllegalStateException("Value type is " + type + ", not " + expected);
        }
    }

    @Override
    public int compareTo(Cell other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot compare with null");
        }
        if (type != other.type) {
            throw new IllegalArgumentException("Cannot compare " + type + " with " + other.type);
        }
        if (raw == null && other.raw == null) return 0;
        if (raw == null) return -1;
        if (other.raw == null) return 1;
        switch (type) {
            case INTEGER: return Integer.compare((Integer) raw, (Integer) other.raw);
            case DOUBLE:  return Double.compare((Double) raw, (Double) other.raw);
            case STRING:  return ((String) raw).compareTo((String) other.raw);
            case DATE:    return ((LocalDate) raw).compareTo((LocalDate) other.raw);
            case BOOLEAN: return Boolean.compare((Boolean) raw, (Boolean) other.raw);
            default: throw new IllegalStateException("Unknown type: " + type);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Cell cell = (Cell) other;
        return type == cell.type && Objects.equals(raw, cell.raw);
    }

    @Override
    public int hashCode() {
        return 31 * type.hashCode() + Objects.hashCode(raw);
    }

    /** Текстовое представление для вывода в консоли. */
    public String display() {
        if (raw == null) return "NULL";
        return raw.toString();
    }

    @Override
    public String toString() {
        return display();
    }
}
