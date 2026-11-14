package ru.lab.db.error;

/** Indicates a database file read or write error. */
public class StorageException extends DbException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a storage exception with its original cause.
     *
     * @param message the error message
     * @param cause the original cause
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
