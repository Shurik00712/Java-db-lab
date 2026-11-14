package ru.lab.db.error;

/** Indicates a schema constraint or data integrity violation. */
public class ConstraintViolationException extends DbException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a constraint violation exception.
     *
     * @param message the violation message
     */
    public ConstraintViolationException(String message) {
        super(message);
    }
}
