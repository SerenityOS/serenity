CREATE SCHEMA TestSchema;
CREATE TABLE TestSchema.TestTable1
(
    TextColumn1 text,
    IntColumn   integer
);
CREATE TABLE TestSchema.TestTable2
(
    TextColumn2 text,
    IntColumn   integer
);
INSERT INTO TestSchema.TestTable1 (TextColumn1, IntColumn)
VALUES ('Test_1', 42),
       ('Test_3', 44),
       ('Test_2', 43),
       ('Test_4', 45),
       ('Test_5', 46);
INSERT INTO TestSchema.TestTable2 (TextColumn2, IntColumn)
VALUES ('Test_10', 40),
       ('Test_11', 41),
       ('Test_12', 42),
       ('Test_13', 47),
       ('Test_14', 48);
SELECT TestTable1.IntColumn, TextColumn1, TextColumn2
FROM TestSchema.TestTable1,
     TestSchema.TestTable2
WHERE TestTable1.IntColumn = TestTable2.IntColumn;
