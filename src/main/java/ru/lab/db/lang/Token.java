package ru.lab.db.lang;

/** A token from an SQL command, including its source position. */
public final class Token {

    /** Lexical token categories. */
    public enum Kind {
        /** A table or field identifier. */
        IDENT,
        /** A reserved word. */
        KEYWORD,
        /** An integer literal. */
        INT_LITERAL,
        /** A floating-point literal. */
        DOUBLE_LITERAL,
        /** A string literal. */
        STRING_LITERAL,
        /** A boolean literal. */
        BOOLEAN_LITERAL,
        /** A comparison operator. */
        OPERATOR,
        /** An item separator. */
        COMMA,
        /** An opening parenthesis. */
        LPAREN,
        /** A closing parenthesis. */
        RPAREN,
        /** The symbol for selecting all fields. */
        STAR,
        /** A statement terminator. */
        SEMICOLON,
        /** The end of the command. */
        EOF
    }

    /** Token category. */
    private final Kind kind;
    /** Token source text. */
    private final String text;
    /** Token position in the command. */
    private final int position;

    /**
     * Creates a lexical token.
     *
     * @param kind the token kind
     * @param text the token text
     * @param position the position in the command
     */
    public Token(Kind kind, String text, int position) {
        this.kind = kind;
        this.text = text;
        this.position = position;
    }

    /**
     * Returns the token category.
     *
     * @return the token category
     */
    public Kind kind() { return kind; }

    /**
     * Returns the token text.
     *
     * @return the token text
     */
    public String text() { return text; }

    /**
     * Returns the token position.
     *
     * @return the position in the source command
     */
    public int position() { return position; }

    /** Returns the token and its position as text. */
    @Override
    public String toString() {
        return kind + "(" + text + ")@" + position;
    }
}
