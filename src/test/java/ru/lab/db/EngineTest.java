package ru.lab.db;

import org.junit.Test;
import ru.lab.db.engine.Engine;
import ru.lab.db.engine.QueryResult;
import ru.lab.db.error.ConstraintViolationException;
import ru.lab.db.model.Schemas;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Database;
import ru.lab.db.model.Record;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EngineTest {

    @Test
    public void insertAndSelect() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);

        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(120000.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(true), Cell.ofInt(1))));

        QueryResult result = engine.select("vacancies",
            Arrays.asList("title", "salary"), null);
        assertEquals(1, result.records().size());
        assertEquals("Java", result.records().get(0).get("title").asString());
    }

    @Test
    public void insertMultiRowRejectsDuplicatePrimaryKey() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);

        try {
            engine.insert("employers", Collections.<String>emptyList(),
                Arrays.asList(
                    Arrays.asList(Cell.ofInt(1), Cell.ofString("A"), Cell.ofBoolean(true)),
                    Arrays.asList(Cell.ofInt(1), Cell.ofString("B"), Cell.ofBoolean(true))));
            fail("Expected duplicate primary key");
        } catch (ConstraintViolationException expected) {
        }
        assertEquals(0, db.table("employers").size());
    }

    @Test
    public void insertRejectsUnknownForeignKey() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);

        try {
            engine.insert("vacancies", Collections.<String>emptyList(),
                Collections.singletonList(Arrays.asList(
                    Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(0.0),
                    Cell.ofString("2026-10-01"), Cell.ofBoolean(true), Cell.ofInt(99))));
            fail("Expected foreign key violation");
        } catch (ConstraintViolationException expected) {
        }
        assertEquals(0, db.table("vacancies").size());
    }

    @Test
    public void deleteEmployerWithVacanciesRejected() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(0.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(true), Cell.ofInt(1))));

        try {
            engine.delete("employers",
                new Condition("id", Condition.EQ, Cell.ofInt(1)));
            fail("Expected referenced employer deletion to fail");
        } catch (ConstraintViolationException expected) {
        }
        assertEquals(1, db.table("employers").size());
    }

    @Test(expected = ConstraintViolationException.class)
    public void updateRejectsUnknownForeignKey() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(0.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(true), Cell.ofInt(1))));

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("employer_id", Cell.ofInt(99));
        engine.update("vacancies", changes,
            new Condition("id", Condition.EQ, Cell.ofInt(101)));
    }

    @Test
    public void insertCoercesStringToDate() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
        int result = engine.insert("vacancies",
            Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(0.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(true), Cell.ofInt(1))));
        assertEquals(1, result);
    }

    @Test
    public void tableDropRemovesIndexesAndRespectsForeignKeys() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.createIndex("idx_vacancy_title", "vacancies", "title");

        try {
            engine.dropTable("employers");
            fail("Expected referenced table drop to fail");
        } catch (ConstraintViolationException expected) {
        }

        engine.dropTable("vacancies");
        assertFalse(db.has("vacancies"));
        assertNull(engine.findIndex("idx_vacancy_title"));
        engine.dropTable("employers");
        assertFalse(db.has("employers"));
    }

    @Test
    public void conditionOperatorsPreserveComparisonBehavior() {
        Map<String, Cell> values = new LinkedHashMap<>();
        values.put("id", Cell.ofInt(5));
        values.put("name", Cell.ofString("Acme"));
        values.put("verified", Cell.ofBoolean(true));
        Record record = new Record(values);

        assertTrue(new Condition("id", Condition.EQ, Cell.ofInt(5)).test(record));
        assertTrue(new Condition("id", Condition.NEQ, Cell.ofInt(4)).test(record));
        assertTrue(new Condition("id", Condition.LT, Cell.ofInt(6)).test(record));
        assertTrue(new Condition("id", Condition.LTE, Cell.ofInt(5)).test(record));
        assertTrue(new Condition("id", Condition.GT, Cell.ofInt(4)).test(record));
        assertTrue(new Condition("id", Condition.GTE, Cell.ofInt(5)).test(record));
        assertFalse(new Condition("id", Condition.EQ, Cell.ofInt(4)).test(record));
        assertEquals("id >= 5",
            new Condition("id", Condition.GTE, Cell.ofInt(5)).toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void conditionRejectsUnknownOperator() {
        new Condition("id", "==", Cell.ofInt(5));
    }

    @Test
    public void updateAndDeleteWithoutConditionAffectAllRows() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Arrays.asList(
                Arrays.asList(Cell.ofInt(1), Cell.ofString("A"), Cell.ofBoolean(false)),
                Arrays.asList(Cell.ofInt(2), Cell.ofString("B"), Cell.ofBoolean(false))));

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("verified", Cell.ofBoolean(true));
        assertEquals(2, engine.update("employers", changes, null));
        assertEquals(2, engine.select("employers", null,
            new Condition("verified", Condition.EQ, Cell.ofBoolean(true))).records().size());
        assertEquals(2, engine.delete("employers", null));
        assertEquals(0, db.table("employers").size());
    }

    @Test
    public void nullInsertAndComparisonFollowSqlNullBehavior() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.createTable(new ru.lab.db.lang.Parser(
            "CREATE TABLE notes (id INTEGER PRIMARY KEY, message STRING NULL);")
            .parse().schema());

        engine.insert("notes", Arrays.asList("id", "message"),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.nullOf(ru.lab.db.model.FieldType.STRING))));

        assertEquals(0, engine.select("notes", null,
            new Condition("message", Condition.EQ, Cell.nullOf(
                ru.lab.db.model.FieldType.STRING))).records().size());
    }

    @Test(expected = ConstraintViolationException.class)
    public void booleanOrderingIsRejected() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.select("employers", null,
            new Condition("verified", Condition.GT, Cell.ofBoolean(false)));
    }

    @Test
    public void primaryKeyCanChangeWhenUniqueAndUnreferenced() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("A"), Cell.ofBoolean(false))));

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("id", Cell.ofInt(2));
        assertEquals(1, engine.update("employers", changes,
            new Condition("id", Condition.EQ, Cell.ofInt(1))));
        assertNull(db.table("employers").findByPk(Cell.ofInt(1)));
        assertNotNull(db.table("employers").findByPk(Cell.ofInt(2)));
    }

    @Test
    public void duplicatePrimaryKeyUpdateDoesNotChangeRows() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Arrays.asList(
                Arrays.asList(Cell.ofInt(1), Cell.ofString("A"), Cell.ofBoolean(false)),
                Arrays.asList(Cell.ofInt(2), Cell.ofString("B"), Cell.ofBoolean(false))));

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("id", Cell.ofInt(3));
        try {
            engine.update("employers", changes, null);
            fail("Expected duplicate primary key");
        } catch (ConstraintViolationException expected) {
        }
        assertNotNull(db.table("employers").findByPk(Cell.ofInt(1)));
        assertNotNull(db.table("employers").findByPk(Cell.ofInt(2)));
        assertNull(db.table("employers").findByPk(Cell.ofInt(3)));
    }

    @Test
    public void referencedPrimaryKeyCannotChange() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("A"), Cell.ofBoolean(false))));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(10), Cell.ofString("Java"), Cell.ofDouble(100.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(false), Cell.ofInt(1))));

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("id", Cell.ofInt(2));
        try {
            engine.update("employers", changes,
                new Condition("id", Condition.EQ, Cell.ofInt(1)));
            fail("Expected referenced primary key update to fail");
        } catch (ConstraintViolationException expected) {
        }
        assertNotNull(db.table("employers").findByPk(Cell.ofInt(1)));
        assertNull(db.table("employers").findByPk(Cell.ofInt(2)));
    }

    @Test(expected = ConstraintViolationException.class)
    public void negativeSalaryIsRejected() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("A"), Cell.ofBoolean(false))));

        engine.insert("vacancies", Collections.<String>emptyList(),
                Collections.singletonList(Arrays.asList(
                    Cell.ofInt(10), Cell.ofString("Java"), Cell.ofDouble(-1.0),
                    Cell.ofString("2026-10-01"), Cell.ofBoolean(false), Cell.ofInt(1))));
    }
}
