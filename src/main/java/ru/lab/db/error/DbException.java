package ru.lab.db.error;

/** Базовая ошибка работы с базой данных. */
public class DbException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DbException(String message) {
        super(message);
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
    }
}
