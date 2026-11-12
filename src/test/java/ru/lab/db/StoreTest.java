package ru.lab.db;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.lab.db.engine.Engine;
import ru.lab.db.error.StorageException;
import ru.lab.db.lang.Parser;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Database;
import ru.lab.db.model.Record;
import ru.lab.db.model.Schema;
import ru.lab.db.store.Store;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StoreTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void loadCreatesEmptyDatabaseOnFirstRun() {
        Path dir = tmp.getRoot().toPath();
        Store store = new Store(dir);

        Database db = store.load();
        assertTrue(db.has("employers"));
        assertTrue(db.has("vacancies"));
        assertEquals(0, db.table("employers").size());
        assertEquals(0, db.table("vacancies").size());
        assertTrue(Files.exists(dir.resolve("database.dat")));
    }

    @Test
    public void saveLoadRoundTrip() {
        Path dir = tmp.getRoot().toPath();
        Store store = new Store(dir);
        Database db = store.load();
        Engine engine = new Engine(db, store);

        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(120000.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(true), Cell.ofInt(1))));

        Database loaded = store.load();
        assertEquals(1, loaded.table("employers").size());
        assertEquals(1, loaded.table("vacancies").size());
        Record vacancy = loaded.table("vacancies").findByPk(Cell.ofInt(101));
        assertNotNull(vacancy);
        assertEquals("Java", vacancy.get("title").asString());
        assertEquals(120000.0, vacancy.get("salary").asDouble(), 0.0001);
        assertEquals(LocalDate.parse("2026-10-01"), vacancy.get("published").asDate());
        assertEquals(1, vacancy.get("employer_id").asInt());
    }

    @Test
    public void createsAndDropsTableAcrossRestarts() {
        Path dir = tmp.getRoot().toPath();
        Store store = new Store(dir);
        Database db = store.load();
        Engine engine = new Engine(db, store);

        Schema schema = new Parser(
            "CREATE TABLE notes (id INTEGER PRIMARY KEY, message STRING "
            + "NOT NULL DEFAULT 'untitled')").parse().schema();
        engine.createTable(schema);
        engine.createIndex("idx_notes_message", "notes", "message");
        engine.insert("notes", Collections.singletonList("id"),
            Collections.singletonList(Collections.singletonList(Cell.ofInt(1))));

        Database reopened = store.load();
        Engine reopenedEngine = new Engine(reopened, store);
        Record note = reopened.table("notes").findByPk(Cell.ofInt(1));
        assertNotNull(note);
        assertEquals("untitled", note.get("message").asString());
        assertNotNull(reopenedEngine.findIndex("idx_notes_message"));

        reopenedEngine.dropTable("notes");
        Database afterDrop = store.load();
        assertFalse(afterDrop.has("notes"));
        assertNull(new Engine(afterDrop).findIndex("idx_notes_message"));
    }

    @Test(expected = StorageException.class)
    public void corruptFileRaisesStorageException() throws Exception {
        Path dir = tmp.getRoot().toPath();
        Files.write(dir.resolve("database.dat"), new byte[] {0, 0, 0, 0});
        Store store = new Store(dir);
        store.load();
    }

    @Test
    public void corruptFileNotOverwrittenByLoad() throws Exception {
        Path dir = tmp.getRoot().toPath();
        byte[] bad = new byte[] {1, 2, 3, 4, 5, 6, 7};
        Files.write(dir.resolve("database.dat"), bad);
        Store store = new Store(dir);
        try {
            store.load();
            fail("Expected corrupt database to fail");
        } catch (StorageException expected) {
        }
        assertEquals(7, Files.size(dir.resolve("database.dat")));
    }

    @Test
    public void queryDeleteAndInsertPersistsCorrectRows() {
        Path dir = tmp.getRoot().toPath();
        Store store = new Store(dir);
        Database db = store.load();
        Engine engine = new Engine(db, store);

        engine.insert("employers", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(1), Cell.ofString("Acme"), Cell.ofBoolean(true))));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(101), Cell.ofString("Java"), Cell.ofDouble(90000.0),
                Cell.ofString("2026-10-01"), Cell.ofBoolean(false), Cell.ofInt(1))));
        engine.delete("vacancies", new Condition("id", Condition.EQ, Cell.ofInt(101)));
        engine.insert("vacancies", Collections.<String>emptyList(),
            Collections.singletonList(Arrays.asList(
                Cell.ofInt(102), Cell.ofString("Kotlin"), Cell.ofDouble(95000.0),
                Cell.ofString("2026-10-02"), Cell.ofBoolean(false), Cell.ofInt(1))));

        Database reopened = store.load();
        assertEquals(1, reopened.table("vacancies").size());
        assertEquals(102, reopened.table("vacancies").records().get(0).get("id").asInt());
    }
}
