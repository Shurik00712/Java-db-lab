package ru.lab.db.model;

import java.io.Serializable;

/** Links a table field to another table's primary key. */
public final class Reference implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Reference field in the current table. */
    private final String field;
    /** Target table. */
    private final String targetTable;
    /** Target field of the other table. */
    private final String targetField;

    /**
     * Creates a reference to a field in another table.
     *
     * @param field the local field name
     * @param targetTable the referenced table name
     * @param targetField the referenced field name
     */
    public Reference(String field, String targetTable, String targetField) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference field must not be empty");
        }
        if (targetTable == null || targetTable.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference target table must not be empty");
        }
        if (targetField == null || targetField.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference target field must not be empty");
        }
        this.field = field;
        this.targetTable = targetTable;
        this.targetField = targetField;
    }

    /**
     * Returns the reference field name.
     *
     * @return the reference field name
     */
    public String field() { return field; }

    /**
     * Returns the target table name.
     *
     * @return the target table name
     */
    public String targetTable() { return targetTable; }

    /**
     * Returns the target field name.
     *
     * @return the target field name
     */
    public String targetField() { return targetField; }

    /** Returns the reference as text. */
    @Override
    public String toString() {
        return field + " -> " + targetTable + "(" + targetField + ")";
    }
}
