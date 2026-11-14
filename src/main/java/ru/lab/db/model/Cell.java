package ru.lab.db.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** A typed field value, including a typed NULL. */
public final class Cell implements Comparable<Cell>, Serializable {
    private static final long serialVersionUID = 1L;

    /** Value type. */
    private final FieldType type;
    /** Raw value, or {@code null} for NULL. */
    private final Object raw;

    private Cell(FieldType type, Object raw) {
        this.type = Objects.requireNonNull(type, "type");
        this.raw = raw;
    }

    /**
     * Creates an integer value.
     *
     * @param value the integer value
     * @return the typed value
     */
    public static Cell ofInt(int value) {
        return new Cell(FieldType.INTEGER, value);
    }

    /**
     * Creates a finite floating-point value.
     *
     * @param value the finite numeric value
     * @return the typed value
     */
    public static Cell ofDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("DOUBLE must be finite: " + value);
        }
        return new Cell(FieldType.DOUBLE, value);
    }

    /**
     * Creates a string value.
     *
     * @param value the string
     * @return the typed value
     */
    public static Cell ofString(String value) {
        return new Cell(FieldType.STRING, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a date value.
     *
     * @param value the date
     * @return the typed value
     */
    public static Cell ofDate(LocalDate value) {
        return new Cell(FieldType.DATE, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a boolean value.
     *
     * @param value the boolean value
     * @return the typed value
     */
    public static Cell ofBoolean(boolean value) {
        return new Cell(FieldType.BOOLEAN, value);
    }

    /**
     * Creates a NULL value of the given type.
     *
     * @param type the field type
     * @return the typed NULL value
     */
    public static Cell nullOf(FieldType type) {
        return new Cell(type, null);
    }

    /**
     * Returns the value type.
     *
     * @return the value type
     */
    public FieldType type() { return type; }

    /**
     * Checks whether the value is NULL.
     *
     * @return {@code true} if the value is NULL
     */
    public boolean isNull() { return raw == null; }

    /**
     * Returns the value as an integer.
     *
     * @return the integer value
     */
    public int asInt() {
        requireType(FieldType.INTEGER);
        return (Integer) raw;
    }

    /**
     * Returns the value as a floating-point number.
     *
     * @return the numeric value
     */
    public double asDouble() {
        requireType(FieldType.DOUBLE);
        return (Double) raw;
    }

    /**
     * Returns the value as a string.
     *
     * @return the string value
     */
    public String asString() {
        requireType(FieldType.STRING);
        return (String) raw;
    }

    /**
     * Returns the value as a date.
     *
     * @return the date value
     */
    public LocalDate asDate() {
        requireType(FieldType.DATE);
        return (LocalDate) raw;
    }

    /**
     * Returns the value as a boolean.
     *
     * @return the boolean value
     */
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

    /**
     * Compares values of the same type.
     *
     * @param other the value to compare
     * @return a negative number, zero, or a positive number
     */
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

    /**
     * Checks whether the type and value are equal.
     *
     * @param other the object to compare
     * @return {@code true} if the values are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Cell cell = (Cell) other;
        return type == cell.type && Objects.equals(raw, cell.raw);
    }

    /** Returns the value's hash code. */
    @Override
    public int hashCode() {
        return 31 * type.hashCode() + Objects.hashCode(raw);
    }

    /**
     * Returns the value as text for console output.
     *
     * @return the value as text or {@code NULL}
     */
    public String display() {
        if (raw == null) return "NULL";
        return raw.toString();
    }

    /** Returns the {@link #display() text representation} of the value. */
    @Override
    public String toString() {
        return display();
    }
}
