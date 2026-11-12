package ru.lab.db.lang;

import ru.lab.db.error.ParseException;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Field;
import ru.lab.db.model.FieldType;
import ru.lab.db.model.Reference;
import ru.lab.db.model.Schema;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Разбирает поддерживаемое подмножество SQL в объекты Statement. */
public final class Parser {

    private static final class FieldDefinition {
        private final String name;
        private final FieldType type;
        private boolean nullable = true;
        private boolean primaryKey;
        private Cell defaultValue;

        private FieldDefinition(String name, FieldType type) {
            this.name = name;
            this.type = type;
        }

        private Field toField() {
            return new Field(name, type, nullable, primaryKey, defaultValue);
        }
    }

    private final List<Token> tokens;
    private int index;

    public Parser(String sql) {
        this.tokens = new Lexer(sql).tokenize();
        this.index = 0;
    }

    public Statement parse() {
        if (peek().kind() == Token.Kind.EOF) {
            throw new ParseException("Empty command", 0);
        }
        Statement statement;
        Token token = peek();
        if (isKeyword(token, "INSERT")) {
            statement = parseInsert();
        } else if (isKeyword(token, "SELECT")) {
            statement = parseSelect();
        } else if (isKeyword(token, "UPDATE")) {
            statement = parseUpdate();
        } else if (isKeyword(token, "DELETE")) {
            statement = parseDelete();
        } else if (isKeyword(token, "CREATE")) {
            statement = parseCreate();
        } else if (isKeyword(token, "DROP")) {
            statement = parseDrop();
        } else {
            throw new ParseException("Unknown command '" + token.text() + "'", token.position());
        }
        if (peek().kind() == Token.Kind.SEMICOLON) {
            consume();
        }
        if (peek().kind() != Token.Kind.EOF) {
            throw new ParseException(
                "Unexpected token '" + peek().text() + "'", peek().position());
        }
        return statement;
    }

    private Statement parseInsert() {
        expectKeyword("INSERT");
        expectKeyword("INTO");
        String table = expectIdentifier();
        List<String> columns = new ArrayList<>();
        if (peek().kind() == Token.Kind.LPAREN) {
            consume();
            columns.add(expectIdentifier());
            while (peek().kind() == Token.Kind.COMMA) {
                consume();
                columns.add(expectIdentifier());
            }
            expectKind(Token.Kind.RPAREN, "')'");
        }
        expectKeyword("VALUES");
        List<List<Cell>> rows = new ArrayList<>();
        rows.add(parseRow());
        while (peek().kind() == Token.Kind.COMMA) {
            consume();
            rows.add(parseRow());
        }
        return Statement.insert(table, columns, rows);
    }

    private Statement parseCreate() {
        expectKeyword("CREATE");
        if (isKeyword(peek(), "INDEX")) {
            consume();
            String indexName = expectIdentifier();
            expectKeyword("ON");
            String table = expectIdentifier();
            expectKind(Token.Kind.LPAREN, "'('");
            String field = expectIdentifier();
            expectKind(Token.Kind.RPAREN, "')'");
            return Statement.createIndex(indexName, table, field);
        }
        if (isKeyword(peek(), "TABLE")) {
            return parseCreateTable();
        }
        throw new ParseException("Unsupported CREATE: expected TABLE or INDEX",
            peek().position());
    }

    private Statement parseDrop() {
        expectKeyword("DROP");
        if (isKeyword(peek(), "INDEX")) {
            consume();
            return Statement.dropIndex(expectIdentifier());
        }
        if (isKeyword(peek(), "TABLE")) {
            consume();
            return Statement.dropTable(expectIdentifier());
        }
        throw new ParseException("Unsupported DROP: expected TABLE or INDEX",
            peek().position());
    }

    private Statement parseCreateTable() {
        expectKeyword("TABLE");
        String tableName = expectIdentifier();
        expectKind(Token.Kind.LPAREN, "'('");
        List<FieldDefinition> definitions = new ArrayList<>();
        String primaryKey = null;
        boolean tablePrimaryKey = false;
        do {
            if (isKeyword(peek(), "PRIMARY")) {
                consume();
                expectKeyword("KEY");
                expectKind(Token.Kind.LPAREN, "'('");
                if (tablePrimaryKey || primaryKey != null) {
                    throw new ParseException("Primary key is specified more than once",
                        peek().position());
                }
                primaryKey = expectIdentifier();
                tablePrimaryKey = true;
                if (peek().kind() == Token.Kind.COMMA) {
                    throw new ParseException(
                        "Composite primary key is not supported", peek().position());
                }
                expectKind(Token.Kind.RPAREN, "')'");
            } else {
                FieldDefinition definition = parseColumnDefinition();
                if (definition.primaryKey) {
                    if (primaryKey != null) {
                        throw new ParseException("Primary key is specified more than once",
                            peek().position());
                    }
                    primaryKey = definition.name;
                }
                definitions.add(definition);
            }
            if (peek().kind() != Token.Kind.COMMA) {
                break;
            }
            consume();
        } while (true);
        expectKind(Token.Kind.RPAREN, "')'");
        if (primaryKey == null) {
            throw new ParseException("CREATE TABLE requires a PRIMARY KEY",
                peek().position());
        }

        List<Field> fields = new ArrayList<>(definitions.size());
        boolean primaryKeyFound = false;
        for (FieldDefinition definition : definitions) {
            if (tablePrimaryKey && primaryKey.equals(definition.name)) {
                definition.primaryKey = true;
                definition.nullable = false;
            }
            primaryKeyFound |= primaryKey.equals(definition.name);
            fields.add(definition.toField());
        }
        if (!primaryKeyFound) {
            throw new ParseException("Unknown primary key field '" + primaryKey + "'",
                peek().position());
        }
        try {
            return Statement.createTable(new Schema(
                tableName, fields, primaryKey, Collections.<Reference>emptyList()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ParseException(e.getMessage(), peek().position());
        }
    }

    private FieldDefinition parseColumnDefinition() {
        String name = expectIdentifier();
        FieldDefinition definition = new FieldDefinition(name, parseColumnType());
        while (true) {
            if (isKeyword(peek(), "NOT")) {
                consume();
                expectKeyword("NULL");
                definition.nullable = false;
            } else if (isKeyword(peek(), "NULL")) {
                consume();
                definition.nullable = true;
            } else if (isKeyword(peek(), "PRIMARY")) {
                consume();
                expectKeyword("KEY");
                definition.primaryKey = true;
                definition.nullable = false;
            } else if (isKeyword(peek(), "DEFAULT")) {
                consume();
                Token literal = peek();
                definition.defaultValue = parseDefaultValue(parseLiteral(), definition.type, literal);
            } else {
                return definition;
            }
        }
    }

    private Cell parseDefaultValue(Cell value, FieldType type, Token token) {
        if (value.isNull()) {
            return Cell.nullOf(type);
        }
        if (value.type() == type) {
            return value;
        }
        if (type == FieldType.DOUBLE && value.type() == FieldType.INTEGER) {
            return Cell.ofDouble(value.asInt());
        }
        if (type == FieldType.DATE && value.type() == FieldType.STRING) {
            try {
                return Cell.ofDate(LocalDate.parse(value.asString()));
            } catch (DateTimeException e) {
                throw new ParseException("Invalid DATE default '" + value.asString() + "'",
                    token.position());
            }
        }
        throw new ParseException("Default value does not match type " + type,
            token.position());
    }

    private FieldType parseColumnType() {
        Token token = peek();
        if (token.kind() != Token.Kind.IDENT) {
            throw new ParseException("Expected field type, got '" + token.text() + "'",
                token.position());
        }
        consume();
        String type = token.text().toUpperCase(Locale.ROOT);
        if ("INTEGER".equals(type) || "INT".equals(type)) return FieldType.INTEGER;
        if ("DOUBLE".equals(type) || "FLOAT".equals(type)) return FieldType.DOUBLE;
        if ("STRING".equals(type)) return FieldType.STRING;
        if ("DATE".equals(type)) return FieldType.DATE;
        if ("BOOLEAN".equals(type) || "BOOL".equals(type)) return FieldType.BOOLEAN;
        throw new ParseException("Unsupported field type '" + token.text() + "'",
            token.position());
    }

    private List<Cell> parseRow() {
        expectKind(Token.Kind.LPAREN, "'('");
        List<Cell> values = new ArrayList<>();
        values.add(parseLiteral());
        while (peek().kind() == Token.Kind.COMMA) {
            consume();
            values.add(parseLiteral());
        }
        expectKind(Token.Kind.RPAREN, "')'");
        return values;
    }

    private Statement parseSelect() {
        expectKeyword("SELECT");
        List<String> columns = new ArrayList<>();
        if (peek().kind() == Token.Kind.STAR) {
            consume();
        } else {
            columns.add(expectIdentifier());
            while (peek().kind() == Token.Kind.COMMA) {
                consume();
                columns.add(expectIdentifier());
            }
        }
        expectKeyword("FROM");
        String table = expectIdentifier();
        Condition where = parseOptionalWhere();
        return Statement.select(table, columns, where);
    }

    private Statement parseUpdate() {
        expectKeyword("UPDATE");
        String table = expectIdentifier();
        expectKeyword("SET");
        Map<String, Cell> changes = new LinkedHashMap<>();
        String field = expectIdentifier();
        expectOperator("=");
        changes.put(field, parseLiteral());
        Condition where = parseOptionalWhere();
        return Statement.update(table, changes, where);
    }

    private Statement parseDelete() {
        expectKeyword("DELETE");
        expectKeyword("FROM");
        String table = expectIdentifier();
        Condition where = parseOptionalWhere();
        return Statement.delete(table, where);
    }

    private Condition parseOptionalWhere() {
        if (isKeyword(peek(), "WHERE")) {
            return parseWhereClause();
        }
        return null;
    }

    private Condition parseWhereClause() {
        expectKeyword("WHERE");
        String field = expectIdentifier();
        Token operator = consume();
        if (operator.kind() != Token.Kind.OPERATOR) {
            throw new ParseException(
                "Expected operator, got '" + operator.text() + "'", operator.position());
        }
        Cell literal = parseLiteral();
        try {
            return new Condition(field, operator.text(), literal);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), operator.position());
        }
    }

    private Cell parseLiteral() {
        Token token = peek();
        switch (token.kind()) {
            case INT_LITERAL:
                consume();
                try {
                    return Cell.ofInt(Integer.parseInt(token.text()));
                } catch (NumberFormatException e) {
                    throw new ParseException(
                        "Invalid integer '" + token.text() + "'", token.position());
                }
            case DOUBLE_LITERAL:
                consume();
                try {
                    return Cell.ofDouble(Double.parseDouble(token.text()));
                } catch (NumberFormatException e) {
                    throw new ParseException(
                        "Invalid double '" + token.text() + "'", token.position());
                }
            case STRING_LITERAL:
                consume();
                return Cell.ofString(token.text());
            case BOOLEAN_LITERAL:
                consume();
                return Cell.ofBoolean("TRUE".equals(token.text()));
            case KEYWORD:
                if ("NULL".equals(token.text())) {
                    consume();
                    return Cell.nullOf(FieldType.STRING);
                }
            default:
                throw new ParseException(
                    "Expected literal, got '" + token.text() + "'", token.position());
        }
    }

    private Token peek() {
        return tokens.get(index);
    }

    private Token consume() {
        return tokens.get(index++);
    }

    private void expectKeyword(String keyword) {
        Token token = peek();
        if (!isKeyword(token, keyword)) {
            throw new ParseException(
                "Expected '" + keyword + "', got '" + token.text() + "'", token.position());
        }
        consume();
    }

    private boolean isKeyword(Token token, String keyword) {
        return token.kind() == Token.Kind.KEYWORD && keyword.equals(token.text());
    }

    private String expectIdentifier() {
        Token token = peek();
        if (token.kind() != Token.Kind.IDENT) {
            throw new ParseException(
                "Expected identifier, got '" + token.text() + "'", token.position());
        }
        consume();
        return token.text();
    }

    private void expectOperator(String operator) {
        Token token = consume();
        if (token.kind() != Token.Kind.OPERATOR || !operator.equals(token.text())) {
            throw new ParseException(
                "Expected operator '" + operator + "', got '" + token.text() + "'",
                token.position());
        }
    }

    private void expectKind(Token.Kind kind, String what) {
        Token token = consume();
        if (token.kind() != kind) {
            throw new ParseException(
                "Expected " + what + ", got '" + token.text() + "'", token.position());
        }
    }
}
