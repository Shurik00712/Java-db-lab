package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable set of values for one table record. */
public final class Record implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Record field values. */
    private final Map<String, Cell> values;

    /**
     * Creates a record from field values.
     *
     * @param values the field values
     */
    public Record(Map<String, Cell> values) {
        this.values = new LinkedHashMap<>(Objects.requireNonNull(values, "values"));
    }

    /**
     * Returns a field value.
     *
     * @param field the field name
     * @return the field value
     */
    public Cell get(String field) {
        Cell value = values.get(field);
        if (value == null) {
            throw new ConstraintViolationException("Unknown field '" + field + "'");
        }
        return value;
    }

    /**
     * Returns a copy of the record values.
     *
     * @return the field values
     */
    public Map<String, Cell> values() {
        return new LinkedHashMap<>(values);
    }

    /**
     * Checks whether the record values are equal.
     *
     * @param other the object to compare
     * @return {@code true} if the values are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return values.equals(((Record) other).values);
    }

    /** Returns the record's hash code. */
    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /** Returns the record as text. */
    @Override
    public String toString() {
        return values.toString();
    }
}
