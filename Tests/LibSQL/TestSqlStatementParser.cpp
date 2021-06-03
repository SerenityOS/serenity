/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Optional.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibSQL/Lexer.h>
#include <LibSQL/Parser.h>

namespace {

using ParseResult = AK::Result<NonnullRefPtr<SQL::Statement>, String>;

ParseResult parse(StringView sql)
{
    auto parser = SQL::Parser(SQL::Lexer(sql));
    auto statement = parser.next_statement();

    if (parser.has_errors()) {
        return parser.errors()[0].to_string();
    }

    return statement;
}

}

TEST_CASE(create_table)
{
    EXPECT(parse("").is_error());
    EXPECT(parse("CREATE").is_error());
    EXPECT(parse("CREATE TABLE").is_error());
    EXPECT(parse("CREATE TABLE test").is_error());
    EXPECT(parse("CREATE TABLE test ()").is_error());
    EXPECT(parse("CREATE TABLE test ();").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 ").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 )").is_error());
    EXPECT(parse("CREATE TABLE IF test ( column1 );").is_error());
    EXPECT(parse("CREATE TABLE IF NOT test ( column1 );").is_error());
    EXPECT(parse("CREATE TABLE AS;").is_error());
    EXPECT(parse("CREATE TABLE AS SELECT;").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar()").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(abc)").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123 )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123,  )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123, ) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(.) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(.abc) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(0x) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(0xzzz) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 int ) AS SELECT * FROM table;").is_error());
    EXPECT(parse("CREATE TABLE test AS SELECT * FROM table ( column1 int ) ;").is_error());

    struct Column {
        StringView name;
        StringView type;
        Vector<double> signed_numbers {};
    };

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, Vector<Column> expected_columns, bool expected_is_temporary = false, bool expected_is_error_if_table_exists = true) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::CreateTable>(*statement));

        const auto& table = static_cast<const SQL::CreateTable&>(*statement);
        EXPECT_EQ(table.schema_name(), expected_schema);
        EXPECT_EQ(table.table_name(), expected_table);
        EXPECT_EQ(table.is_temporary(), expected_is_temporary);
        EXPECT_EQ(table.is_error_if_table_exists(), expected_is_error_if_table_exists);

        bool expect_select_statement = expected_columns.is_empty();
        EXPECT_EQ(table.has_selection(), expect_select_statement);
        EXPECT_EQ(table.has_columns(), !expect_select_statement);

        const auto& select_statement = table.select_statement();
        EXPECT_EQ(select_statement.is_null(), !expect_select_statement);

        const auto& columns = table.columns();
        EXPECT_EQ(columns.size(), expected_columns.size());

        for (size_t i = 0; i < columns.size(); ++i) {
            const auto& column = columns[i];
            const auto& expected_column = expected_columns[i];
            EXPECT_EQ(column.name(), expected_column.name);

            const auto& type_name = column.type_name();
            EXPECT_EQ(type_name->name(), expected_column.type);

            const auto& signed_numbers = type_name->signed_numbers();
            EXPECT_EQ(signed_numbers.size(), expected_column.signed_numbers.size());

            for (size_t j = 0; j < signed_numbers.size(); ++j) {
                double signed_number = signed_numbers[j].value();
                double expected_signed_number = expected_column.signed_numbers[j];
                EXPECT_EQ(signed_number, expected_signed_number);
            }
        }
    };

    validate("CREATE TABLE test ( column1 );", {}, "test", { { "column1", "BLOB" } });
    validate("CREATE TABLE schema.test ( column1 );", "schema", "test", { { "column1", "BLOB" } });
    validate("CREATE TEMP TABLE test ( column1 );", {}, "test", { { "column1", "BLOB" } }, true, true);
    validate("CREATE TEMPORARY TABLE test ( column1 );", {}, "test", { { "column1", "BLOB" } }, true, true);
    validate("CREATE TABLE IF NOT EXISTS test ( column1 );", {}, "test", { { "column1", "BLOB" } }, false, false);

    validate("CREATE TABLE test AS SELECT * FROM table;", {}, "test", {});

    validate("CREATE TABLE test ( column1 int );", {}, "test", { { "column1", "int" } });
    validate("CREATE TABLE test ( column1 varchar );", {}, "test", { { "column1", "varchar" } });
    validate("CREATE TABLE test ( column1 varchar(255) );", {}, "test", { { "column1", "varchar", { 255 } } });
    validate("CREATE TABLE test ( column1 varchar(255, 123) );", {}, "test", { { "column1", "varchar", { 255, 123 } } });
    validate("CREATE TABLE test ( column1 varchar(255, -123) );", {}, "test", { { "column1", "varchar", { 255, -123 } } });
    validate("CREATE TABLE test ( column1 varchar(0xff) );", {}, "test", { { "column1", "varchar", { 255 } } });
    validate("CREATE TABLE test ( column1 varchar(3.14) );", {}, "test", { { "column1", "varchar", { 3.14 } } });
    validate("CREATE TABLE test ( column1 varchar(1e3) );", {}, "test", { { "column1", "varchar", { 1000 } } });
}

TEST_CASE(alter_table)
{
    // This test case only contains common error cases of the AlterTable subclasses.
    EXPECT(parse("ALTER").is_error());
    EXPECT(parse("ALTER TABLE").is_error());
    EXPECT(parse("ALTER TABLE table").is_error());
    EXPECT(parse("ALTER TABLE table;").is_error());
}

TEST_CASE(alter_table_rename_table)
{
    EXPECT(parse("ALTER TABLE table RENAME").is_error());
    EXPECT(parse("ALTER TABLE table RENAME TO").is_error());
    EXPECT(parse("ALTER TABLE table RENAME TO new_table").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_new_table) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::RenameTable>(*statement));

        const auto& alter = static_cast<const SQL::RenameTable&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);
        EXPECT_EQ(alter.new_table_name(), expected_new_table);
    };

    validate("ALTER TABLE table RENAME TO new_table;", {}, "table", "new_table");
    validate("ALTER TABLE schema.table RENAME TO new_table;", "schema", "table", "new_table");
}

TEST_CASE(alter_table_rename_column)
{
    EXPECT(parse("ALTER TABLE table RENAME").is_error());
    EXPECT(parse("ALTER TABLE table RENAME COLUMN").is_error());
    EXPECT(parse("ALTER TABLE table RENAME COLUMN column").is_error());
    EXPECT(parse("ALTER TABLE table RENAME COLUMN column TO").is_error());
    EXPECT(parse("ALTER TABLE table RENAME COLUMN column TO new_column").is_error());
    EXPECT(parse("ALTER TABLE table RENAME column").is_error());
    EXPECT(parse("ALTER TABLE table RENAME column TO").is_error());
    EXPECT(parse("ALTER TABLE table RENAME column TO new_column").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column, StringView expected_new_column) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::RenameColumn>(*statement));

        const auto& alter = static_cast<const SQL::RenameColumn&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);
        EXPECT_EQ(alter.column_name(), expected_column);
        EXPECT_EQ(alter.new_column_name(), expected_new_column);
    };

    validate("ALTER TABLE table RENAME column TO new_column;", {}, "table", "column", "new_column");
    validate("ALTER TABLE table RENAME COLUMN column TO new_column;", {}, "table", "column", "new_column");
    validate("ALTER TABLE schema.table RENAME column TO new_column;", "schema", "table", "column", "new_column");
    validate("ALTER TABLE schema.table RENAME COLUMN column TO new_column;", "schema", "table", "column", "new_column");
}

TEST_CASE(alter_table_add_column)
{
    EXPECT(parse("ALTER TABLE table ADD").is_error());
    EXPECT(parse("ALTER TABLE table ADD COLUMN").is_error());
    EXPECT(parse("ALTER TABLE table ADD COLUMN column").is_error());

    struct Column {
        StringView name;
        StringView type;
        Vector<double> signed_numbers {};
    };

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, Column expected_column) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::AddColumn>(*statement));

        const auto& alter = static_cast<const SQL::AddColumn&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);

        const auto& column = alter.column();
        EXPECT_EQ(column->name(), expected_column.name);

        const auto& type_name = column->type_name();
        EXPECT_EQ(type_name->name(), expected_column.type);

        const auto& signed_numbers = type_name->signed_numbers();
        EXPECT_EQ(signed_numbers.size(), expected_column.signed_numbers.size());

        for (size_t j = 0; j < signed_numbers.size(); ++j) {
            double signed_number = signed_numbers[j].value();
            double expected_signed_number = expected_column.signed_numbers[j];
            EXPECT_EQ(signed_number, expected_signed_number);
        }
    };

    validate("ALTER TABLE test ADD column1;", {}, "test", { "column1", "BLOB" });
    validate("ALTER TABLE test ADD column1 int;", {}, "test", { "column1", "int" });
    validate("ALTER TABLE test ADD column1 varchar;", {}, "test", { "column1", "varchar" });
    validate("ALTER TABLE test ADD column1 varchar(255);", {}, "test", { "column1", "varchar", { 255 } });
    validate("ALTER TABLE test ADD column1 varchar(255, 123);", {}, "test", { "column1", "varchar", { 255, 123 } });

    validate("ALTER TABLE schema.test ADD COLUMN column1;", "schema", "test", { "column1", "BLOB" });
    validate("ALTER TABLE schema.test ADD COLUMN column1 int;", "schema", "test", { "column1", "int" });
    validate("ALTER TABLE schema.test ADD COLUMN column1 varchar;", "schema", "test", { "column1", "varchar" });
    validate("ALTER TABLE schema.test ADD COLUMN column1 varchar(255);", "schema", "test", { "column1", "varchar", { 255 } });
    validate("ALTER TABLE schema.test ADD COLUMN column1 varchar(255, 123);", "schema", "test", { "column1", "varchar", { 255, 123 } });
}

TEST_CASE(alter_table_drop_column)
{
    EXPECT(parse("ALTER TABLE table DROP").is_error());
    EXPECT(parse("ALTER TABLE table DROP COLUMN").is_error());
    EXPECT(parse("ALTER TABLE table DROP column").is_error());
    EXPECT(parse("ALTER TABLE table DROP COLUMN column").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::DropColumn>(*statement));

        const auto& alter = static_cast<const SQL::DropColumn&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);
        EXPECT_EQ(alter.column_name(), expected_column);
    };

    validate("ALTER TABLE table DROP column;", {}, "table", "column");
    validate("ALTER TABLE table DROP COLUMN column;", {}, "table", "column");
    validate("ALTER TABLE schema.table DROP column;", "schema", "table", "column");
    validate("ALTER TABLE schema.table DROP COLUMN column;", "schema", "table", "column");
}

TEST_CASE(drop_table)
{
    EXPECT(parse("DROP").is_error());
    EXPECT(parse("DROP TABLE").is_error());
    EXPECT(parse("DROP TABLE test").is_error());
    EXPECT(parse("DROP TABLE IF test;").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, bool expected_is_error_if_table_does_not_exist = true) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::DropTable>(*statement));

        const auto& table = static_cast<const SQL::DropTable&>(*statement);
        EXPECT_EQ(table.schema_name(), expected_schema);
        EXPECT_EQ(table.table_name(), expected_table);
        EXPECT_EQ(table.is_error_if_table_does_not_exist(), expected_is_error_if_table_does_not_exist);
    };

    validate("DROP TABLE test;", {}, "test");
    validate("DROP TABLE schema.test;", "schema", "test");
    validate("DROP TABLE IF EXISTS test;", {}, "test", false);
}

TEST_CASE(insert)
{
    EXPECT(parse("INSERT").is_error());
    EXPECT(parse("INSERT INTO").is_error());
    EXPECT(parse("INSERT INTO table").is_error());
    EXPECT(parse("INSERT INTO table (column)").is_error());
    EXPECT(parse("INSERT INTO table (column, ) DEFAULT VALUES;").is_error());
    EXPECT(parse("INSERT INTO table VALUES").is_error());
    EXPECT(parse("INSERT INTO table VALUES ();").is_error());
    EXPECT(parse("INSERT INTO table VALUES (1)").is_error());
    EXPECT(parse("INSERT INTO table SELECT").is_error());
    EXPECT(parse("INSERT INTO table SELECT * from table").is_error());
    EXPECT(parse("INSERT OR INTO table DEFAULT VALUES;").is_error());
    EXPECT(parse("INSERT OR foo INTO table DEFAULT VALUES;").is_error());

    auto validate = [](StringView sql, SQL::ConflictResolution expected_conflict_resolution, StringView expected_schema, StringView expected_table, StringView expected_alias, Vector<StringView> expected_column_names, Vector<size_t> expected_chain_sizes, bool expect_select_statement) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::Insert>(*statement));

        const auto& insert = static_cast<const SQL::Insert&>(*statement);
        EXPECT_EQ(insert.conflict_resolution(), expected_conflict_resolution);
        EXPECT_EQ(insert.schema_name(), expected_schema);
        EXPECT_EQ(insert.table_name(), expected_table);
        EXPECT_EQ(insert.alias(), expected_alias);

        const auto& column_names = insert.column_names();
        EXPECT_EQ(column_names.size(), expected_column_names.size());
        for (size_t i = 0; i < column_names.size(); ++i)
            EXPECT_EQ(column_names[i], expected_column_names[i]);

        EXPECT_EQ(insert.has_expressions(), !expected_chain_sizes.is_empty());
        if (insert.has_expressions()) {
            const auto& chained_expressions = insert.chained_expressions();
            EXPECT_EQ(chained_expressions.size(), expected_chain_sizes.size());

            for (size_t i = 0; i < chained_expressions.size(); ++i) {
                const auto& chained_expression = chained_expressions[i];
                const auto& expressions = chained_expression.expressions();
                EXPECT_EQ(expressions.size(), expected_chain_sizes[i]);

                for (const auto& expression : expressions)
                    EXPECT(!is<SQL::ErrorExpression>(expression));
            }
        }

        EXPECT_EQ(insert.has_selection(), expect_select_statement);
        EXPECT_EQ(insert.default_values(), expected_chain_sizes.is_empty() && !expect_select_statement);
    };

    validate("INSERT OR ABORT INTO table DEFAULT VALUES;", SQL::ConflictResolution::Abort, {}, "table", {}, {}, {}, false);
    validate("INSERT OR FAIL INTO table DEFAULT VALUES;", SQL::ConflictResolution::Fail, {}, "table", {}, {}, {}, false);
    validate("INSERT OR IGNORE INTO table DEFAULT VALUES;", SQL::ConflictResolution::Ignore, {}, "table", {}, {}, {}, false);
    validate("INSERT OR REPLACE INTO table DEFAULT VALUES;", SQL::ConflictResolution::Replace, {}, "table", {}, {}, {}, false);
    validate("INSERT OR ROLLBACK INTO table DEFAULT VALUES;", SQL::ConflictResolution::Rollback, {}, "table", {}, {}, {}, false);

    auto resolution = SQL::ConflictResolution::Abort;
    validate("INSERT INTO table DEFAULT VALUES;", resolution, {}, "table", {}, {}, {}, false);
    validate("INSERT INTO schema.table DEFAULT VALUES;", resolution, "schema", "table", {}, {}, {}, false);
    validate("INSERT INTO table AS foo DEFAULT VALUES;", resolution, {}, "table", "foo", {}, {}, false);

    validate("INSERT INTO table (column) DEFAULT VALUES;", resolution, {}, "table", {}, { "column" }, {}, false);
    validate("INSERT INTO table (column1, column2) DEFAULT VALUES;", resolution, {}, "table", {}, { "column1", "column2" }, {}, false);

    validate("INSERT INTO table VALUES (1);", resolution, {}, "table", {}, {}, { 1 }, false);
    validate("INSERT INTO table VALUES (1, 2);", resolution, {}, "table", {}, {}, { 2 }, false);
    validate("INSERT INTO table VALUES (1, 2), (3, 4, 5);", resolution, {}, "table", {}, {}, { 2, 3 }, false);

    validate("INSERT INTO table SELECT * FROM table;", resolution, {}, "table", {}, {}, {}, true);
}

TEST_CASE(update)
{
    EXPECT(parse("UPDATE").is_error());
    EXPECT(parse("UPDATE table").is_error());
    EXPECT(parse("UPDATE table SET").is_error());
    EXPECT(parse("UPDATE table SET column").is_error());
    EXPECT(parse("UPDATE table SET column=4").is_error());
    EXPECT(parse("UPDATE table SET column=4, ;").is_error());
    EXPECT(parse("UPDATE table SET (column)=4").is_error());
    EXPECT(parse("UPDATE table SET (column)=4, ;").is_error());
    EXPECT(parse("UPDATE table SET (column, )=4;").is_error());
    EXPECT(parse("UPDATE table SET column=4 FROM").is_error());
    EXPECT(parse("UPDATE table SET column=4 FROM table").is_error());
    EXPECT(parse("UPDATE table SET column=4 WHERE").is_error());
    EXPECT(parse("UPDATE table SET column=4 WHERE 1==1").is_error());
    EXPECT(parse("UPDATE table SET column=4 RETURNING").is_error());
    EXPECT(parse("UPDATE table SET column=4 RETURNING *").is_error());
    EXPECT(parse("UPDATE table SET column=4 RETURNING column").is_error());
    EXPECT(parse("UPDATE table SET column=4 RETURNING column AS").is_error());
    EXPECT(parse("UPDATE OR table SET column=4;").is_error());
    EXPECT(parse("UPDATE OR foo table SET column=4;").is_error());

    auto validate = [](StringView sql, SQL::ConflictResolution expected_conflict_resolution, StringView expected_schema, StringView expected_table, StringView expected_alias, Vector<Vector<String>> expected_update_columns, bool expect_where_clause, bool expect_returning_clause, Vector<StringView> expected_returned_column_aliases) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::Update>(*statement));

        const auto& update = static_cast<const SQL::Update&>(*statement);
        EXPECT_EQ(update.conflict_resolution(), expected_conflict_resolution);

        const auto& qualified_table_name = update.qualified_table_name();
        EXPECT_EQ(qualified_table_name->schema_name(), expected_schema);
        EXPECT_EQ(qualified_table_name->table_name(), expected_table);
        EXPECT_EQ(qualified_table_name->alias(), expected_alias);

        const auto& update_columns = update.update_columns();
        EXPECT_EQ(update_columns.size(), expected_update_columns.size());
        for (size_t i = 0; i < update_columns.size(); ++i) {
            const auto& update_column = update_columns[i];
            const auto& expected_update_column = expected_update_columns[i];
            EXPECT_EQ(update_column.column_names.size(), expected_update_column.size());
            EXPECT(!is<SQL::ErrorExpression>(*update_column.expression));

            for (size_t j = 0; j < update_column.column_names.size(); ++j)
                EXPECT_EQ(update_column.column_names[j], expected_update_column[j]);
        }

        const auto& where_clause = update.where_clause();
        EXPECT_EQ(where_clause.is_null(), !expect_where_clause);
        if (where_clause)
            EXPECT(!is<SQL::ErrorExpression>(*where_clause));

        const auto& returning_clause = update.returning_clause();
        EXPECT_EQ(returning_clause.is_null(), !expect_returning_clause);
        if (returning_clause) {
            EXPECT_EQ(returning_clause->columns().size(), expected_returned_column_aliases.size());

            for (size_t i = 0; i < returning_clause->columns().size(); ++i) {
                const auto& column = returning_clause->columns()[i];
                const auto& expected_column_alias = expected_returned_column_aliases[i];

                EXPECT(!is<SQL::ErrorExpression>(*column.expression));
                EXPECT_EQ(column.column_alias, expected_column_alias);
            }
        }
    };

    Vector<Vector<String>> update_columns { { "column" } };
    validate("UPDATE OR ABORT table SET column=1;", SQL::ConflictResolution::Abort, {}, "table", {}, update_columns, false, false, {});
    validate("UPDATE OR FAIL table SET column=1;", SQL::ConflictResolution::Fail, {}, "table", {}, update_columns, false, false, {});
    validate("UPDATE OR IGNORE table SET column=1;", SQL::ConflictResolution::Ignore, {}, "table", {}, update_columns, false, false, {});
    validate("UPDATE OR REPLACE table SET column=1;", SQL::ConflictResolution::Replace, {}, "table", {}, update_columns, false, false, {});
    validate("UPDATE OR ROLLBACK table SET column=1;", SQL::ConflictResolution::Rollback, {}, "table", {}, update_columns, false, false, {});

    auto resolution = SQL::ConflictResolution::Abort;
    validate("UPDATE table SET column=1;", resolution, {}, "table", {}, update_columns, false, false, {});
    validate("UPDATE schema.table SET column=1;", resolution, "schema", "table", {}, update_columns, false, false, {});
    validate("UPDATE table AS foo SET column=1;", resolution, {}, "table", "foo", update_columns, false, false, {});

    validate("UPDATE table SET column=1;", resolution, {}, "table", {}, { { "column" } }, false, false, {});
    validate("UPDATE table SET column1=1, column2=2;", resolution, {}, "table", {}, { { "column1" }, { "column2" } }, false, false, {});
    validate("UPDATE table SET (column1, column2)=1, column3=2;", resolution, {}, "table", {}, { { "column1", "column2" }, { "column3" } }, false, false, {});

    validate("UPDATE table SET column=1 WHERE 1==1;", resolution, {}, "table", {}, update_columns, true, false, {});

    validate("UPDATE table SET column=1 RETURNING *;", resolution, {}, "table", {}, update_columns, false, true, {});
    validate("UPDATE table SET column=1 RETURNING column;", resolution, {}, "table", {}, update_columns, false, true, { {} });
    validate("UPDATE table SET column=1 RETURNING column AS alias;", resolution, {}, "table", {}, update_columns, false, true, { "alias" });
    validate("UPDATE table SET column=1 RETURNING column1 AS alias1, column2 AS alias2;", resolution, {}, "table", {}, update_columns, false, true, { "alias1", "alias2" });
}

TEST_CASE(delete_)
{
    EXPECT(parse("DELETE").is_error());
    EXPECT(parse("DELETE FROM").is_error());
    EXPECT(parse("DELETE FROM table").is_error());
    EXPECT(parse("DELETE FROM table WHERE").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15 RETURNING").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15 RETURNING *").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15 RETURNING column").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15 RETURNING column AS;").is_error());
    EXPECT(parse("DELETE FROM table WHERE (');").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_alias, bool expect_where_clause, bool expect_returning_clause, Vector<StringView> expected_returned_column_aliases) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::Delete>(*statement));

        const auto& delete_ = static_cast<const SQL::Delete&>(*statement);

        const auto& qualified_table_name = delete_.qualified_table_name();
        EXPECT_EQ(qualified_table_name->schema_name(), expected_schema);
        EXPECT_EQ(qualified_table_name->table_name(), expected_table);
        EXPECT_EQ(qualified_table_name->alias(), expected_alias);

        const auto& where_clause = delete_.where_clause();
        EXPECT_EQ(where_clause.is_null(), !expect_where_clause);
        if (where_clause)
            EXPECT(!is<SQL::ErrorExpression>(*where_clause));

        const auto& returning_clause = delete_.returning_clause();
        EXPECT_EQ(returning_clause.is_null(), !expect_returning_clause);
        if (returning_clause) {
            EXPECT_EQ(returning_clause->columns().size(), expected_returned_column_aliases.size());

            for (size_t i = 0; i < returning_clause->columns().size(); ++i) {
                const auto& column = returning_clause->columns()[i];
                const auto& expected_column_alias = expected_returned_column_aliases[i];

                EXPECT(!is<SQL::ErrorExpression>(*column.expression));
                EXPECT_EQ(column.column_alias, expected_column_alias);
            }
        }
    };

    validate("DELETE FROM table;", {}, "table", {}, false, false, {});
    validate("DELETE FROM schema.table;", "schema", "table", {}, false, false, {});
    validate("DELETE FROM schema.table AS alias;", "schema", "table", "alias", false, false, {});
    validate("DELETE FROM table WHERE (1 == 1);", {}, "table", {}, true, false, {});
    validate("DELETE FROM table RETURNING *;", {}, "table", {}, false, true, {});
    validate("DELETE FROM table RETURNING column;", {}, "table", {}, false, true, { {} });
    validate("DELETE FROM table RETURNING column AS alias;", {}, "table", {}, false, true, { "alias" });
    validate("DELETE FROM table RETURNING column1 AS alias1, column2 AS alias2;", {}, "table", {}, false, true, { "alias1", "alias2" });
}

TEST_CASE(select)
{
    EXPECT(parse("SELECT").is_error());
    EXPECT(parse("SELECT;").is_error());
    EXPECT(parse("SELECT DISTINCT;").is_error());
    EXPECT(parse("SELECT ALL;").is_error());
    EXPECT(parse("SELECT *").is_error());
    EXPECT(parse("SELECT * FROM;").is_error());
    EXPECT(parse("SELECT table. FROM table;").is_error());
    EXPECT(parse("SELECT column AS FROM table;").is_error());
    EXPECT(parse("SELECT * FROM (").is_error());
    EXPECT(parse("SELECT * FROM ()").is_error());
    EXPECT(parse("SELECT * FROM ();").is_error());
    EXPECT(parse("SELECT * FROM (table1)").is_error());
    EXPECT(parse("SELECT * FROM (table1, )").is_error());
    EXPECT(parse("SELECT * FROM (table1, table2)").is_error());
    EXPECT(parse("SELECT * FROM table").is_error());
    EXPECT(parse("SELECT * FROM table AS;").is_error());
    EXPECT(parse("SELECT * FROM table WHERE;").is_error());
    EXPECT(parse("SELECT * FROM table WHERE 1 ==1").is_error());
    EXPECT(parse("SELECT * FROM table GROUP;").is_error());
    EXPECT(parse("SELECT * FROM table GROUP BY;").is_error());
    EXPECT(parse("SELECT * FROM table GROUP BY column").is_error());
    EXPECT(parse("SELECT * FROM table ORDER:").is_error());
    EXPECT(parse("SELECT * FROM table ORDER BY column").is_error());
    EXPECT(parse("SELECT * FROM table ORDER BY column COLLATE:").is_error());
    EXPECT(parse("SELECT * FROM table ORDER BY column COLLATE collation").is_error());
    EXPECT(parse("SELECT * FROM table ORDER BY column NULLS;").is_error());
    EXPECT(parse("SELECT * FROM table ORDER BY column NULLS SECOND;").is_error());
    EXPECT(parse("SELECT * FROM table LIMIT;").is_error());
    EXPECT(parse("SELECT * FROM table LIMIT 12").is_error());
    EXPECT(parse("SELECT * FROM table LIMIT 12 OFFSET;").is_error());
    EXPECT(parse("SELECT * FROM table LIMIT 12 OFFSET 15").is_error());
    EXPECT(parse("SELECT * FROM table LIMIT 15, 16;").is_error());

    struct Type {
        SQL::ResultType type;
        StringView table_name_or_column_alias {};
    };

    struct From {
        StringView schema_name;
        StringView table_name;
        StringView table_alias;
    };

    struct Ordering {
        String collation_name;
        SQL::Order order;
        SQL::Nulls nulls;
    };

    auto validate = [](StringView sql, Vector<Type> expected_columns, Vector<From> expected_from_list, bool expect_where_clause, size_t expected_group_by_size, bool expect_having_clause, Vector<Ordering> expected_ordering, bool expect_limit_clause, bool expect_offset_clause) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::Select>(*statement));

        const auto& select = static_cast<const SQL::Select&>(*statement);

        const auto& result_column_list = select.result_column_list();
        EXPECT_EQ(result_column_list.size(), expected_columns.size());
        for (size_t i = 0; i < result_column_list.size(); ++i) {
            const auto& result_column = result_column_list[i];
            const auto& expected_column = expected_columns[i];
            EXPECT_EQ(result_column.type(), expected_column.type);

            switch (result_column.type()) {
            case SQL::ResultType::All:
                EXPECT(expected_column.table_name_or_column_alias.is_null());
                break;
            case SQL::ResultType::Table:
                EXPECT_EQ(result_column.table_name(), expected_column.table_name_or_column_alias);
                break;
            case SQL::ResultType::Expression:
                EXPECT_EQ(result_column.column_alias(), expected_column.table_name_or_column_alias);
                break;
            }
        }

        const auto& table_or_subquery_list = select.table_or_subquery_list();
        EXPECT_EQ(table_or_subquery_list.size(), expected_from_list.size());
        for (size_t i = 0; i < table_or_subquery_list.size(); ++i) {
            const auto& result_from = table_or_subquery_list[i];
            const auto& expected_from = expected_from_list[i];
            EXPECT_EQ(result_from.schema_name(), expected_from.schema_name);
            EXPECT_EQ(result_from.table_name(), expected_from.table_name);
            EXPECT_EQ(result_from.table_alias(), expected_from.table_alias);
        }

        const auto& where_clause = select.where_clause();
        EXPECT_EQ(where_clause.is_null(), !expect_where_clause);
        if (where_clause)
            EXPECT(!is<SQL::ErrorExpression>(*where_clause));

        const auto& group_by_clause = select.group_by_clause();
        EXPECT_EQ(group_by_clause.is_null(), (expected_group_by_size == 0));
        if (group_by_clause) {
            const auto& group_by_list = group_by_clause->group_by_list();
            EXPECT_EQ(group_by_list.size(), expected_group_by_size);
            for (size_t i = 0; i < group_by_list.size(); ++i)
                EXPECT(!is<SQL::ErrorExpression>(group_by_list[i]));

            const auto& having_clause = group_by_clause->having_clause();
            EXPECT_EQ(having_clause.is_null(), !expect_having_clause);
            if (having_clause)
                EXPECT(!is<SQL::ErrorExpression>(*having_clause));
        }

        const auto& ordering_term_list = select.ordering_term_list();
        EXPECT_EQ(ordering_term_list.size(), expected_ordering.size());
        for (size_t i = 0; i < ordering_term_list.size(); ++i) {
            const auto& result_order = ordering_term_list[i];
            const auto& expected_order = expected_ordering[i];
            EXPECT(!is<SQL::ErrorExpression>(*result_order.expression()));
            EXPECT_EQ(result_order.collation_name(), expected_order.collation_name);
            EXPECT_EQ(result_order.order(), expected_order.order);
            EXPECT_EQ(result_order.nulls(), expected_order.nulls);
        }

        const auto& limit_clause = select.limit_clause();
        EXPECT_EQ(limit_clause.is_null(), !expect_limit_clause);
        if (limit_clause) {
            const auto& limit_expression = limit_clause->limit_expression();
            EXPECT(!is<SQL::ErrorExpression>(*limit_expression));

            const auto& offset_expression = limit_clause->offset_expression();
            EXPECT_EQ(offset_expression.is_null(), !expect_offset_clause);
            if (offset_expression)
                EXPECT(!is<SQL::ErrorExpression>(*offset_expression));
        }
    };

    Vector<Type> all { { SQL::ResultType::All } };
    Vector<From> from { { {}, "table", {} } };

    validate("SELECT * FROM table;", { { SQL::ResultType::All } }, from, false, 0, false, {}, false, false);
    validate("SELECT table.* FROM table;", { { SQL::ResultType::Table, "table" } }, from, false, 0, false, {}, false, false);
    validate("SELECT column AS alias FROM table;", { { SQL::ResultType::Expression, "alias" } }, from, false, 0, false, {}, false, false);
    validate("SELECT table.column AS alias FROM table;", { { SQL::ResultType::Expression, "alias" } }, from, false, 0, false, {}, false, false);
    validate("SELECT schema.table.column AS alias FROM table;", { { SQL::ResultType::Expression, "alias" } }, from, false, 0, false, {}, false, false);
    validate("SELECT column AS alias, *, table.* FROM table;", { { SQL::ResultType::Expression, "alias" }, { SQL::ResultType::All }, { SQL::ResultType::Table, "table" } }, from, false, 0, false, {}, false, false);

    validate("SELECT * FROM table;", all, { { {}, "table", {} } }, false, 0, false, {}, false, false);
    validate("SELECT * FROM schema.table;", all, { { "schema", "table", {} } }, false, 0, false, {}, false, false);
    validate("SELECT * FROM schema.table AS alias;", all, { { "schema", "table", "alias" } }, false, 0, false, {}, false, false);
    validate("SELECT * FROM schema.table AS alias, table2, table3 AS table4;", all, { { "schema", "table", "alias" }, { {}, "table2", {} }, { {}, "table3", "table4" } }, false, 0, false, {}, false, false);

    validate("SELECT * FROM table WHERE column IS NOT NULL;", all, from, true, 0, false, {}, false, false);

    validate("SELECT * FROM table GROUP BY column;", all, from, false, 1, false, {}, false, false);
    validate("SELECT * FROM table GROUP BY column1, column2, column3;", all, from, false, 3, false, {}, false, false);
    validate("SELECT * FROM table GROUP BY column HAVING 'abc';", all, from, false, 1, true, {}, false, false);

    validate("SELECT * FROM table ORDER BY column;", all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table ORDER BY column COLLATE collation;", all, from, false, 0, false, { { "collation", SQL::Order::Ascending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table ORDER BY column ASC;", all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table ORDER BY column DESC;", all, from, false, 0, false, { { {}, SQL::Order::Descending, SQL::Nulls::Last } }, false, false);
    validate("SELECT * FROM table ORDER BY column ASC NULLS LAST;", all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::Last } }, false, false);
    validate("SELECT * FROM table ORDER BY column DESC NULLS FIRST;", all, from, false, 0, false, { { {}, SQL::Order::Descending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table ORDER BY column1, column2 DESC, column3 NULLS LAST;", all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::First }, { {}, SQL::Order::Descending, SQL::Nulls::Last }, { {}, SQL::Order::Ascending, SQL::Nulls::Last } }, false, false);

    validate("SELECT * FROM table LIMIT 15;", all, from, false, 0, false, {}, true, false);
    validate("SELECT * FROM table LIMIT 15 OFFSET 16;", all, from, false, 0, false, {}, true, true);
}

TEST_CASE(common_table_expression)
{
    EXPECT(parse("WITH").is_error());
    EXPECT(parse("WITH;").is_error());
    EXPECT(parse("WITH DELETE FROM table;").is_error());
    EXPECT(parse("WITH table DELETE FROM table;").is_error());
    EXPECT(parse("WITH table AS DELETE FROM table;").is_error());
    EXPECT(parse("WITH RECURSIVE table DELETE FROM table;").is_error());
    EXPECT(parse("WITH RECURSIVE table AS DELETE FROM table;").is_error());

    // Below are otherwise valid common-table-expressions, but attached to statements which do not allow them.
    EXPECT(parse("WITH table AS (SELECT * AS TABLE) CREATE TABLE test ( column1 );").is_error());
    EXPECT(parse("WITH table AS (SELECT * FROM table) DROP TABLE test;").is_error());

    struct SelectedTableList {
        struct SelectedTable {
            StringView table_name {};
            Vector<StringView> column_names {};
        };

        bool recursive { false };
        Vector<SelectedTable> selected_tables {};
    };

    auto validate = [](StringView sql, SelectedTableList expected_selected_tables) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::Delete>(*statement));

        const auto& delete_ = static_cast<const SQL::Delete&>(*statement);

        const auto& common_table_expression_list = delete_.common_table_expression_list();
        EXPECT(!common_table_expression_list.is_null());

        EXPECT_EQ(common_table_expression_list->recursive(), expected_selected_tables.recursive);

        const auto& common_table_expressions = common_table_expression_list->common_table_expressions();
        EXPECT_EQ(common_table_expressions.size(), expected_selected_tables.selected_tables.size());

        for (size_t i = 0; i < common_table_expressions.size(); ++i) {
            const auto& common_table_expression = common_table_expressions[i];
            const auto& expected_common_table_expression = expected_selected_tables.selected_tables[i];
            EXPECT_EQ(common_table_expression.table_name(), expected_common_table_expression.table_name);
            EXPECT_EQ(common_table_expression.column_names().size(), expected_common_table_expression.column_names.size());

            for (size_t j = 0; j < common_table_expression.column_names().size(); ++j)
                EXPECT_EQ(common_table_expression.column_names()[j], expected_common_table_expression.column_names[j]);
        }
    };

    validate("WITH table AS (SELECT * FROM table) DELETE FROM table;", { false, { { "table" } } });
    validate("WITH table (column) AS (SELECT * FROM table) DELETE FROM table;", { false, { { "table", { "column" } } } });
    validate("WITH table (column1, column2) AS (SELECT * FROM table) DELETE FROM table;", { false, { { "table", { "column1", "column2" } } } });
    validate("WITH RECURSIVE table AS (SELECT * FROM table) DELETE FROM table;", { true, { { "table", {} } } });
}
