CREATE SCHEMA TestSchema;
CREATE TABLE TestSchema.TestTable
(
    TextColumn text,
    IntColumn  integer
);
INSERT INTO TestSchema.TestTable (TextColumn, IntColumn)
VALUES ('Test_1', 42),
       ('Test_3', 44),
       ('Test_2', 43),
       ('Test_4', 45),
       ('Test_5', 46);
SELECT *
FROM TestSchema.TestTable;
