package ru.lab.db.error;

/** Ошибка разбора SQL-команды с позицией проблемного токена. */
public class ParseException extends DbException {
    private static final long serialVersionUID = 1L;

    public ParseException(String message, int position) {
        super(message + " at position " + position);
    }
}
