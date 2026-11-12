package ru.lab.db.store;

import ru.lab.db.error.StorageException;
import ru.lab.db.model.Schemas;
import ru.lab.db.model.Database;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Сохраняет базу данных целиком в один файл через Java-сериализацию. */
public final class Store {

    private static final String FILE = "database.dat";

    private final Path dir;

    public Store(Path dir) {
        this.dir = dir;
    }

    public Database load() {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(FILE);
            if (!Files.exists(file)) {
                Database database = Schemas.create();
                save(database);
                return database;
            }
            try (ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(Files.newInputStream(file)))) {
                Object object = in.readObject();
                if (!(object instanceof Database)) {
                    throw new IOException("File does not contain a database");
                }
                Database database = (Database) object;
                database.checkReferences();
                return database;
            }
        } catch (Exception e) {
            throw new StorageException(
                "Failed to load database from " + dir + ": " + e.getMessage(), e);
        }
    }

    public void save(Database database) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(FILE);
            Path temporary = dir.resolve(FILE + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeObject(database);
            }
            moveReplacing(temporary, file);
        } catch (Exception e) {
            throw new StorageException("Failed to save database: " + e.getMessage(), e);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
