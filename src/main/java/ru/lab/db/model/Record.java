package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Неизменяемый набор значений одной записи таблицы. */
public final class Record implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Cell> values;

    public Record(Map<String, Cell> values) {
        this.values = new LinkedHashMap<>(Objects.requireNonNull(values, "values"));
    }

    public Cell get(String field) {
        Cell value = values.get(field);
        if (value == null) {
            throw new ConstraintViolationException("Unknown field '" + field + "'");
        }
        return value;
    }

    public Map<String, Cell> values() {
        return new LinkedHashMap<>(values);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        return values.equals(((Record) other).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
