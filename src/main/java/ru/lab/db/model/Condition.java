package ru.lab.db.model;

import ru.lab.db.error.ConstraintViolationException;

import java.util.Objects;

/** Условие сравнения значения поля с литералом. */
public final class Condition {

    public static final String EQ = "=";
    public static final String NEQ = "!=";
    public static final String LT = "<";
    public static final String LTE = "<=";
    public static final String GT = ">";
    public static final String GTE = ">=";

    private final String field;
    private final String operator;
    private final Cell literal;

    public Condition(String field, String operator, Cell literal) {
        this.field = Objects.requireNonNull(field, "field");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.literal = Objects.requireNonNull(literal, "literal");
        if (!supported(operator)) {
            throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

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

    public String field() { return field; }
    public String operator() { return operator; }
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

    @Override
    public String toString() {
        return field + " " + operator + " " + literal.display();
    }
}
