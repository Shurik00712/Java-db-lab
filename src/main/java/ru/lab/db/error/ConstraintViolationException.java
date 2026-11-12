package ru.lab.db.error;

/** Нарушение ограничения схемы или целостности данных. */
public class ConstraintViolationException extends DbException {
    private static final long serialVersionUID = 1L;

    public ConstraintViolationException(String message) {
        super(message);
    }
}
