package ru.lab.db.model;

import java.util.Arrays;
import java.util.Collections;

/** Built-in schemas for employers and vacancies. */
public final class Schemas {

    private Schemas() {
    }

    /**
     * Returns the employers table schema.
     *
     * @return the table schema
     */
    public static Schema employers() {
        return new Schema(
            "employers",
            Arrays.asList(
                new Field("id", FieldType.INTEGER, false, true, null),
                new Field("name", FieldType.STRING, false, false, null),
                new Field("verified", FieldType.BOOLEAN, false, false, Cell.ofBoolean(false))
            ),
            "id",
            Collections.<Reference>emptyList()
        );
    }

    /**
     * Returns the vacancies table schema.
     *
     * @return the schema with a reference to an employer
     */
    public static Schema vacancies() {
        return new Schema(
            "vacancies",
            Arrays.asList(
                new Field("id", FieldType.INTEGER, false, true, null),
                new Field("title", FieldType.STRING, false, false, null),
                new Field("salary", FieldType.DOUBLE, false, false,
                    Cell.ofDouble(0.0), 0.0),
                new Field("published", FieldType.DATE, false, false, null),
                new Field("remote", FieldType.BOOLEAN, false, false, Cell.ofBoolean(false)),
                new Field("employer_id", FieldType.INTEGER, false, false, null)
            ),
            "id",
            Collections.singletonList(new Reference("employer_id", "employers", "id"))
        );
    }

    /**
     * Creates a database with the built-in tables.
     *
     * @return a database with the employers and vacancies tables
     */
    public static Database create() {
        Database db = new Database();
        db.add(new Table(employers()));
        db.add(new Table(vacancies()));
        db.checkReferences();
        return db;
    }
}
