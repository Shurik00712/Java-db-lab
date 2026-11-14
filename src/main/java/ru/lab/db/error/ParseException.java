package ru.lab.db.error;

/** Indicates a syntax error and its position in an SQL command. */
public class ParseException extends DbException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a parse exception with a source position.
     *
     * @param message the error message
     * @param position the position in the source command
     */
    public ParseException(String message, int position) {
        super(message + " at position " + position);
    }
}
