package ru.lab.db.model;

import java.io.Serializable;

/** Связывает поле таблицы с первичным ключом другой таблицы. */
public final class Reference implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String field;
    private final String targetTable;
    private final String targetField;

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

    public String field() { return field; }
    public String targetTable() { return targetTable; }
    public String targetField() { return targetField; }

    @Override
    public String toString() {
        return field + " -> " + targetTable + "(" + targetField + ")";
    }
}
