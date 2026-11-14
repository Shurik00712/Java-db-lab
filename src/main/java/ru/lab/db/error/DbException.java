package ru.lab.db.error;

/** Base exception for database errors. */
public class DbException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a database exception with a message.
     *
     * @param message the error message
     */
    public DbException(String message) {
        super(message);
    }

    /**
     * Creates a database exception with a cause.
     *
     * @param message the error message
     * @param cause the original cause
     */
    public DbException(String message, Throwable cause) {
        super(message, cause);
    }
}
