package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;
import ru.lab.db.error.DbException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Stores tables and index definitions and checks references between tables. */
public final class Database implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Database tables by name. */
    private final Map<String, Table> tables = new LinkedHashMap<>();
    /** Definitions of the created indexes. */
    private final List<IndexSpec> indexes = new ArrayList<>();

    /** Creates an empty database. */
    public Database() {
    }

    /**
     * Adds a table.
     *
     * @param table the table to add
     */
    public void add(Table table) {
        Objects.requireNonNull(table, "table");
        String name = table.schema().name();
        if (tables.containsKey(name)) {
            throw new IllegalArgumentException("Table '" + name + "' already exists");
        }
        tables.put(name, table);
    }

    /**
     * Removes a table and its indexes if no other table references it.
     *
     * @param name the table name
     */
    public void drop(String name) {
        table(name);
        for (Table child : tables.values()) {
            if (child.schema().name().equals(name)) continue;
            for (Reference reference : child.schema().references()) {
                if (name.equals(reference.targetTable())) {
                    throw new ConstraintViolationException(
                        "Cannot drop table '" + name + "': table '" + child.schema().name()
                        + "' references it");
                }
            }
        }
        tables.remove(name);
        Iterator<IndexSpec> iterator = indexes.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().table().equals(name)) iterator.remove();
        }
    }

    /**
     * Returns a table by name.
     *
     * @param name the table name
     * @return the table
     */
    public Table table(String name) {
        Table found = tables.get(name);
        if (found == null) {
            throw new DbException("Unknown table '" + name + "'");
        }
        return found;
    }

    /**
     * Checks whether a table exists.
     *
     * @param name the table name
     * @return {@code true} if the table exists
     */
    public boolean has(String name) {
        return tables.containsKey(name);
    }

    /**
     * Returns the tables in this database.
     *
     * @return the database tables
     */
    public Collection<Table> tables() {
        return tables.values();
    }

    /**
     * Returns the index definitions.
     *
     * @return the index definitions
     */
    public List<IndexSpec> indexes() {
        return indexes;
    }

    /**
     * Adds an index definition.
     *
     * @param spec the index definition
     */
    public void addIndex(IndexSpec spec) {
        indexes.add(Objects.requireNonNull(spec, "spec"));
    }

    /**
     * Removes an index definition by name.
     *
     * @param name the index name
     */
    public void removeIndex(String name) {
        Iterator<IndexSpec> iterator = indexes.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().name().equals(name)) iterator.remove();
        }
    }

    /** Checks references declared by all tables. */
    public void checkReferences() {
        for (Table table : tables.values()) {
            checkReferences(table);
        }
    }

    /**
     * Checks the references declared by a table schema.
     *
     * @param table the table to check
     */
    public void checkReferences(Table table) {
        for (Reference reference : table.schema().references()) {
            Table target = table.schema().name().equals(reference.targetTable())
                ? table : tables.get(reference.targetTable());
            if (target == null) {
                throw new IllegalStateException(
                    "Reference " + table.schema().name() + "." + reference.field()
                    + " points to unknown table '" + reference.targetTable() + "'");
            }
            if (!target.schema().hasField(reference.targetField())) {
                throw new IllegalStateException(
                    "Reference points to unknown field '" + reference.targetField() + "'");
            }
            if (!reference.targetField().equals(target.schema().primaryKey())) {
                throw new IllegalStateException("Reference must point to the primary key");
            }
            Field local = table.schema().field(reference.field());
            Field remote = target.schema().field(reference.targetField());
            if (local.type() != remote.type()) {
                throw new IllegalStateException(
                    "Reference field '" + reference.field() + "' in table '"
                    + table.schema().name() + "' has type " + local.type()
                    + " but the referenced primary key has type " + remote.type());
            }
        }
    }

    /**
     * Checks the foreign keys of a new record.
     *
     * @param table the table receiving the record
     * @param record the record to insert
     */
    public void checkInsert(Table table, Record record) {
        for (Reference reference : table.schema().references()) {
            Cell value = record.get(reference.field());
            if (value.isNull()) continue;
            Table target = targetOf(reference);
            if (target.findByPk(value) == null) {
                throw new ConstraintViolationException(
                    "Foreign key violation: " + table.schema().name() + "."
                    + reference.field() + " = " + value.display()
                    + " does not exist in " + reference.targetTable());
            }
        }
    }

    /**
     * Checks foreign keys and whether the primary key can be changed.
     *
     * @param table the table being updated
     * @param before the original record
     * @param after the updated record
     */
    public void checkUpdate(Table table, Record before, Record after) {
        for (Reference reference : table.schema().references()) {
            Cell oldValue = before.get(reference.field());
            Cell newValue = after.get(reference.field());
            if (oldValue.equals(newValue) || newValue.isNull()) continue;
            Table target = targetOf(reference);
            if (target.findByPk(newValue) == null) {
                throw new ConstraintViolationException(
                    "Foreign key violation on UPDATE: " + table.schema().name() + "."
                    + reference.field() + " = " + newValue.display()
                    + " does not exist in " + reference.targetTable());
            }
        }
        String primaryKey = table.schema().primaryKey();
        Cell oldKey = before.get(primaryKey);
        Cell newKey = after.get(primaryKey);
        if (!oldKey.equals(newKey)) {
            String tableName = table.schema().name();
            for (Table child : tables.values()) {
                for (Reference reference : child.schema().references()) {
                    if (reference.targetTable().equals(tableName)
                            && reference.targetField().equals(primaryKey)
                            && child.containsValue(reference.field(), oldKey)) {
                        throw new ConstraintViolationException(
                            "Cannot change primary key in '" + tableName
                            + "': table '" + child.schema().name() + "' still references "
                            + primaryKey + " = " + oldKey.display());
                    }
                }
            }
        }
    }

    /**
     * Checks whether other records reference records to be deleted.
     *
     * @param table the table containing records to delete
     * @param records the records to delete
     */
    public void checkDelete(Table table, Collection<Record> records) {
        String tableName = table.schema().name();
        for (Table child : tables.values()) {
            for (Reference reference : child.schema().references()) {
                if (!reference.targetTable().equals(tableName)) continue;
                for (Record record : records) {
                    Cell referenced = record.get(reference.targetField());
                    if (child.containsValue(reference.field(), referenced)) {
                        throw new ConstraintViolationException(
                            "Cannot delete from '" + tableName + "': table '"
                            + child.schema().name() + "' still references "
                            + reference.targetField() + " = " + referenced.display());
                    }
                }
            }
        }
    }

    private Table targetOf(Reference reference) {
        Table target = tables.get(reference.targetTable());
        if (target == null) {
            throw new IllegalStateException(
                "Reference points to unknown table '" + reference.targetTable() + "'");
        }
        return target;
    }
}
