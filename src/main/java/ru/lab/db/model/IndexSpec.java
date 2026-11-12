package ru.lab.db.model;

import java.io.Serializable;
import java.util.Objects;

/** Сохраняемое описание индекса по одному полю таблицы. */
public final class IndexSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String table;
    private final String field;

    public IndexSpec(String name, String table, String field) {
        this.name = Objects.requireNonNull(name, "name");
        this.table = Objects.requireNonNull(table, "table");
        this.field = Objects.requireNonNull(field, "field");
    }

    public String name() { return name; }
    public String table() { return table; }
    public String field() { return field; }
}
