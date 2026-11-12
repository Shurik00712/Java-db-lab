package ru.lab.db;

import ru.lab.db.engine.Engine;
import ru.lab.db.model.Cell;
import ru.lab.db.model.Condition;
import ru.lab.db.model.Database;
import ru.lab.db.model.Schemas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PerformanceBenchmark {

    private static final int ROWS = 30000;
    private static final int WARMUP = 150;
    private static final int ITERATIONS = 600;
    private static final int ROUNDS = 5;
    private static int resultSink;

    private PerformanceBenchmark() {
    }

    public static void main(String[] args) {
        Database database = Schemas.create();
        Engine scanEngine = new Engine(database);
        List<List<Cell>> rows = new ArrayList<>(ROWS);
        for (int i = 0; i < ROWS; i++) {
            rows.add(Arrays.asList(
                Cell.ofInt(i),
                Cell.ofString("Employer-" + i),
                Cell.ofBoolean(i % 2 == 0)));
        }
        scanEngine.insert("employers", Collections.<String>emptyList(), rows);

        Condition condition = new Condition(
            "name", Condition.EQ, Cell.ofString("Employer-" + (ROWS - 1)));
        for (int i = 0; i < WARMUP; i++) {
            runQuery(scanEngine, condition, 1);
        }

        Engine indexBuilder = new Engine(database);
        indexBuilder.createIndex("idx_employer_name", "employers", "name");
        Engine indexEngine = new Engine(database);
        for (int i = 0; i < WARMUP; i++) {
            runQuery(indexEngine, condition, 1);
        }

        long[] scanTimes = new long[ROUNDS];
        long[] indexTimes = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            if (i % 2 == 0) {
                scanTimes[i] = measure(scanEngine, condition);
                indexTimes[i] = measure(indexEngine, condition);
            } else {
                indexTimes[i] = measure(indexEngine, condition);
                scanTimes[i] = measure(scanEngine, condition);
            }
        }

        long scanMedian = median(scanTimes);
        long indexMedian = median(indexTimes);
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Rows: " + ROWS + ", queries per round: " + ITERATIONS);
        System.out.println("Median without index: " + scanMedian + " ms");
        System.out.println("Median with hash index: " + indexMedian + " ms");
        System.out.println("Speedup: " + (double) scanMedian / indexMedian + "x");
        System.out.println("Result checksum: " + resultSink);
    }

    private static long measure(Engine engine, Condition condition) {
        long started = System.currentTimeMillis();
        runQuery(engine, condition, ITERATIONS);
        return System.currentTimeMillis() - started;
    }

    private static void runQuery(Engine engine, Condition condition, int count) {
        int matches = 0;
        for (int i = 0; i < count; i++) {
            matches += engine.select("employers",
                Collections.singletonList("name"), condition).records().size();
        }
        if (matches != count) {
            throw new IllegalStateException("Benchmark query returned " + matches + " matches");
        }
        resultSink += matches;
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

}
