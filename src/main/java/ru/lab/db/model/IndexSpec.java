package ru.lab.db.model;

import java.io.Serializable;
import java.util.Objects;

/** Serializable definition of an index on one table field. */
public final class IndexSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Index name. */
    private final String name;
    /** Table name. */
    private final String table;
    /** Indexed field name. */
    private final String field;

    /**
     * Creates an index definition.
     *
     * @param name the index name
     * @param table the table name
     * @param field the indexed field name
     */
    public IndexSpec(String name, String table, String field) {
        this.name = Objects.requireNonNull(name, "name");
        this.table = Objects.requireNonNull(table, "table");
        this.field = Objects.requireNonNull(field, "field");
    }

    /**
     * Returns the index name.
     *
     * @return the index name
     */
    public String name() { return name; }

    /**
     * Returns the table name.
     *
     * @return the table name
     */
    public String table() { return table; }

    /**
     * Returns the indexed field name.
     *
     * @return the field name
     */
    public String field() { return field; }
}
