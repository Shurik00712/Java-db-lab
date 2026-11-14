package ru.lab.db;

import ru.lab.db.engine.Engine;
import ru.lab.db.error.StorageException;
import ru.lab.db.model.Database;
import ru.lab.db.store.Store;
import ru.lab.db.ui.Console;

import java.nio.file.Paths;

/** Entry point of the console application. */
public final class Main {

    private Main() {
    }

    /**
     * Loads the database and starts the console interface.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        Store store = new Store(Paths.get("data"));
        Database database;
        try {
            database = store.load();
        } catch (StorageException e) {
            System.err.println("Failed to load the database: " + e.getMessage());
            System.exit(2);
            return;
        }
        Engine engine = new Engine(database, store);
        new Console(engine).run();
    }
}
