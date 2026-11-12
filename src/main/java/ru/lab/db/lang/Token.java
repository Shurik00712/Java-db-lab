package ru.lab.db.lang;

/** Лексическая единица SQL-команды с исходной позицией. */
public final class Token {

    public enum Kind {
        IDENT, KEYWORD, INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL,
        BOOLEAN_LITERAL, OPERATOR, COMMA, LPAREN, RPAREN, STAR, SEMICOLON, EOF
    }

    private final Kind kind;
    private final String text;
    private final int position;

    public Token(Kind kind, String text, int position) {
        this.kind = kind;
        this.text = text;
        this.position = position;
    }

    public Kind kind() { return kind; }
    public String text() { return text; }
    public int position() { return position; }

    @Override
    public String toString() {
        return kind + "(" + text + ")@" + position;
    }
}
