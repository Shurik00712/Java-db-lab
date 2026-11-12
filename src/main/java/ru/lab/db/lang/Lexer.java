package ru.lab.db.lang;

import ru.lab.db.error.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Разбивает SQL-команду на токены. */
public final class Lexer {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "INSERT", "INTO", "VALUES", "SELECT", "FROM", "WHERE",
        "UPDATE", "SET", "DELETE", "CREATE", "INDEX", "ON", "DROP",
        "TABLE", "PRIMARY", "KEY", "NOT", "NULL", "DEFAULT"
    ));

    private final String source;
    private int position;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Token token = next();
            tokens.add(token);
            if (token.kind() == Token.Kind.EOF) return tokens;
        }
    }

    private Token next() {
        skipWhitespace();
        if (position >= source.length()) {
            return new Token(Token.Kind.EOF, "", position);
        }
        char ch = source.charAt(position);
        if (ch == '\'') return readString();
        if (Character.isDigit(ch)) return readNumber(false);
        if (ch == '-' && position + 1 < source.length()
                && Character.isDigit(source.charAt(position + 1))) {
            return readNumber(true);
        }
        if (Character.isLetter(ch) || ch == '_') return readIdentifier();
        if (ch == '=') {
            position++;
            return new Token(Token.Kind.OPERATOR, "=", position - 1);
        }
        if (ch == '!') {
            if (position + 1 < source.length() && source.charAt(position + 1) == '=') {
                position += 2;
                return new Token(Token.Kind.OPERATOR, "!=", position - 2);
            }
            throw new ParseException("Expected '=' after '!'", position);
        }
        if (ch == '<') {
            if (position + 1 < source.length() && source.charAt(position + 1) == '=') {
                position += 2;
                return new Token(Token.Kind.OPERATOR, "<=", position - 2);
            }
            position++;
            return new Token(Token.Kind.OPERATOR, "<", position - 1);
        }
        if (ch == '>') {
            if (position + 1 < source.length() && source.charAt(position + 1) == '=') {
                position += 2;
                return new Token(Token.Kind.OPERATOR, ">=", position - 2);
            }
            position++;
            return new Token(Token.Kind.OPERATOR, ">", position - 1);
        }
        if (ch == ',') { position++; return new Token(Token.Kind.COMMA, ",", position - 1); }
        if (ch == '(') { position++; return new Token(Token.Kind.LPAREN, "(", position - 1); }
        if (ch == ')') { position++; return new Token(Token.Kind.RPAREN, ")", position - 1); }
        if (ch == '*') { position++; return new Token(Token.Kind.STAR, "*", position - 1); }
        if (ch == ';') { position++; return new Token(Token.Kind.SEMICOLON, ";", position - 1); }
        throw new ParseException("Unexpected character '" + ch + "'", position);
    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }

    private Token readString() {
        int start = position;
        position++;
        StringBuilder value = new StringBuilder();
        while (position < source.length()) {
            char ch = source.charAt(position);
            if (ch == '\'') {
                if (position + 1 < source.length() && source.charAt(position + 1) == '\'') {
                    value.append('\'');
                    position += 2;
                } else {
                    position++;
                    return new Token(Token.Kind.STRING_LITERAL, value.toString(), start);
                }
            } else {
                value.append(ch);
                position++;
            }
        }
        throw new ParseException("Unterminated string literal", start);
    }

    private Token readNumber(boolean negative) {
        int start = position;
        if (negative) position++;
        while (position < source.length() && Character.isDigit(source.charAt(position))) {
            position++;
        }
        boolean isDouble = false;
        if (position < source.length() && source.charAt(position) == '.') {
            if (position + 1 >= source.length()
                    || !Character.isDigit(source.charAt(position + 1))) {
                throw new ParseException("Expected digit after '.'", position);
            }
            isDouble = true;
            position++;
            while (position < source.length() && Character.isDigit(source.charAt(position))) {
                position++;
            }
        }
        String text = source.substring(start, position);
        return new Token(
            isDouble ? Token.Kind.DOUBLE_LITERAL : Token.Kind.INT_LITERAL, text, start);
    }

    private Token readIdentifier() {
        int start = position;
        while (position < source.length()
                && (Character.isLetterOrDigit(source.charAt(position))
                    || source.charAt(position) == '_')) {
            position++;
        }
        String text = source.substring(start, position);
        String upper = text.toUpperCase(Locale.ROOT);
        if ("TRUE".equals(upper) || "FALSE".equals(upper)) {
            return new Token(Token.Kind.BOOLEAN_LITERAL, upper, start);
        }
        if (KEYWORDS.contains(upper)) {
            return new Token(Token.Kind.KEYWORD, upper, start);
        }
        return new Token(Token.Kind.IDENT, text.toLowerCase(Locale.ROOT), start);
    }
}
