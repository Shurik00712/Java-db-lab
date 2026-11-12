package ru.lab.db;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.lab.db.engine.Engine;
import ru.lab.db.engine.HashIndex;
import ru.lab.db.engine.QueryResult;
import ru.lab.db.error.ConstraintViolationException;
import ru.lab.db.error.DbException;
import ru.lab.db.error.StorageException;
import ru.lab.db.lang.Parser;
import ru.lab.db.lang.Statement;
import ru.lab.db.model.Schemas;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Database;
import ru.lab.db.store.Store;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IndexTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void parsesCreateAndDropIndex() {
        Statement create = new Parser(
            "CREATE INDEX idx_title ON vacancies (title);").parse();
        assertEquals(Statement.Kind.CREATE_INDEX, create.kind());
        assertEquals("idx_title", create.indexName());
        assertEquals("vacancies", create.table());
        assertEquals("title", create.indexField());

        Statement drop = new Parser("DROP INDEX idx_title;").parse();
        assertEquals(Statement.Kind.DROP_INDEX, drop.kind());
        assertEquals("idx_title", drop.indexName());
    }

    @Test
    public void indexTracksInsertUpdateDeleteAndOnlySupportsEquality() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        insertEmployer(engine);
        insertVacancies(engine);

        HashIndex index = engine.createIndex("idx_title", "vacancies", "title");
        Set<Cell> candidates = index.candidates(Cell.ofString("Java"), Condition.EQ);
        assertEquals(2, candidates.size());
        assertNull(index.candidates(Cell.ofString("Java"), Condition.GT));

        QueryResult result = engine.select("vacancies",
            Collections.singletonList("title"),
            new Condition("title", Condition.EQ, Cell.ofString("Java")));
        assertEquals(2, result.records().size());

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("title", Cell.ofString("Kotlin"));
        engine.update("vacancies", changes,
            new Condition("id", Condition.EQ, Cell.ofInt(101)));
        assertEquals(1, index.candidates(Cell.ofString("Java"), Condition.EQ).size());
        assertEquals(1, index.candidates(Cell.ofString("Kotlin"), Condition.EQ).size());

        engine.delete("vacancies",
            new Condition("id", Condition.EQ, Cell.ofInt(102)));
        assertEquals(0, index.candidates(Cell.ofString("Java"), Condition.EQ).size());
    }

    @Test
    public void indexTracksPrimaryKeyUpdates() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        insertEmployer(engine);
        HashIndex index = engine.createIndex("idx_employer_name", "employers", "name");

        Map<String, Cell> changes = new LinkedHashMap<>();
        changes.put("id", Cell.ofInt(2));
        engine.update("employers", changes,
            new Condition("id", Condition.EQ, Cell.ofInt(1)));

        Set<Cell> candidates = index.candidates(Cell.ofString("Acme"), Condition.EQ);
        assertEquals(Collections.singleton(Cell.ofInt(2)), candidates);
    }

    @Test
    public void indexDefinitionsArePersistedAndRebuiltOnLoad() {
        Path dir = tmp.getRoot().toPath();
        Store store = new Store(dir);
        Database db = store.load();
        Engine engine = new Engine(db, store);
        insertEmployer(engine);
        insertVacancies(engine);
        engine.createIndex("idx_title", "vacancies", "title");

        Database loaded = store.load();
        Engine loadedEngine = new Engine(loaded);
        HashIndex index = loadedEngine.findIndex("idx_title");
        assertNotNull(index);
        assertEquals(2, index.candidates(Cell.ofString("Java"), Condition.EQ).size());

        new Engine(loaded, store).dropIndex("idx_title");
        assertNull(new Engine(store.load()).findIndex("idx_title"));
    }

    @Test
    public void persistenceFailureIsReportedWithoutRollback() throws Exception {
        Path dir = tmp.getRoot().toPath();
        Files.createDirectory(dir.resolve("database.dat"));
        Store store = new Store(dir);
        Database db = Schemas.create();
        Engine engine = new Engine(db, store);

        try {
            engine.createIndex("idx_title", "vacancies", "title");
            fail("Expected save to fail");
        } catch (StorageException expected) {
        }
        assertNotNull(engine.findIndex("idx_title"));

        try {
            engine.insert(
                "employers", Collections.<String>emptyList(),
                Collections.singletonList(Arrays.asList(
                    Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
            fail("Expected save to fail");
        } catch (StorageException expected) {
        }
        assertEquals(1, db.table("employers").size());
    }

    @Test
    public void rejectsDuplicateIndexAndUnknownDrop() {
        Database db = Schemas.create();
        Engine engine = new Engine(db);
        engine.createIndex("idx_title", "vacancies", "title");

        try {
            engine.createIndex("idx_other", "vacancies", "title");
            fail("Expected duplicate index to fail");
        } catch (ConstraintViolationException expected) {
        }
        try {
            engine.dropIndex("missing");
            fail("Expected unknown index to fail");
        } catch (ConstraintViolationException expected) {
        }
    }

    @Test(expected = DbException.class)
    public void rejectsIndexOnUnknownField() {
        Engine engine = new Engine(Schemas.create());
        engine.createIndex("idx_missing", "vacancies", "missing");
    }

    private void insertEmployer(Engine engine) {
        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
    }

    private void insertVacancies(Engine engine) {
        List<List<Cell>> rows = Arrays.asList(
            Arrays.asList(Cell.ofInt(101), Cell.ofString("Java"),
                Cell.ofDouble(90000.0), Cell.ofString("2026-10-01"),
                Cell.ofBoolean(false), Cell.ofInt(1)),
            Arrays.asList(Cell.ofInt(102), Cell.ofString("Java"),
                Cell.ofDouble(100000.0), Cell.ofString("2026-10-02"),
                Cell.ofBoolean(true), Cell.ofInt(1)),
            Arrays.asList(Cell.ofInt(103), Cell.ofString("Go"),
                Cell.ofDouble(110000.0), Cell.ofString("2026-10-03"),
                Cell.ofBoolean(false), Cell.ofInt(1)));
        engine.insert("vacancies", Collections.<String>emptyList(), rows);
    }
}
