/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/ScopeGuard.h>
#include <LibSQL/AST/Parser.h>
#include <LibSQL/Database.h>
#include <LibSQL/Row.h>
#include <LibSQL/SQLResult.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

namespace {

constexpr const char* db_name = "/tmp/test.db";

RefPtr<SQL::SQLResult> execute(NonnullRefPtr<SQL::Database> database, String const& sql)
{
    auto parser = SQL::AST::Parser(SQL::AST::Lexer(sql));
    auto statement = parser.next_statement();
    EXPECT(!parser.has_errors());
    if (parser.has_errors())
        outln("{}", parser.errors()[0].to_string());
    auto result = statement->execute(move(database));
    if (result->error().code != SQL::SQLErrorCode::NoError)
        outln("{}", result->error().to_string());
    return result;
}

void create_schema(NonnullRefPtr<SQL::Database> database)
{
    auto result = execute(database, "CREATE SCHEMA TestSchema;");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 1);
}

void create_table(NonnullRefPtr<SQL::Database> database)
{
    create_schema(database);
    auto result = execute(database, "CREATE TABLE TestSchema.TestTable ( TextColumn text, IntColumn integer );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 1);
}

void create_two_tables(NonnullRefPtr<SQL::Database> database)
{
    create_schema(database);
    auto result = execute(database, "CREATE TABLE TestSchema.TestTable1 ( TextColumn1 text, IntColumn integer );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 1);
    result = execute(database, "CREATE TABLE TestSchema.TestTable2 ( TextColumn2 text, IntColumn integer );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 1);
}

TEST_CASE(create_schema)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_schema(database);
    auto schema_or_error = database->get_schema("TESTSCHEMA");
    EXPECT(!schema_or_error.is_error());
    EXPECT(schema_or_error.value());
}

TEST_CASE(create_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto table_or_error = database->get_table("TESTSCHEMA", "TESTTABLE");
    EXPECT(!table_or_error.is_error());
    EXPECT(table_or_error.value());
}

TEST_CASE(insert_into_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test', 42 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 1);

    auto table_or_error = database->get_table("TESTSCHEMA", "TESTTABLE");
    EXPECT(!table_or_error.is_error());
    auto table = table_or_error.value();

    int count = 0;
    auto rows_or_error = database->select_all(*table);
    EXPECT(!rows_or_error.is_error());
    for (auto& row : rows_or_error.value()) {
        EXPECT_EQ(row["TEXTCOLUMN"].to_string(), "Test");
        EXPECT_EQ(row["INTCOLUMN"].to_int().value(), 42);
        count++;
    }
    EXPECT_EQ(count, 1);
}

TEST_CASE(insert_into_table_wrong_data_types)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES (43, 'Test_2');");
    EXPECT(result->inserted() == 0);
    EXPECT(result->error().code == SQL::SQLErrorCode::InvalidValueType);
}

TEST_CASE(insert_into_table_multiple_tuples_wrong_data_types)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ('Test_1', 42), (43, 'Test_2');");
    EXPECT(result->inserted() == 0);
    EXPECT(result->error().code == SQL::SQLErrorCode::InvalidValueType);
}

TEST_CASE(insert_wrong_number_of_values)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES ( 42 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::InvalidNumberOfValues);
    EXPECT(result->inserted() == 0);
}

TEST_CASE(insert_identifier_as_value)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES ( identifier, 42 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::SyntaxError);
    EXPECT(result->inserted() == 0);
}

TEST_CASE(insert_quoted_identifier_as_value)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES ( \"QuotedIdentifier\", 42 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::SyntaxError);
    EXPECT(result->inserted() == 0);
}

TEST_CASE(insert_without_column_names)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES ('Test_1', 42), ('Test_2', 43);");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 2);

    auto table_or_error = database->get_table("TESTSCHEMA", "TESTTABLE");
    EXPECT(!table_or_error.is_error());
    auto rows_or_error = database->select_all(*(table_or_error.value()));
    EXPECT(!rows_or_error.is_error());
    EXPECT_EQ(rows_or_error.value().size(), 2u);
}

TEST_CASE(select_from_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database, "SELECT * FROM TestSchema.TestTable;");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->has_results());
    EXPECT_EQ(result->results().size(), 5u);
}

TEST_CASE(select_with_column_names)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable;");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->has_results());
    EXPECT_EQ(result->results().size(), 5u);
    EXPECT_EQ(result->results()[0].size(), 1u);
}

TEST_CASE(select_with_nonexisting_column_name)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database, "SELECT Bogus FROM TestSchema.TestTable;");
    EXPECT(result->error().code == SQL::SQLErrorCode::ColumnDoesNotExist);
}

TEST_CASE(select_with_where)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable WHERE IntColumn > 44;");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->has_results());
    EXPECT_EQ(result->results().size(), 2u);
    for (auto& row : result->results()) {
        EXPECT(row[1].to_int().value() > 44);
    }
}

TEST_CASE(select_cross_join)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_two_tables(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable1 ( TextColumn1, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database,
        "INSERT INTO TestSchema.TestTable2 ( TextColumn2, IntColumn ) VALUES "
        "( 'Test_10', 40 ), "
        "( 'Test_11', 41 ), "
        "( 'Test_12', 42 ), "
        "( 'Test_13', 47 ), "
        "( 'Test_14', 48 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database, "SELECT * FROM TestSchema.TestTable1, TestSchema.TestTable2;");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->has_results());
    EXPECT_EQ(result->results().size(), 25u);
    for (auto& row : result->results()) {
        EXPECT(row.size() == 4);
        EXPECT(row[1].to_int().value() >= 42);
        EXPECT(row[1].to_int().value() <= 46);
        EXPECT(row[3].to_int().value() >= 40);
        EXPECT(row[3].to_int().value() <= 48);
    }
}

TEST_CASE(select_inner_join)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = SQL::Database::construct(db_name);
    EXPECT(!database->open().is_error());
    create_two_tables(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable1 ( TextColumn1, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database,
        "INSERT INTO TestSchema.TestTable2 ( TextColumn2, IntColumn ) VALUES "
        "( 'Test_10', 40 ), "
        "( 'Test_11', 41 ), "
        "( 'Test_12', 42 ), "
        "( 'Test_13', 47 ), "
        "( 'Test_14', 48 );");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->inserted() == 5);
    result = execute(database,
        "SELECT TestTable1.IntColumn, TextColumn1, TextColumn2 "
        "FROM TestSchema.TestTable1, TestSchema.TestTable2 "
        "WHERE TestTable1.IntColumn = TestTable2.IntColumn;");
    EXPECT(result->error().code == SQL::SQLErrorCode::NoError);
    EXPECT(result->has_results());
    EXPECT_EQ(result->results().size(), 1u);
    auto& row = result->results()[0];
    EXPECT_EQ(row.size(), 3u);
    EXPECT_EQ(row[0].to_int().value(), 42);
    EXPECT_EQ(row[1].to_string(), "Test_1");
    EXPECT_EQ(row[2].to_string(), "Test_12");
}

}
