package ru.lab.db.ui;

import ru.lab.db.engine.Engine;
import ru.lab.db.engine.QueryResult;
import ru.lab.db.error.DbException;
import ru.lab.db.lang.Parser;
import ru.lab.db.lang.Statement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Interactive console for entering SQL commands. */
public final class Console {

    private final Engine engine;
    private final Printer printer;
    private final BufferedReader in;

    /**
     * Creates a console that executes commands through a database engine.
     *
     * @param engine the database engine
     */
    public Console(Engine engine) {
        this.engine = engine;
        this.printer = new Printer();
        this.in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    /** Reads and executes commands until the user exits. */
    public void run() {
        printer.info("Vacancy DB. Type 'exit' to quit.");
        while (true) {
            System.out.print("db> ");
            System.out.flush();
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                printer.info("Read error: " + e.getMessage());
                return;
            }
            if (line == null) {
                System.out.println();
                return;
            }
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) return;

            try {
                Statement statement = new Parser(line).parse();
                execute(statement);
            } catch (DbException e) {
                printer.error(e.getMessage());
            } catch (IllegalArgumentException e) {
                printer.error("Unexpected error: " + e);
            }
        }
    }

    private void execute(Statement statement) {
        switch (statement.kind()) {
            case INSERT: {
                int affected =
                    engine.insert(statement.table(), statement.columns(), statement.rows());
                printer.modified("inserted", affected);
                break;
            }
            case SELECT: {
                QueryResult result =
                    engine.select(statement.table(), statement.columns(), statement.where());
                printer.select(result.columns(), result.records());
                break;
            }
            case UPDATE: {
                int affected =
                    engine.update(statement.table(), statement.assignments(), statement.where());
                printer.modified("updated", affected);
                break;
            }
            case DELETE: {
                int affected = engine.delete(statement.table(), statement.where());
                printer.modified("deleted", affected);
                break;
            }
            case CREATE_INDEX:
                engine.createIndex(statement.indexName(), statement.table(), statement.indexField());
                printer.info("Index created");
                break;
            case DROP_INDEX:
                engine.dropIndex(statement.indexName());
                printer.info("Index dropped");
                break;
            case CREATE_TABLE:
                engine.createTable(statement.schema());
                printer.info("Table created");
                break;
            case DROP_TABLE:
                engine.dropTable(statement.table());
                printer.info("Table dropped");
                break;
            default:
                throw new DbException("Unsupported statement: " + statement.kind());
        }
    }
}
