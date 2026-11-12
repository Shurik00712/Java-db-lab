package ru.lab.db;

import org.junit.Test;
import ru.lab.db.error.ParseException;
import ru.lab.db.lang.Parser;
import ru.lab.db.lang.Statement;
import ru.lab.db.model.FieldType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    @Test
    public void parsesInsertWithoutColumns() {
        Statement s = new Parser("INSERT INTO employers VALUES (1, 'Acme', TRUE);").parse();
        assertEquals(Statement.Kind.INSERT, s.kind());
        assertEquals("employers", s.table());
        assertTrue(s.columns().isEmpty());
        assertEquals(1, s.rows().size());
        assertEquals(3, s.rows().get(0).size());
    }

    @Test
    public void parsesMultiRowInsert() {
        Statement s = new Parser(
            "INSERT INTO vacancies (id, title, employer_id) "
            + "VALUES (1, 'A', 10), (2, 'B', 10);").parse();
        assertEquals(2, s.rows().size());
        assertEquals(3, s.columns().size());
    }

    @Test
    public void parsesSelectWithWhere() {
        Statement s = new Parser(
            "SELECT title, salary FROM vacancies WHERE salary > 100000;").parse();
        assertEquals(Statement.Kind.SELECT, s.kind());
        assertEquals("vacancies", s.table());
        assertEquals(2, s.columns().size());
        assertNotNull(s.where());
        assertEquals("salary", s.where().field());
    }

    @Test
    public void parsesSelectStar() {
        Statement s = new Parser("SELECT * FROM employers;").parse();
        assertTrue(s.columns().isEmpty());
        assertNull(s.where());
    }

    @Test
    public void parsesUpdate() {
        Statement s = new Parser(
            "UPDATE vacancies SET salary = 130000.0 WHERE id = 1;").parse();
        assertEquals(Statement.Kind.UPDATE, s.kind());
        assertEquals(1, s.assignments().size());
        assertNotNull(s.where());
    }

    @Test
    public void parsesDelete() {
        Statement s = new Parser(
            "DELETE FROM vacancies WHERE title = 'Java';").parse();
        assertEquals(Statement.Kind.DELETE, s.kind());
        assertEquals("vacancies", s.table());
        assertNotNull(s.where());
    }

    @Test
    public void parsesUpdateWithoutWhereAndNullValue() {
        Statement s = new Parser("UPDATE VACANCIES SET title = NULL;").parse();
        assertEquals(Statement.Kind.UPDATE, s.kind());
        assertNull(s.where());
        assertTrue(s.assignments().get("title").isNull());
    }

    @Test
    public void parsesDeleteWithoutWhere() {
        Statement s = new Parser("DELETE FROM VACANCIES;").parse();
        assertEquals(Statement.Kind.DELETE, s.kind());
        assertNull(s.where());
    }

    @Test
    public void identifiersAreCaseInsensitive() {
        Statement s = new Parser(
            "SELECT TITLE FROM VACANCIES WHERE ID = 1;").parse();
        assertEquals("vacancies", s.table());
        assertEquals("title", s.columns().get(0));
        assertEquals("id", s.where().field());
    }

    @Test
    public void parsesNullLiteral() {
        Statement s = new Parser(
            "INSERT INTO notes (id, message) VALUES (1, NULL);").parse();
        assertTrue(s.rows().get(0).get(1).isNull());
    }

    @Test(expected = ParseException.class)
    public void rejectsTrailingTokens() {
        new Parser("SELECT * FROM employers garbage;").parse();
    }

    @Test(expected = ParseException.class)
    public void rejectsEmpty() {
        new Parser("   ").parse();
    }

    @Test
    public void handlesEscapedQuote() {
        Statement s = new Parser(
            "INSERT INTO employers VALUES (1, 'O''Brien', FALSE);").parse();
        assertEquals("O'Brien", s.rows().get(0).get(1).asString());
    }

    @Test
    public void keywordsAreCaseInsensitive() {
        Statement s = new Parser("select * from employers;").parse();
        assertEquals("employers", s.table());
    }

    @Test
    public void parsesCreateTableColumnAndTablePrimaryKeys() {
        Statement s = new Parser(
            "CREATE TABLE people (id INTEGER, name STRING NOT NULL, "
            + "active BOOLEAN DEFAULT TRUE, PRIMARY KEY (id));").parse();

        assertEquals(Statement.Kind.CREATE_TABLE, s.kind());
        assertEquals("people", s.schema().name());
        assertEquals("id", s.schema().primaryKey());
        assertEquals(FieldType.INTEGER, s.schema().field("id").type());
        assertEquals(false, s.schema().field("id").nullable());
        assertEquals(false, s.schema().field("name").nullable());
        assertEquals(true, s.schema().field("active").defaultValue().asBoolean());
    }

    @Test
    public void parsesCreateTableWithColumnPrimaryKeyAndDateDefault() {
        Statement s = new Parser(
            "CREATE TABLE events (id INT PRIMARY KEY, happened DATE "
            + "DEFAULT '2026-01-02', amount DOUBLE DEFAULT 1);").parse();

        assertEquals("id", s.schema().primaryKey());
        assertEquals("2026-01-02",
            s.schema().field("happened").defaultValue().asDate().toString());
        assertEquals(1.0, s.schema().field("amount").defaultValue().asDouble(), 0.0001);
    }

    @Test(expected = ParseException.class)
    public void rejectsCreateTableWithoutPrimaryKey() {
        new Parser("CREATE TABLE notes (text STRING);").parse();
    }

    @Test
    public void parsesDropTable() {
        Statement s = new Parser("DROP TABLE people;").parse();
        assertEquals(Statement.Kind.DROP_TABLE, s.kind());
        assertEquals("people", s.table());
    }
}
