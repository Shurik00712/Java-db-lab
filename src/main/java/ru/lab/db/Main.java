package ru.lab.db;

import ru.lab.db.engine.Engine;
import ru.lab.db.error.StorageException;
import ru.lab.db.model.Database;
import ru.lab.db.store.Store;
import ru.lab.db.ui.Console;

import java.nio.file.Paths;

/** Точка входа консольного приложения. */
public final class Main {

    public static void main(String[] args) {
        Store store = new Store(Paths.get("data"));
        Database database;
        try {
            database = store.load();
        } catch (StorageException e) {
            System.err.println("Не удалось загрузить базу: " + e.getMessage());
            System.exit(2);
            return;
        }
        Engine engine = new Engine(database, store);
        new Console(engine).run();
    }
}
