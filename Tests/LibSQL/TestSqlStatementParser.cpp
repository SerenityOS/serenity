/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <AK/Result.h>
#include <AK/StringView.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/Parser.h>

namespace {

using ParseResult = AK::Result<NonnullRefPtr<SQL::AST::Statement>, ByteString>;

ParseResult parse(StringView sql)
{
    auto parser = SQL::AST::Parser(SQL::AST::Lexer(sql));
    auto statement = parser.next_statement();

    if (parser.has_errors()) {
        return parser.errors()[0].to_byte_string();
    }

    return statement;
}

}

TEST_CASE(create_table)
{
    EXPECT(parse("CREATE TABLE"sv).is_error());
    EXPECT(parse("CREATE TABLE test"sv).is_error());
    EXPECT(parse("CREATE TABLE test ()"sv).is_error());
    EXPECT(parse("CREATE TABLE test ();"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 "sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 )"sv).is_error());
    EXPECT(parse("CREATE TABLE IF test ( column1 );"sv).is_error());
    EXPECT(parse("CREATE TABLE IF NOT test ( column1 );"sv).is_error());
    EXPECT(parse("CREATE TABLE AS;"sv).is_error());
    EXPECT(parse("CREATE TABLE AS SELECT;"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar()"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(abc)"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123 )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123,  )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123, ) )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(.) )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(.abc) )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(0x) )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(0xzzz) )"sv).is_error());
    EXPECT(parse("CREATE TABLE test ( column1 int ) AS SELECT * FROM table_name;"sv).is_error());
    EXPECT(parse("CREATE TABLE test AS SELECT * FROM table_name ( column1 int ) ;"sv).is_error());

    struct Column {
        StringView name;
        StringView type;
        Vector<double> signed_numbers {};
    };

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, Vector<Column> expected_columns, bool expected_is_temporary = false, bool expected_is_error_if_table_exists = true) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::CreateTable>(*statement));

        auto const& table = static_cast<const SQL::AST::CreateTable&>(*statement);
        EXPECT_EQ(table.schema_name(), expected_schema);
        EXPECT_EQ(table.table_name(), expected_table);
        EXPECT_EQ(table.is_temporary(), expected_is_temporary);
        EXPECT_EQ(table.is_error_if_table_exists(), expected_is_error_if_table_exists);

        bool expect_select_statement = expected_columns.is_empty();
        EXPECT_EQ(table.has_selection(), expect_select_statement);
        EXPECT_EQ(table.has_columns(), !expect_select_statement);

        auto const& select_statement = table.select_statement();
        EXPECT_EQ(select_statement.is_null(), !expect_select_statement);

        auto const& columns = table.columns();
        EXPECT_EQ(columns.size(), expected_columns.size());

        for (size_t i = 0; i < columns.size(); ++i) {
            auto const& column = columns[i];
            auto const& expected_column = expected_columns[i];
            EXPECT_EQ(column->name(), expected_column.name);

            auto const& type_name = column->type_name();
            EXPECT_EQ(type_name->name(), expected_column.type);

            auto const& signed_numbers = type_name->signed_numbers();
            EXPECT_EQ(signed_numbers.size(), expected_column.signed_numbers.size());

            for (size_t j = 0; j < signed_numbers.size(); ++j) {
                double signed_number = signed_numbers[j]->value();
                double expected_signed_number = expected_column.signed_numbers[j];
                EXPECT_EQ(signed_number, expected_signed_number);
            }
        }
    };

    validate("CREATE TABLE test ( column1 );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } });
    validate("Create Table test ( column1 );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } });
    validate(R"(CREATE TABLE "test" ( "column1" );)"sv, {}, "test"sv, { { "column1"sv, "BLOB"sv } });
    validate(R"(CREATE TABLE "te""st" ( "co""lumn1" );)"sv, {}, "te\"st"sv, { { "co\"lumn1"sv, "BLOB"sv } });
    validate("CREATE TABLE schema_name.test ( column1 );"sv, "SCHEMA_NAME"sv, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } });
    validate("CREATE TABLE \"schema\".test ( column1 );"sv, "schema"sv, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } });
    validate("CREATE TEMP TABLE test ( column1 );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } }, true, true);
    validate("CREATE TEMPORARY TABLE test ( column1 );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } }, true, true);
    validate("CREATE TABLE IF NOT EXISTS test ( column1 );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "BLOB"sv } }, false, false);

    validate("CREATE TABLE test AS SELECT * FROM table_name;"sv, {}, "TEST"sv, {});

    validate("CREATE TABLE test ( column1 int );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "INT"sv } });
    validate("CREATE TABLE test ( column1 varchar );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv } });
    validate("CREATE TABLE test ( column1 varchar(255) );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv, { 255 } } });
    validate("CREATE TABLE test ( column1 varchar(255, 123) );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv, { 255, 123 } } });
    validate("CREATE TABLE test ( column1 varchar(255, -123) );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv, { 255, -123 } } });
    validate("CREATE TABLE test ( column1 varchar(0xff) );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv, { 255 } } });
    validate("CREATE TABLE test ( column1 varchar(3.14) );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv, { 3.14 } } });
    validate("CREATE TABLE test ( column1 varchar(1e3) );"sv, {}, "TEST"sv, { { "COLUMN1"sv, "VARCHAR"sv, { 1000 } } });
}

TEST_CASE(alter_table)
{
    // This test case only contains common error cases of the AlterTable subclasses.
    EXPECT(parse("ALTER"sv).is_error());
    EXPECT(parse("ALTER TABLE"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name;"sv).is_error());
}

TEST_CASE(alter_table_rename_table)
{
    EXPECT(parse("ALTER TABLE table_name RENAME"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME TO"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME TO new_table"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_new_table) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::RenameTable>(*statement));

        auto const& alter = static_cast<const SQL::AST::RenameTable&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);
        EXPECT_EQ(alter.new_table_name(), expected_new_table);
    };

    validate("ALTER TABLE table_name RENAME TO new_table;"sv, {}, "TABLE_NAME"sv, "NEW_TABLE"sv);
    validate("ALTER TABLE schema_name.table_name RENAME TO new_table;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "NEW_TABLE"sv);
}

TEST_CASE(alter_table_rename_column)
{
    EXPECT(parse("ALTER TABLE table_name RENAME"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME COLUMN"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME COLUMN column_name"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME COLUMN column_name TO"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME COLUMN column_name TO new_column"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME column_name"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME column_name TO"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name RENAME column_name TO new_column"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column, StringView expected_new_column) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::RenameColumn>(*statement));

        auto const& alter = static_cast<const SQL::AST::RenameColumn&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);
        EXPECT_EQ(alter.column_name(), expected_column);
        EXPECT_EQ(alter.new_column_name(), expected_new_column);
    };

    validate("ALTER TABLE table_name RENAME column_name TO new_column;"sv, {}, "TABLE_NAME"sv, "COLUMN_NAME"sv, "NEW_COLUMN"sv);
    validate("ALTER TABLE table_name RENAME COLUMN column_name TO new_column;"sv, {}, "TABLE_NAME"sv, "COLUMN_NAME"sv, "NEW_COLUMN"sv);
    validate("ALTER TABLE schema_name.table_name RENAME column_name TO new_column;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "COLUMN_NAME"sv, "NEW_COLUMN"sv);
    validate("ALTER TABLE schema_name.table_name RENAME COLUMN column_name TO new_column;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "COLUMN_NAME"sv, "NEW_COLUMN"sv);
}

TEST_CASE(alter_table_add_column)
{
    EXPECT(parse("ALTER TABLE table_name ADD"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name ADD COLUMN"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name ADD COLUMN column_name"sv).is_error());

    struct Column {
        StringView name;
        StringView type;
        Vector<double> signed_numbers {};
    };

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, Column expected_column) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::AddColumn>(*statement));

        auto const& alter = static_cast<const SQL::AST::AddColumn&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);

        auto const& column = alter.column();
        EXPECT_EQ(column->name(), expected_column.name);

        auto const& type_name = column->type_name();
        EXPECT_EQ(type_name->name(), expected_column.type);

        auto const& signed_numbers = type_name->signed_numbers();
        EXPECT_EQ(signed_numbers.size(), expected_column.signed_numbers.size());

        for (size_t j = 0; j < signed_numbers.size(); ++j) {
            double signed_number = signed_numbers[j]->value();
            double expected_signed_number = expected_column.signed_numbers[j];
            EXPECT_EQ(signed_number, expected_signed_number);
        }
    };

    validate("ALTER TABLE test ADD column1;"sv, {}, "TEST"sv, { "COLUMN1"sv, "BLOB"sv });
    validate("ALTER TABLE test ADD column1 int;"sv, {}, "TEST"sv, { "COLUMN1"sv, "INT"sv });
    validate("ALTER TABLE test ADD column1 varchar;"sv, {}, "TEST"sv, { "COLUMN1"sv, "VARCHAR"sv });
    validate("ALTER TABLE test ADD column1 varchar(255);"sv, {}, "TEST"sv, { "COLUMN1"sv, "VARCHAR"sv, { 255 } });
    validate("ALTER TABLE test ADD column1 varchar(255, 123);"sv, {}, "TEST"sv, { "COLUMN1"sv, "VARCHAR"sv, { 255, 123 } });

    validate("ALTER TABLE schema_name.test ADD COLUMN column1;"sv, "SCHEMA_NAME"sv, "TEST"sv, { "COLUMN1"sv, "BLOB"sv });
    validate("ALTER TABLE schema_name.test ADD COLUMN column1 int;"sv, "SCHEMA_NAME"sv, "TEST"sv, { "COLUMN1"sv, "INT"sv });
    validate("ALTER TABLE schema_name.test ADD COLUMN column1 varchar;"sv, "SCHEMA_NAME"sv, "TEST"sv, { "COLUMN1"sv, "VARCHAR"sv });
    validate("ALTER TABLE schema_name.test ADD COLUMN column1 varchar(255);"sv, "SCHEMA_NAME"sv, "TEST"sv, { "COLUMN1"sv, "VARCHAR"sv, { 255 } });
    validate("ALTER TABLE schema_name.test ADD COLUMN column1 varchar(255, 123);"sv, "SCHEMA_NAME"sv, "TEST"sv, { "COLUMN1"sv, "VARCHAR"sv, { 255, 123 } });
}

TEST_CASE(alter_table_drop_column)
{
    EXPECT(parse("ALTER TABLE table_name DROP"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name DROP COLUMN"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name DROP column_name"sv).is_error());
    EXPECT(parse("ALTER TABLE table_name DROP COLUMN column_name"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::DropColumn>(*statement));

        auto const& alter = static_cast<const SQL::AST::DropColumn&>(*statement);
        EXPECT_EQ(alter.schema_name(), expected_schema);
        EXPECT_EQ(alter.table_name(), expected_table);
        EXPECT_EQ(alter.column_name(), expected_column);
    };

    validate("ALTER TABLE table_name DROP column_name;"sv, {}, "TABLE_NAME"sv, "COLUMN_NAME"sv);
    validate("ALTER TABLE table_name DROP COLUMN column_name;"sv, {}, "TABLE_NAME"sv, "COLUMN_NAME"sv);
    validate("ALTER TABLE schema_name.table_name DROP column_name;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "COLUMN_NAME"sv);
    validate("ALTER TABLE schema_name.table_name DROP COLUMN column_name;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "COLUMN_NAME"sv);
}

TEST_CASE(drop_table)
{
    EXPECT(parse("DROP"sv).is_error());
    EXPECT(parse("DROP TABLE"sv).is_error());
    EXPECT(parse("DROP TABLE test"sv).is_error());
    EXPECT(parse("DROP TABLE IF test;"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, bool expected_is_error_if_table_does_not_exist = true) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::DropTable>(*statement));

        auto const& table = static_cast<const SQL::AST::DropTable&>(*statement);
        EXPECT_EQ(table.schema_name(), expected_schema);
        EXPECT_EQ(table.table_name(), expected_table);
        EXPECT_EQ(table.is_error_if_table_does_not_exist(), expected_is_error_if_table_does_not_exist);
    };

    validate("DROP TABLE test;"sv, {}, "TEST"sv);
    validate("DROP TABLE schema_name.test;"sv, "SCHEMA_NAME"sv, "TEST"sv);
    validate("DROP TABLE IF EXISTS test;"sv, {}, "TEST"sv, false);
}

TEST_CASE(insert)
{
    EXPECT(parse("INSERT"sv).is_error());
    EXPECT(parse("INSERT INTO"sv).is_error());
    EXPECT(parse("INSERT INTO table_name"sv).is_error());
    EXPECT(parse("INSERT INTO table_name (column_name)"sv).is_error());
    EXPECT(parse("INSERT INTO table_name (column_name, ) DEFAULT VALUES;"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES ();"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES (1)"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES SELECT"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES EXISTS"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES NOT"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES EXISTS (SELECT 1)"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES (SELECT)"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES (EXISTS SELECT)"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES ((SELECT))"sv).is_error());
    EXPECT(parse("INSERT INTO table_name VALUES (EXISTS (SELECT))"sv).is_error());
    EXPECT(parse("INSERT INTO table_name SELECT"sv).is_error());
    EXPECT(parse("INSERT INTO table_name SELECT * from table_name"sv).is_error());
    EXPECT(parse("INSERT OR INTO table_name DEFAULT VALUES;"sv).is_error());
    EXPECT(parse("INSERT OR foo INTO table_name DEFAULT VALUES;"sv).is_error());

    auto validate = [](StringView sql, SQL::AST::ConflictResolution expected_conflict_resolution, StringView expected_schema, StringView expected_table, StringView expected_alias, Vector<StringView> expected_column_names, Vector<size_t> expected_chain_sizes, bool expect_select_statement) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::Insert>(*statement));

        auto const& insert = static_cast<const SQL::AST::Insert&>(*statement);
        EXPECT_EQ(insert.conflict_resolution(), expected_conflict_resolution);
        EXPECT_EQ(insert.schema_name(), expected_schema);
        EXPECT_EQ(insert.table_name(), expected_table);
        EXPECT_EQ(insert.alias(), expected_alias);

        auto const& column_names = insert.column_names();
        EXPECT_EQ(column_names.size(), expected_column_names.size());
        for (size_t i = 0; i < column_names.size(); ++i)
            EXPECT_EQ(column_names[i], expected_column_names[i]);

        EXPECT_EQ(insert.has_expressions(), !expected_chain_sizes.is_empty());
        if (insert.has_expressions()) {
            auto const& chained_expressions = insert.chained_expressions();
            EXPECT_EQ(chained_expressions.size(), expected_chain_sizes.size());

            for (size_t i = 0; i < chained_expressions.size(); ++i) {
                auto const& chained_expression = chained_expressions[i];
                auto const& expressions = chained_expression->expressions();
                EXPECT_EQ(expressions.size(), expected_chain_sizes[i]);

                for (auto const& expression : expressions)
                    EXPECT(!is<SQL::AST::ErrorExpression>(expression));
            }
        }

        EXPECT_EQ(insert.has_selection(), expect_select_statement);
        EXPECT_EQ(insert.default_values(), expected_chain_sizes.is_empty() && !expect_select_statement);
    };

    validate("INSERT OR ABORT INTO table_name DEFAULT VALUES;"sv, SQL::AST::ConflictResolution::Abort, {}, "TABLE_NAME"sv, {}, {}, {}, false);
    validate("INSERT OR FAIL INTO table_name DEFAULT VALUES;"sv, SQL::AST::ConflictResolution::Fail, {}, "TABLE_NAME"sv, {}, {}, {}, false);
    validate("INSERT OR IGNORE INTO table_name DEFAULT VALUES;"sv, SQL::AST::ConflictResolution::Ignore, {}, "TABLE_NAME"sv, {}, {}, {}, false);
    validate("INSERT OR REPLACE INTO table_name DEFAULT VALUES;"sv, SQL::AST::ConflictResolution::Replace, {}, "TABLE_NAME"sv, {}, {}, {}, false);
    validate("INSERT OR ROLLBACK INTO table_name DEFAULT VALUES;"sv, SQL::AST::ConflictResolution::Rollback, {}, "TABLE_NAME"sv, {}, {}, {}, false);

    auto resolution = SQL::AST::ConflictResolution::Abort;
    validate("INSERT INTO table_name DEFAULT VALUES;"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, {}, false);
    validate("INSERT INTO schema_name.table_name DEFAULT VALUES;"sv, resolution, "SCHEMA_NAME"sv, "TABLE_NAME"sv, {}, {}, {}, false);
    validate("INSERT INTO table_name AS foo DEFAULT VALUES;"sv, resolution, {}, "TABLE_NAME"sv, "FOO"sv, {}, {}, false);

    validate("INSERT INTO table_name (column_name) DEFAULT VALUES;"sv, resolution, {}, "TABLE_NAME"sv, {}, { "COLUMN_NAME"sv }, {}, false);
    validate("INSERT INTO table_name (column1, column2) DEFAULT VALUES;"sv, resolution, {}, "TABLE_NAME"sv, {}, { "COLUMN1"sv, "COLUMN2"sv }, {}, false);

    validate("INSERT INTO table_name VALUES (1);"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 1 }, false);
    validate("INSERT INTO table_name VALUES (1, 2);"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 2 }, false);
    validate("INSERT INTO table_name VALUES (1, 2), (3, 4, 5);"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 2, 3 }, false);

    validate("INSERT INTO table_name VALUES ((SELECT 1));"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 1 }, false);
    validate("INSERT INTO table_name VALUES (EXISTS (SELECT 1));"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 1 }, false);
    validate("INSERT INTO table_name VALUES (NOT EXISTS (SELECT 1));"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 1 }, false);
    validate("INSERT INTO table_name VALUES ((SELECT 1), (SELECT 1));"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 2 }, false);
    validate("INSERT INTO table_name VALUES ((SELECT 1), (SELECT 1)), ((SELECT 1), (SELECT 1), (SELECT 1));"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, { 2, 3 }, false);

    validate("INSERT INTO table_name SELECT * FROM table_name;"sv, resolution, {}, "TABLE_NAME"sv, {}, {}, {}, true);
}

TEST_CASE(update)
{
    EXPECT(parse("UPDATE"sv).is_error());
    EXPECT(parse("UPDATE table_name"sv).is_error());
    EXPECT(parse("UPDATE table_name SET"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4, ;"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name)=4"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name)=EXISTS"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name)=SELECT"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name)=(SELECT)"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name)=NOT (SELECT 1)"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name)=4, ;"sv).is_error());
    EXPECT(parse("UPDATE table_name SET (column_name, )=4;"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 FROM"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 FROM table_name"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE EXISTS"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE NOT"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE NOT EXISTS"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE SELECT"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE (SELECT)"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE NOT (SELECT)"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 WHERE 1==1"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 RETURNING"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 RETURNING *"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 RETURNING column_name"sv).is_error());
    EXPECT(parse("UPDATE table_name SET column_name=4 RETURNING column_name AS"sv).is_error());
    EXPECT(parse("UPDATE OR table_name SET column_name=4;"sv).is_error());
    EXPECT(parse("UPDATE OR foo table_name SET column_name=4;"sv).is_error());

    auto validate = [](StringView sql, SQL::AST::ConflictResolution expected_conflict_resolution, StringView expected_schema, StringView expected_table, StringView expected_alias, Vector<Vector<ByteString>> expected_update_columns, bool expect_where_clause, bool expect_returning_clause, Vector<StringView> expected_returned_column_aliases) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::Update>(*statement));

        auto const& update = static_cast<const SQL::AST::Update&>(*statement);
        EXPECT_EQ(update.conflict_resolution(), expected_conflict_resolution);

        auto const& qualified_table_name = update.qualified_table_name();
        EXPECT_EQ(qualified_table_name->schema_name(), expected_schema);
        EXPECT_EQ(qualified_table_name->table_name(), expected_table);
        EXPECT_EQ(qualified_table_name->alias(), expected_alias);

        auto const& update_columns = update.update_columns();
        EXPECT_EQ(update_columns.size(), expected_update_columns.size());
        for (size_t i = 0; i < update_columns.size(); ++i) {
            auto const& update_column = update_columns[i];
            auto const& expected_update_column = expected_update_columns[i];
            EXPECT_EQ(update_column.column_names.size(), expected_update_column.size());
            EXPECT(!is<SQL::AST::ErrorExpression>(*update_column.expression));

            for (size_t j = 0; j < update_column.column_names.size(); ++j)
                EXPECT_EQ(update_column.column_names[j], expected_update_column[j]);
        }

        auto const& where_clause = update.where_clause();
        EXPECT_EQ(where_clause.is_null(), !expect_where_clause);
        if (where_clause)
            EXPECT(!is<SQL::AST::ErrorExpression>(*where_clause));

        auto const& returning_clause = update.returning_clause();
        EXPECT_EQ(returning_clause.is_null(), !expect_returning_clause);
        if (returning_clause) {
            EXPECT_EQ(returning_clause->columns().size(), expected_returned_column_aliases.size());

            for (size_t i = 0; i < returning_clause->columns().size(); ++i) {
                auto const& column = returning_clause->columns()[i];
                auto const& expected_column_alias = expected_returned_column_aliases[i];

                EXPECT(!is<SQL::AST::ErrorExpression>(*column.expression));
                EXPECT_EQ(column.column_alias, expected_column_alias);
            }
        }
    };

    Vector<Vector<ByteString>> update_columns { { "COLUMN_NAME" } };
    validate("UPDATE OR ABORT table_name SET column_name=1;"sv, SQL::AST::ConflictResolution::Abort, {}, "TABLE_NAME"sv, {}, update_columns, false, false, {});
    validate("UPDATE OR FAIL table_name SET column_name=1;"sv, SQL::AST::ConflictResolution::Fail, {}, "TABLE_NAME"sv, {}, update_columns, false, false, {});
    validate("UPDATE OR IGNORE table_name SET column_name=1;"sv, SQL::AST::ConflictResolution::Ignore, {}, "TABLE_NAME"sv, {}, update_columns, false, false, {});
    validate("UPDATE OR REPLACE table_name SET column_name=1;"sv, SQL::AST::ConflictResolution::Replace, {}, "TABLE_NAME"sv, {}, update_columns, false, false, {});
    validate("UPDATE OR ROLLBACK table_name SET column_name=1;"sv, SQL::AST::ConflictResolution::Rollback, {}, "TABLE_NAME"sv, {}, update_columns, false, false, {});

    auto resolution = SQL::AST::ConflictResolution::Abort;
    validate("UPDATE table_name SET column_name=1;"sv, resolution, {}, "TABLE_NAME"sv, {}, update_columns, false, false, {});
    validate("UPDATE schema_name.table_name SET column_name=1;"sv, resolution, "SCHEMA_NAME"sv, "TABLE_NAME"sv, {}, update_columns, false, false, {});
    validate("UPDATE table_name AS foo SET column_name=1;"sv, resolution, {}, "TABLE_NAME"sv, "FOO"sv, update_columns, false, false, {});

    validate("UPDATE table_name SET column_name=1;"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, false, false, {});
    validate("UPDATE table_name SET column_name=(SELECT 1);"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, false, false, {});
    validate("UPDATE table_name SET column_name=EXISTS (SELECT 1);"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, false, false, {});
    validate("UPDATE table_name SET column_name=NOT EXISTS (SELECT 1);"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, false, false, {});
    validate("UPDATE table_name SET column1=1, column2=2;"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN1"sv }, { "COLUMN2"sv } }, false, false, {});
    validate("UPDATE table_name SET (column1, column2)=1, column3=2;"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN1"sv, "COLUMN2"sv }, { "COLUMN3"sv } }, false, false, {});

    validate("UPDATE table_name SET column_name=1 WHERE 1==1;"sv, resolution, {}, "TABLE_NAME"sv, {}, update_columns, true, false, {});

    validate("UPDATE table_name SET column_name=1 WHERE (SELECT 1);"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, true, false, {});
    validate("UPDATE table_name SET column_name=1 WHERE EXISTS (SELECT 1);"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, true, false, {});
    validate("UPDATE table_name SET column_name=1 WHERE NOT EXISTS (SELECT 1);"sv, resolution, {}, "TABLE_NAME"sv, {}, { { "COLUMN_NAME"sv } }, true, false, {});

    validate("UPDATE table_name SET column_name=1 RETURNING *;"sv, resolution, {}, "TABLE_NAME"sv, {}, update_columns, false, true, {});
    validate("UPDATE table_name SET column_name=1 RETURNING column_name;"sv, resolution, {}, "TABLE_NAME"sv, {}, update_columns, false, true, { {} });
    validate("UPDATE table_name SET column_name=1 RETURNING column_name AS alias;"sv, resolution, {}, "TABLE_NAME"sv, {}, update_columns, false, true, { "ALIAS"sv });
    validate("UPDATE table_name SET column_name=1 RETURNING column1 AS alias1, column2 AS alias2;"sv, resolution, {}, "TABLE_NAME"sv, {}, update_columns, false, true, { "ALIAS1"sv, "ALIAS2"sv });
}

TEST_CASE(delete_)
{
    EXPECT(parse("DELETE"sv).is_error());
    EXPECT(parse("DELETE FROM"sv).is_error());
    EXPECT(parse("DELETE FROM table_name"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE EXISTS"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE NOT"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE NOT (SELECT 1)"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE NOT EXISTS"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE SELECT"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE (SELECT)"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE 15"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE 15 RETURNING"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE 15 RETURNING *"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE 15 RETURNING column_name"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE 15 RETURNING column_name AS;"sv).is_error());
    EXPECT(parse("DELETE FROM table_name WHERE (');"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_alias, bool expect_where_clause, bool expect_returning_clause, Vector<StringView> expected_returned_column_aliases) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::Delete>(*statement));

        auto const& delete_ = static_cast<const SQL::AST::Delete&>(*statement);

        auto const& qualified_table_name = delete_.qualified_table_name();
        EXPECT_EQ(qualified_table_name->schema_name(), expected_schema);
        EXPECT_EQ(qualified_table_name->table_name(), expected_table);
        EXPECT_EQ(qualified_table_name->alias(), expected_alias);

        auto const& where_clause = delete_.where_clause();
        EXPECT_EQ(where_clause.is_null(), !expect_where_clause);
        if (where_clause)
            EXPECT(!is<SQL::AST::ErrorExpression>(*where_clause));

        auto const& returning_clause = delete_.returning_clause();
        EXPECT_EQ(returning_clause.is_null(), !expect_returning_clause);
        if (returning_clause) {
            EXPECT_EQ(returning_clause->columns().size(), expected_returned_column_aliases.size());

            for (size_t i = 0; i < returning_clause->columns().size(); ++i) {
                auto const& column = returning_clause->columns()[i];
                auto const& expected_column_alias = expected_returned_column_aliases[i];

                EXPECT(!is<SQL::AST::ErrorExpression>(*column.expression));
                EXPECT_EQ(column.column_alias, expected_column_alias);
            }
        }
    };

    validate("DELETE FROM table_name;"sv, {}, "TABLE_NAME"sv, {}, false, false, {});
    validate("DELETE FROM schema_name.table_name;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, {}, false, false, {});
    validate("DELETE FROM schema_name.table_name AS alias;"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "ALIAS"sv, false, false, {});
    validate("DELETE FROM table_name WHERE (1 == 1);"sv, {}, "TABLE_NAME"sv, {}, true, false, {});
    validate("DELETE FROM table_name WHERE EXISTS (SELECT 1);"sv, {}, "TABLE_NAME"sv, {}, true, false, {});
    validate("DELETE FROM table_name WHERE NOT EXISTS (SELECT 1);"sv, {}, "TABLE_NAME"sv, {}, true, false, {});
    validate("DELETE FROM table_name WHERE (SELECT 1);"sv, {}, "TABLE_NAME"sv, {}, true, false, {});
    validate("DELETE FROM table_name RETURNING *;"sv, {}, "TABLE_NAME"sv, {}, false, true, {});
    validate("DELETE FROM table_name RETURNING column_name;"sv, {}, "TABLE_NAME"sv, {}, false, true, { {} });
    validate("DELETE FROM table_name RETURNING column_name AS alias;"sv, {}, "TABLE_NAME"sv, {}, false, true, { "ALIAS"sv });
    validate("DELETE FROM table_name RETURNING column1 AS alias1, column2 AS alias2;"sv, {}, "TABLE_NAME"sv, {}, false, true, { "ALIAS1"sv, "ALIAS2"sv });
}

TEST_CASE(select)
{
    EXPECT(parse("SELECT"sv).is_error());
    EXPECT(parse("SELECT;"sv).is_error());
    EXPECT(parse("SELECT DISTINCT;"sv).is_error());
    EXPECT(parse("SELECT ALL;"sv).is_error());
    EXPECT(parse("SELECT *"sv).is_error());
    EXPECT(parse("SELECT * FROM;"sv).is_error());
    EXPECT(parse("SELECT table_name. FROM table_name;"sv).is_error());
    EXPECT(parse("SELECT column_name AS FROM table_name;"sv).is_error());
    EXPECT(parse("SELECT * FROM ("sv).is_error());
    EXPECT(parse("SELECT * FROM ()"sv).is_error());
    EXPECT(parse("SELECT * FROM ();"sv).is_error());
    EXPECT(parse("SELECT * FROM (table_name1)"sv).is_error());
    EXPECT(parse("SELECT * FROM (table_name1, )"sv).is_error());
    EXPECT(parse("SELECT * FROM (table_name1, table_name2)"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name AS;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name WHERE;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name WHERE 1 ==1"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name GROUP;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name GROUP BY;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name GROUP BY column_name"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name ORDER:"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name ORDER BY column_name"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name ORDER BY column_name COLLATE:"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name ORDER BY column_name COLLATE collation"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name ORDER BY column_name NULLS;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name ORDER BY column_name NULLS SECOND;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name LIMIT;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name LIMIT 12"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name LIMIT 12 OFFSET;"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name LIMIT 12 OFFSET 15"sv).is_error());
    EXPECT(parse("SELECT * FROM table_name LIMIT 15, 16;"sv).is_error());

    struct Type {
        SQL::AST::ResultType type;
        StringView table_name_or_column_alias {};
    };

    struct From {
        StringView schema_name;
        StringView table_name;
        StringView table_alias;
    };

    struct Ordering {
        ByteString collation_name;
        SQL::Order order;
        SQL::Nulls nulls;
    };

    auto validate = [](StringView sql, Vector<Type> expected_columns, Vector<From> expected_from_list, bool expect_where_clause, size_t expected_group_by_size, bool expect_having_clause, Vector<Ordering> expected_ordering, bool expect_limit_clause, bool expect_offset_clause) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::Select>(*statement));

        auto const& select = static_cast<const SQL::AST::Select&>(*statement);

        auto const& result_column_list = select.result_column_list();
        EXPECT_EQ(result_column_list.size(), expected_columns.size());
        for (size_t i = 0; i < result_column_list.size(); ++i) {
            auto const& result_column = result_column_list[i];
            auto const& expected_column = expected_columns[i];
            EXPECT_EQ(result_column->type(), expected_column.type);

            switch (result_column->type()) {
            case SQL::AST::ResultType::All:
                EXPECT(expected_column.table_name_or_column_alias.is_null());
                break;
            case SQL::AST::ResultType::Table:
                EXPECT_EQ(result_column->table_name(), expected_column.table_name_or_column_alias);
                break;
            case SQL::AST::ResultType::Expression:
                EXPECT_EQ(result_column->column_alias(), expected_column.table_name_or_column_alias);
                break;
            }
        }

        auto const& table_or_subquery_list = select.table_or_subquery_list();
        EXPECT_EQ(table_or_subquery_list.size(), expected_from_list.size());
        for (size_t i = 0; i < table_or_subquery_list.size(); ++i) {
            auto const& result_from = table_or_subquery_list[i];
            auto const& expected_from = expected_from_list[i];
            EXPECT_EQ(result_from->schema_name(), expected_from.schema_name);
            EXPECT_EQ(result_from->table_name(), expected_from.table_name);
            EXPECT_EQ(result_from->table_alias(), expected_from.table_alias);
        }

        auto const& where_clause = select.where_clause();
        EXPECT_EQ(where_clause.is_null(), !expect_where_clause);
        if (where_clause)
            EXPECT(!is<SQL::AST::ErrorExpression>(*where_clause));

        auto const& group_by_clause = select.group_by_clause();
        EXPECT_EQ(group_by_clause.is_null(), (expected_group_by_size == 0));
        if (group_by_clause) {
            auto const& group_by_list = group_by_clause->group_by_list();
            EXPECT_EQ(group_by_list.size(), expected_group_by_size);
            for (size_t i = 0; i < group_by_list.size(); ++i)
                EXPECT(!is<SQL::AST::ErrorExpression>(group_by_list[i]));

            auto const& having_clause = group_by_clause->having_clause();
            EXPECT_EQ(having_clause.is_null(), !expect_having_clause);
            if (having_clause)
                EXPECT(!is<SQL::AST::ErrorExpression>(*having_clause));
        }

        auto const& ordering_term_list = select.ordering_term_list();
        EXPECT_EQ(ordering_term_list.size(), expected_ordering.size());
        for (size_t i = 0; i < ordering_term_list.size(); ++i) {
            auto const& result_order = ordering_term_list[i];
            auto const& expected_order = expected_ordering[i];
            EXPECT(!is<SQL::AST::ErrorExpression>(*result_order->expression()));
            EXPECT_EQ(result_order->collation_name(), expected_order.collation_name);
            EXPECT_EQ(result_order->order(), expected_order.order);
            EXPECT_EQ(result_order->nulls(), expected_order.nulls);
        }

        auto const& limit_clause = select.limit_clause();
        EXPECT_EQ(limit_clause.is_null(), !expect_limit_clause);
        if (limit_clause) {
            auto const& limit_expression = limit_clause->limit_expression();
            EXPECT(!is<SQL::AST::ErrorExpression>(*limit_expression));

            auto const& offset_expression = limit_clause->offset_expression();
            EXPECT_EQ(offset_expression.is_null(), !expect_offset_clause);
            if (offset_expression)
                EXPECT(!is<SQL::AST::ErrorExpression>(*offset_expression));
        }
    };

    Vector<Type> all { { SQL::AST::ResultType::All } };
    Vector<From> from { { {}, "TABLE_NAME"sv, {} } };

    validate("SELECT * FROM table_name;"sv, { { SQL::AST::ResultType::All } }, from, false, 0, false, {}, false, false);
    validate("SELECT table_name.* FROM table_name;"sv, { { SQL::AST::ResultType::Table, "TABLE_NAME"sv } }, from, false, 0, false, {}, false, false);
    validate("SELECT column_name AS alias FROM table_name;"sv, { { SQL::AST::ResultType::Expression, "ALIAS"sv } }, from, false, 0, false, {}, false, false);
    validate("SELECT table_name.column_name AS alias FROM table_name;"sv, { { SQL::AST::ResultType::Expression, "ALIAS"sv } }, from, false, 0, false, {}, false, false);
    validate("SELECT schema_name.table_name.column_name AS alias FROM table_name;"sv, { { SQL::AST::ResultType::Expression, "ALIAS"sv } }, from, false, 0, false, {}, false, false);
    validate("SELECT column_name AS alias, *, table_name.* FROM table_name;"sv, { { SQL::AST::ResultType::Expression, "ALIAS"sv }, { SQL::AST::ResultType::All }, { SQL::AST::ResultType::Table, "TABLE_NAME"sv } }, from, false, 0, false, {}, false, false);

    validate("SELECT * FROM table_name;"sv, all, { { {}, "TABLE_NAME"sv, {} } }, false, 0, false, {}, false, false);
    validate("SELECT * FROM schema_name.table_name;"sv, all, { { "SCHEMA_NAME"sv, "TABLE_NAME"sv, {} } }, false, 0, false, {}, false, false);
    validate("SELECT * FROM schema_name.table_name AS alias;"sv, all, { { "SCHEMA_NAME"sv, "TABLE_NAME"sv, "ALIAS"sv } }, false, 0, false, {}, false, false);
    validate("SELECT * FROM schema_name.table_name AS alias, table_name2, table_name3 AS table_name4;"sv, all, { { "SCHEMA_NAME"sv, "TABLE_NAME"sv, "ALIAS"sv }, { {}, "TABLE_NAME2"sv, {} }, { {}, "TABLE_NAME3"sv, "TABLE_NAME4"sv } }, false, 0, false, {}, false, false);

    validate("SELECT * FROM table_name WHERE column_name IS NOT NULL;"sv, all, from, true, 0, false, {}, false, false);

    validate("SELECT * FROM table_name GROUP BY column_name;"sv, all, from, false, 1, false, {}, false, false);
    validate("SELECT * FROM table_name GROUP BY column1, column2, column3;"sv, all, from, false, 3, false, {}, false, false);
    validate("SELECT * FROM table_name GROUP BY column_name HAVING 'abc';"sv, all, from, false, 1, true, {}, false, false);

    validate("SELECT * FROM table_name ORDER BY column_name;"sv, all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table_name ORDER BY column_name COLLATE collation;"sv, all, from, false, 0, false, { { "COLLATION"sv, SQL::Order::Ascending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table_name ORDER BY column_name ASC;"sv, all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table_name ORDER BY column_name DESC;"sv, all, from, false, 0, false, { { {}, SQL::Order::Descending, SQL::Nulls::Last } }, false, false);
    validate("SELECT * FROM table_name ORDER BY column_name ASC NULLS LAST;"sv, all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::Last } }, false, false);
    validate("SELECT * FROM table_name ORDER BY column_name DESC NULLS FIRST;"sv, all, from, false, 0, false, { { {}, SQL::Order::Descending, SQL::Nulls::First } }, false, false);
    validate("SELECT * FROM table_name ORDER BY column1, column2 DESC, column3 NULLS LAST;"sv, all, from, false, 0, false, { { {}, SQL::Order::Ascending, SQL::Nulls::First }, { {}, SQL::Order::Descending, SQL::Nulls::Last }, { {}, SQL::Order::Ascending, SQL::Nulls::Last } }, false, false);

    validate("SELECT * FROM table_name LIMIT 15;"sv, all, from, false, 0, false, {}, true, false);
    validate("SELECT * FROM table_name LIMIT 15 OFFSET 16;"sv, all, from, false, 0, false, {}, true, true);
}

TEST_CASE(common_table_expression)
{
    EXPECT(parse("WITH"sv).is_error());
    EXPECT(parse("WITH;"sv).is_error());
    EXPECT(parse("WITH DELETE FROM table_name;"sv).is_error());
    EXPECT(parse("WITH table_name DELETE FROM table_name;"sv).is_error());
    EXPECT(parse("WITH table_name AS DELETE FROM table_name;"sv).is_error());
    EXPECT(parse("WITH RECURSIVE table_name DELETE FROM table_name;"sv).is_error());
    EXPECT(parse("WITH RECURSIVE table_name AS DELETE FROM table_name;"sv).is_error());

    // Below are otherwise valid common-table-expressions, but attached to statements which do not allow them.
    EXPECT(parse("WITH table_name AS (SELECT * AS TABLE) CREATE TABLE test ( column1 );"sv).is_error());
    EXPECT(parse("WITH table_name AS (SELECT * FROM table_name) DROP TABLE test;"sv).is_error());

    struct SelectedTableList {
        struct SelectedTable {
            StringView table_name {};
            Vector<StringView> column_names {};
        };

        bool recursive { false };
        Vector<SelectedTable> selected_tables {};
    };

    auto validate = [](StringView sql, SelectedTableList expected_selected_tables) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::Delete>(*statement));

        auto const& delete_ = static_cast<const SQL::AST::Delete&>(*statement);

        auto const& common_table_expression_list = delete_.common_table_expression_list();
        EXPECT(!common_table_expression_list.is_null());

        EXPECT_EQ(common_table_expression_list->recursive(), expected_selected_tables.recursive);

        auto const& common_table_expressions = common_table_expression_list->common_table_expressions();
        EXPECT_EQ(common_table_expressions.size(), expected_selected_tables.selected_tables.size());

        for (size_t i = 0; i < common_table_expressions.size(); ++i) {
            auto const& common_table_expression = common_table_expressions[i];
            auto const& expected_common_table_expression = expected_selected_tables.selected_tables[i];
            EXPECT_EQ(common_table_expression->table_name(), expected_common_table_expression.table_name);
            EXPECT_EQ(common_table_expression->column_names().size(), expected_common_table_expression.column_names.size());

            for (size_t j = 0; j < common_table_expression->column_names().size(); ++j)
                EXPECT_EQ(common_table_expression->column_names()[j], expected_common_table_expression.column_names[j]);
        }
    };

    validate("WITH table_name AS (SELECT * FROM table_name) DELETE FROM table_name;"sv, { false, { { "TABLE_NAME"sv } } });
    validate("WITH table_name (column_name) AS (SELECT * FROM table_name) DELETE FROM table_name;"sv, { false, { { "TABLE_NAME"sv, { "COLUMN_NAME"sv } } } });
    validate("WITH table_name (column1, column2) AS (SELECT * FROM table_name) DELETE FROM table_name;"sv, { false, { { "TABLE_NAME"sv, { "COLUMN1"sv, "COLUMN2"sv } } } });
    validate("WITH RECURSIVE table_name AS (SELECT * FROM table_name) DELETE FROM table_name;"sv, { true, { { "TABLE_NAME"sv, {} } } });
}

TEST_CASE(nested_subquery_limit)
{
    auto subquery = ByteString::formatted("{:(^{}}table_name{:)^{}}", "", SQL::AST::Limits::maximum_subquery_depth - 1, "", SQL::AST::Limits::maximum_subquery_depth - 1);
    EXPECT(!parse(ByteString::formatted("SELECT * FROM {};"sv, subquery)).is_error());
    EXPECT(parse(ByteString::formatted("SELECT * FROM ({});"sv, subquery)).is_error());
}

TEST_CASE(bound_parameter_limit)
{
    auto subquery = ByteString::repeated("?, "sv, SQL::AST::Limits::maximum_bound_parameters);
    EXPECT(!parse(ByteString::formatted("INSERT INTO table_name VALUES ({}42);"sv, subquery)).is_error());
    EXPECT(parse(ByteString::formatted("INSERT INTO table_name VALUES ({}?);"sv, subquery)).is_error());
}

TEST_CASE(describe_table)
{
    EXPECT(parse("DESCRIBE"sv).is_error());
    EXPECT(parse("DESCRIBE;"sv).is_error());
    EXPECT(parse("DESCRIBE TABLE;"sv).is_error());
    EXPECT(parse("DESCRIBE table_name;"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table) {
        auto statement = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::DescribeTable>(*statement));

        auto const& describe_table_statement = static_cast<const SQL::AST::DescribeTable&>(*statement);
        EXPECT_EQ(describe_table_statement.qualified_table_name()->schema_name(), expected_schema);
        EXPECT_EQ(describe_table_statement.qualified_table_name()->table_name(), expected_table);
    };

    validate("DESCRIBE TABLE TableName;"sv, {}, "TABLENAME"sv);
    validate("DESCRIBE TABLE SchemaName.TableName;"sv, "SCHEMANAME"sv, "TABLENAME"sv);
}
