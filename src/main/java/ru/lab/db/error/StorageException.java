package ru.lab.db.error;

/** Ошибка чтения или записи файла базы данных. */
public class StorageException extends DbException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
