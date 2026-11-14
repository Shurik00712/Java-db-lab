package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.util.Objects;

/** Compares a field value with a literal. */
public final class Condition {

    /** Equality operator. */
    public static final String EQ = "=";
    /** Inequality operator. */
    public static final String NEQ = "!=";
    /** Less-than operator. */
    public static final String LT = "<";
    /** Less-than-or-equal operator. */
    public static final String LTE = "<=";
    /** Greater-than operator. */
    public static final String GT = ">";
    /** Greater-than-or-equal operator. */
    public static final String GTE = ">=";

    private final String field;
    private final String operator;
    private final Cell literal;

    /**
     * Creates a condition that compares a field with a literal.
     *
     * @param field the field name
     * @param operator the comparison operator
     * @param literal the value to compare
     */
    public Condition(String field, String operator, Cell literal) {
        this.field = Objects.requireNonNull(field, "field");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.literal = Objects.requireNonNull(literal, "literal");
        if (!supported(operator)) {
            throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    /**
     * Checks whether this condition is compatible with a field definition.
     *
     * @param fieldDef the field definition to check
     */
    public void check(Field fieldDef) {
        if (!literal.isNull() && literal.type() != fieldDef.type()) {
            throw new ConstraintViolationException(
                "Cannot compare field '" + field + "' of type " + fieldDef.type()
                + " with literal of type " + literal.type());
        }
        if (fieldDef.type() == FieldType.BOOLEAN
                && !EQ.equals(operator) && !NEQ.equals(operator)) {
            throw new ConstraintViolationException(
                "Boolean field '" + field + "' supports only '=' and '!='");
        }
    }

    /**
     * Checks whether a record matches this condition.
     *
     * @param record the record to check
     * @return {@code true} if the record matches
     */
    public boolean test(Record record) {
        Cell actual = record.get(field);
        if (actual.isNull() || literal.isNull()) return false;
        int cmp = actual.compareTo(literal);
        switch (operator) {
            case EQ:  return cmp == 0;
            case NEQ: return cmp != 0;
            case LT:  return cmp < 0;
            case LTE: return cmp <= 0;
            case GT:  return cmp > 0;
            case GTE: return cmp >= 0;
            default: throw new IllegalStateException("Unknown operator: " + operator);
        }
    }

    /**
     * Returns the condition field name.
     *
     * @return the field name
     */
    public String field() { return field; }

    /**
     * Returns the comparison operator.
     *
     * @return the comparison operator
     */
    public String operator() { return operator; }

    /**
     * Returns the condition literal.
     *
     * @return the value to compare with
     */
    public Cell literal() { return literal; }

    private static boolean supported(String operator) {
        switch (operator) {
            case EQ:
            case NEQ:
            case LT:
            case LTE:
            case GT:
            case GTE:
                return true;
            default:
                return false;
        }
    }

    /** Returns this condition as text. */
    @Override
    public String toString() {
        return field + " " + operator + " " + literal.display();
    }
}
