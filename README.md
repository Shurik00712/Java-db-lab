# Лабораторная работа 2. Файловая СУБД

Консольная СУБД на Java 8+ с SQL-подобными командами, связанными таблицами и hash-индексами. Данные и описания индексов сохраняются в `data/database.dat`.

## Запуск

Нужны JDK 8+ и Maven. Команды выполняются из папки проекта:

```text
mvn clean test
mvn package
java -cp target\classes ru.lab.db.Main
```

Для генерации Javadoc: `mvn javadoc:javadoc`. Введите `exit`, чтобы закрыть программу. При первом запуске создаются пустые таблицы.

## Таблицы и типы

Встроенная база хранит работодателей и их вакансии:

| Таблица | Поля |
| --- | --- |
| `employers` | `id` INTEGER - первичный ключ; `name` STRING; `verified` BOOLEAN (по умолчанию FALSE) |
| `vacancies` | `id` INTEGER - первичный ключ; `title` STRING; `salary` DOUBLE (по умолчанию 0); `published` DATE; `remote` BOOLEAN (по умолчанию FALSE); `employer_id` INTEGER - ссылка на `employers.id` |

Поддерживаются INTEGER, DOUBLE, STRING, DATE (`'YYYY-MM-DD'`), BOOLEAN (`TRUE`/`FALSE`) и `NULL` для nullable-полей. Зарплата не может быть отрицательной. Первичные ключи уникальны; нельзя удалить или изменить ключ работодателя, на который ссылаются вакансии. Имена команд, таблиц и полей нечувствительны к регистру.

## Команды

Поддерживаются `INSERT INTO`, `SELECT`, `UPDATE`, `DELETE`, `CREATE TABLE`, `DROP TABLE`, `CREATE INDEX` и `DROP INDEX`. В `UPDATE` за одну команду задаётся одно поле. Условие `WHERE` - одно сравнение с `=`, `!=`, `<`, `>`, `<=` или `>=`; у `UPDATE` и `DELETE` оно необязательно. Без него операция применяется ко всем строкам.

```sql
INSERT INTO employers VALUES (1, 'Acme', TRUE);
INSERT INTO vacancies (id, title, salary, published, employer_id) VALUES (101, 'Java developer', 120000, '2026-10-01', 1);
SELECT title, salary FROM vacancies WHERE salary > 20000;
UPDATE vacancies SET salary = 130000 WHERE id = 101;
DELETE FROM vacancies WHERE id = 101;
CREATE INDEX idx_title ON vacancies (title);
CREATE TABLE notes (id INTEGER PRIMARY KEY, message STRING DEFAULT 'empty');
DROP TABLE notes;
```

Частичный `INSERT` использует значения по умолчанию; иначе пропущенное поле должно допускать `NULL`. Для BOOLEAN доступны только `=` и `!=`; сравнение строк чувствительно к регистру, а сравнение с `NULL` не находит строк. Индекс ускоряет поиск по равенству. Сложные условия, `IS NULL` и объявление внешних ключей в `CREATE TABLE` не поддерживаются.

## Хранение и архитектура

Таблицы и описания индексов сохраняются после каждой изменяющей команды; при запуске индексы восстанавливаются из данных. Запись проходит через временный файл. Если сохранение не удалось, изменение остаётся в памяти и выводится ошибка. Форматы данных старых версий автоматически не мигрируются.

Классы разделены по слоям: `Store` отвечает за файловое хранение, `Database` и `Table` - за данные и ограничения, `Parser` - за разбор команд, `Engine` - за выполнение и сохранение, `IndexManager` и `HashIndex` - за индексы; элементы списков удаляются через `Iterator`.

```mermaid
classDiagram
    Main --> Store
    Main --> Engine
    Main --> Console
    Console --> Parser
    Console --> Engine
    Engine --> Database
    Database --> Table
    Table --> Record
    Engine --> IndexManager
    IndexManager --> HashIndex
    Engine --> Store
```

```mermaid
sequenceDiagram
    actor User
    participant Main
    participant Store
    participant Console
    participant Parser
    participant Engine
    participant Database
    Main->>Store: загрузить базу
    Store-->>Main: Database
    Main->>Engine: создать (Database, Store)
    Main->>Console: запустить
    User->>Console: SQL-команда
    Console->>Parser: разобрать
    Parser-->>Console: Statement
    Console->>Engine: выполнить
    Engine->>Database: проверить и выполнить
    opt Команда изменяет данные
        Engine->>Store: сохранить базу
        Store-->>Engine: результат сохранения
    end
    Engine-->>Console: результат
    Console-->>User: вывести результат
```

## Тесты и производительность

Тесты запускаются командой `mvn clean test`. Бенчмарк повторяется так:

```text
mvn test-compile
java -cp "target\test-classes;target\classes" ru.lab.db.PerformanceBenchmark
```

Поиск по уникальному имени в таблице из 30 000 работодателей; 150 запросов для прогрева, медиана пяти раундов по 600 запросов. Замер не включает создание данных и индекса.

| Без индекса | С hash-индексом | Ускорение |
| ---: | ---: | ---: |
| 1227 мс | 2 мс | около 600 раз |

Измерено на Windows с JDK 27.0.1.0.3; абсолютные значения зависят от компьютера и JVM, важно соотношение.

## Выводы

Реализованы две связанные таблицы, все требуемые типы данных, команды INSERT, SELECT, UPDATE, DELETE, CREATE/DROP TABLE и CREATE/DROP INDEX, файловое хранение с загрузкой при старте и hash-индексы с сохранением на диск. Индекс сокращает поиск по равенству с полного просмотра таблицы до выборки по хеш-таблице; на 30 000 записей ускорение составило около 600 раз.

## Источники

1. Документация Java SE 8: пакеты `java.io`, `java.time`, `java.util`.
2. Документация Apache Maven (maven-compiler-plugin, maven-surefire-plugin, maven-javadoc-plugin).
3. Документация JUnit 4.

## Участники и вклад

- **Александр Щенников** - модель данных, связанные таблицы, ограничения и файловое хранение.
- **Артём Павлов** - SQL-парсер, выполнение запросов, консольный интерфейс и hash-индексы.

Тестирование и документацию подготовили совместно.
