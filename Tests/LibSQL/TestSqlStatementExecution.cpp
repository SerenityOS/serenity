/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <unistd.h>

#include <AK/QuickSort.h>
#include <AK/ScopeGuard.h>
#include <LibSQL/AST/Parser.h>
#include <LibSQL/Database.h>
#include <LibSQL/Result.h>
#include <LibSQL/ResultSet.h>
#include <LibSQL/Row.h>
#include <LibSQL/Value.h>
#include <LibTest/TestCase.h>

namespace {

constexpr char const* db_name = "/tmp/test.db";

SQL::ResultOr<SQL::ResultSet> try_execute(NonnullRefPtr<SQL::Database> database, ByteString const& sql, Vector<SQL::Value> placeholder_values = {})
{
    auto parser = SQL::AST::Parser(SQL::AST::Lexer(sql));
    auto statement = parser.next_statement();
    EXPECT(!parser.has_errors());
    if (parser.has_errors())
        outln("{}", parser.errors()[0].to_byte_string());
    return statement->execute(move(database), placeholder_values);
}

SQL::ResultSet execute(NonnullRefPtr<SQL::Database> database, ByteString const& sql, Vector<SQL::Value> placeholder_values = {})
{
    auto result = try_execute(move(database), sql, move(placeholder_values));
    if (result.is_error()) {
        outln("{}", result.release_error().error_string());
        VERIFY_NOT_REACHED();
    }
    return result.release_value();
}

template<typename... Args>
Vector<SQL::Value> placeholders(Args&&... args)
{
    return { SQL::Value(forward<Args>(args))... };
}

void create_schema(NonnullRefPtr<SQL::Database> database)
{
    auto result = execute(database, "CREATE SCHEMA TestSchema;");
    EXPECT_EQ(result.command(), SQL::SQLCommand::Create);
}

void create_table(NonnullRefPtr<SQL::Database> database)
{
    create_schema(database);
    auto result = execute(database, "CREATE TABLE TestSchema.TestTable ( TextColumn text, IntColumn integer );");
    EXPECT_EQ(result.command(), SQL::SQLCommand::Create);
}

void create_two_tables(NonnullRefPtr<SQL::Database> database)
{
    create_schema(database);
    auto result = execute(database, "CREATE TABLE TestSchema.TestTable1 ( TextColumn1 text, IntColumn integer );");
    EXPECT_EQ(result.command(), SQL::SQLCommand::Create);
    result = execute(database, "CREATE TABLE TestSchema.TestTable2 ( TextColumn2 text, IntColumn integer );");
    EXPECT_EQ(result.command(), SQL::SQLCommand::Create);
}

TEST_CASE(create_schema)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_schema(database);
    auto schema = MUST(database->get_schema("TESTSCHEMA"));
}

TEST_CASE(create_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto table = MUST(database->get_table("TESTSCHEMA", "TESTTABLE"));
}

TEST_CASE(insert_into_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test', 42 );");
    EXPECT(result.size() == 1);

    auto table = MUST(database->get_table("TESTSCHEMA", "TESTTABLE"));

    int count = 0;
    auto rows = TRY_OR_FAIL(database->select_all(*table));
    for (auto& row : rows) {
        EXPECT_EQ(row["TEXTCOLUMN"].to_byte_string(), "Test");
        EXPECT_EQ(row["INTCOLUMN"].to_int<i32>(), 42);
        count++;
    }
    EXPECT_EQ(count, 1);
}

TEST_CASE(insert_into_table_wrong_data_types)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = try_execute(database, "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES (43, 'Test_2');");
    EXPECT(result.is_error());
    EXPECT(result.release_error().error() == SQL::SQLErrorCode::InvalidValueType);
}

TEST_CASE(insert_into_table_multiple_tuples_wrong_data_types)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = try_execute(database, "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ('Test_1', 42), (43, 'Test_2');");
    EXPECT(result.is_error());
    EXPECT(result.release_error().error() == SQL::SQLErrorCode::InvalidValueType);
}

TEST_CASE(insert_wrong_number_of_values)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES ( 42 );");
    EXPECT(result.is_error());
    EXPECT(result.release_error().error() == SQL::SQLErrorCode::InvalidNumberOfValues);
}

TEST_CASE(insert_identifier_as_value)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES ( identifier, 42 );");
    EXPECT(result.is_error());
    EXPECT(result.release_error().error() == SQL::SQLErrorCode::SyntaxError);
}

TEST_CASE(insert_quoted_identifier_as_value)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES ( \"QuotedIdentifier\", 42 );");
    EXPECT(result.is_error());
    EXPECT(result.release_error().error() == SQL::SQLErrorCode::SyntaxError);
}

TEST_CASE(insert_without_column_names)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES ('Test_1', 42), ('Test_2', 43);");
    EXPECT(result.size() == 2);

    auto table = MUST(database->get_table("TESTSCHEMA", "TESTTABLE"));
    auto rows = TRY_OR_FAIL(database->select_all(*table));
    EXPECT_EQ(rows.size(), 2u);
}

TEST_CASE(insert_with_placeholders)
{
    ScopeGuard guard([]() { unlink(db_name); });

    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);

    {
        auto result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES (?, ?);");
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::InvalidNumberOfPlaceholderValues);

        result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES (?, ?);", placeholders("Test_1"sv));
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::InvalidNumberOfPlaceholderValues);

        result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES (?, ?);", placeholders(42, 42));
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::InvalidValueType);

        result = try_execute(database, "INSERT INTO TestSchema.TestTable VALUES (?, ?);", placeholders("Test_1"sv, "Test_2"sv));
        EXPECT(result.is_error());
        EXPECT_EQ(result.error().error(), SQL::SQLErrorCode::InvalidValueType);
    }
    {
        auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES (?, ?);", placeholders("Test_1"sv, 42));
        EXPECT_EQ(result.size(), 1u);

        result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable ORDER BY TextColumn;");
        EXPECT_EQ(result.size(), 1u);

        EXPECT_EQ(result[0].row[0], "Test_1"sv);
        EXPECT_EQ(result[0].row[1], 42);
    }
    {
        auto result = execute(database, "INSERT INTO TestSchema.TestTable VALUES (?, ?), (?, ?);", placeholders("Test_2"sv, 43, "Test_3"sv, 44));
        EXPECT_EQ(result.size(), 2u);

        result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable ORDER BY TextColumn;");
        EXPECT_EQ(result.size(), 3u);

        EXPECT_EQ(result[0].row[0], "Test_1"sv);
        EXPECT_EQ(result[0].row[1], 42);

        EXPECT_EQ(result[1].row[0], "Test_2"sv);
        EXPECT_EQ(result[1].row[1], 43);

        EXPECT_EQ(result[2].row[0], "Test_3"sv);
        EXPECT_EQ(result[2].row[1], 44);
    }
}

TEST_CASE(select_from_empty_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database, "SELECT * FROM TestSchema.TestTable;");
    EXPECT(result.is_empty());
}

TEST_CASE(select_from_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result.size() == 5);
    result = execute(database, "SELECT * FROM TestSchema.TestTable;");
    EXPECT_EQ(result.size(), 5u);
}

TEST_CASE(select_with_column_names)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result.size() == 5);
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable;");
    EXPECT_EQ(result.size(), 5u);
    EXPECT_EQ(result[0].row.size(), 1u);
}

TEST_CASE(select_with_nonexisting_column_name)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result.size() == 5);

    auto insert_result = try_execute(database, "SELECT Bogus FROM TestSchema.TestTable;");
    EXPECT(insert_result.is_error());
    EXPECT(insert_result.release_error().error() == SQL::SQLErrorCode::ColumnDoesNotExist);
}

TEST_CASE(select_with_where)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result.size() == 5);
    result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable WHERE IntColumn > 44;");
    EXPECT_EQ(result.size(), 2u);
    for (auto& row : result) {
        EXPECT(row.row[1].to_int<i32>().value() > 44);
    }
}

TEST_CASE(select_cross_join)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_two_tables(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable1 ( TextColumn1, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result.size() == 5);
    result = execute(database,
        "INSERT INTO TestSchema.TestTable2 ( TextColumn2, IntColumn ) VALUES "
        "( 'Test_10', 40 ), "
        "( 'Test_11', 41 ), "
        "( 'Test_12', 42 ), "
        "( 'Test_13', 47 ), "
        "( 'Test_14', 48 );");
    EXPECT(result.size() == 5);
    result = execute(database, "SELECT * FROM TestSchema.TestTable1, TestSchema.TestTable2;");
    EXPECT_EQ(result.size(), 25u);
    for (auto& row : result) {
        EXPECT(row.row.size() == 4);
        EXPECT(row.row[1].to_int<i32>().value() >= 42);
        EXPECT(row.row[1].to_int<i32>().value() <= 46);
        EXPECT(row.row[3].to_int<i32>().value() >= 40);
        EXPECT(row.row[3].to_int<i32>().value() <= 48);
    }
}

TEST_CASE(select_inner_join)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_two_tables(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable1 ( TextColumn1, IntColumn ) VALUES "
        "( 'Test_1', 42 ), "
        "( 'Test_2', 43 ), "
        "( 'Test_3', 44 ), "
        "( 'Test_4', 45 ), "
        "( 'Test_5', 46 );");
    EXPECT(result.size() == 5);
    result = execute(database,
        "INSERT INTO TestSchema.TestTable2 ( TextColumn2, IntColumn ) VALUES "
        "( 'Test_10', 40 ), "
        "( 'Test_11', 41 ), "
        "( 'Test_12', 42 ), "
        "( 'Test_13', 47 ), "
        "( 'Test_14', 48 );");
    EXPECT(result.size() == 5);
    result = execute(database,
        "SELECT TestTable1.IntColumn, TextColumn1, TextColumn2 "
        "FROM TestSchema.TestTable1, TestSchema.TestTable2 "
        "WHERE TestTable1.IntColumn = TestTable2.IntColumn;");
    EXPECT_EQ(result.size(), 1u);
    EXPECT_EQ(result[0].row.size(), 3u);
    EXPECT_EQ(result[0].row[0].to_int<i32>(), 42);
    EXPECT_EQ(result[0].row[1].to_byte_string(), "Test_1");
    EXPECT_EQ(result[0].row[2].to_byte_string(), "Test_12");
}

TEST_CASE(select_with_like)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test+1', 42 ), "
        "( 'Test+2', 43 ), "
        "( 'Test+3', 44 ), "
        "( 'Test+4', 45 ), "
        "( 'Test+5', 46 ), "
        "( 'Another+Test_6', 47 );");
    EXPECT(result.size() == 6);

    // Simple match
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE 'Test+1';");
    EXPECT_EQ(result.size(), 1u);

    // Use % to match most rows
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE 'T%';");
    EXPECT_EQ(result.size(), 5u);

    // Same as above but invert the match
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn NOT LIKE 'T%';");
    EXPECT_EQ(result.size(), 1u);

    // Use _ and % to match all rows
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE '%e_t%';");
    EXPECT_EQ(result.size(), 6u);

    // Use escape to match a single row. The escape character happens to be a
    // Regex metacharacter, let's make sure we don't get confused by that.
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE '%Test^_%' ESCAPE '^';");
    EXPECT_EQ(result.size(), 1u);

    // Same as above but escape the escape character happens to be a SQL
    // metacharacter - we want to make sure it's treated as an escape.
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE '%Test__%' ESCAPE '_';");
    EXPECT_EQ(result.size(), 1u);

    // (Unnecessarily) escaping a character that happens to be a Regex
    // metacharacter should have no effect.
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE 'Test:+_' ESCAPE ':';");
    EXPECT_EQ(result.size(), 5u);

    // Make sure we error out if the ESCAPE is empty
    auto select_result = try_execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE '%' ESCAPE '';");
    EXPECT(select_result.is_error());
    EXPECT(select_result.release_error().error() == SQL::SQLErrorCode::SyntaxError);

    // Make sure we error out if the ESCAPE has more than a single character
    select_result = try_execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn LIKE '%' ESCAPE 'whf';");
    EXPECT(select_result.is_error());
    EXPECT(select_result.release_error().error() == SQL::SQLErrorCode::SyntaxError);
}

TEST_CASE(select_with_order)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_5', 44 ), "
        "( 'Test_2', 42 ), "
        "( 'Test_1', 47 ), "
        "( 'Test_3', 40 ), "
        "( 'Test_4', 41 );");
    EXPECT(result.size() == 5);

    result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
    EXPECT_EQ(result.size(), 5u);
    EXPECT_EQ(result[0].row[1].to_int<i32>(), 40);
    EXPECT_EQ(result[1].row[1].to_int<i32>(), 41);
    EXPECT_EQ(result[2].row[1].to_int<i32>(), 42);
    EXPECT_EQ(result[3].row[1].to_int<i32>(), 44);
    EXPECT_EQ(result[4].row[1].to_int<i32>(), 47);

    result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable ORDER BY TextColumn;");
    EXPECT_EQ(result.size(), 5u);
    EXPECT_EQ(result[0].row[0].to_byte_string(), "Test_1");
    EXPECT_EQ(result[1].row[0].to_byte_string(), "Test_2");
    EXPECT_EQ(result[2].row[0].to_byte_string(), "Test_3");
    EXPECT_EQ(result[3].row[0].to_byte_string(), "Test_4");
    EXPECT_EQ(result[4].row[0].to_byte_string(), "Test_5");
}

TEST_CASE(select_with_regexp)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test+1', 42 ), "
        "( 'Pröv+2', 43 ), "
        "( 'Test(3)', 44 ), "
        "( 'Test[4]', 45 ), "
        "( 'Test+5', 46 ), "
        "( 'Another-Test_6', 47 );");
    EXPECT(result.size() == 6);

    // Simple match
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn REGEXP 'Test\\+1';");
    EXPECT_EQ(result.size(), 1u);

    // Match all
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn REGEXP '.*';");
    EXPECT_EQ(result.size(), 6u);

    // Match with wildcards
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn REGEXP '^Test.+';");
    EXPECT_EQ(result.size(), 4u);

    // Match with case insensitive basic Latin and case sensitive Swedish ö
    // FIXME: If LibRegex is changed to support case insensitive matches of Unicode characters
    //        This test should be updated and changed to match 'PRÖV'.
    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn REGEXP 'PRöV.*';");
    EXPECT_EQ(result.size(), 1u);
}

TEST_CASE(handle_regexp_errors)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test', 0 );");
    EXPECT(result.size() == 1);

    // Malformed regex, unmatched square bracket
    auto select_result = try_execute(database, "SELECT TextColumn FROM TestSchema.TestTable WHERE TextColumn REGEXP 'Test\\+[0-9.*';");
    EXPECT(select_result.is_error());
}

TEST_CASE(select_with_order_two_columns)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_5', 44 ), "
        "( 'Test_2', 42 ), "
        "( 'Test_1', 47 ), "
        "( 'Test_2', 40 ), "
        "( 'Test_4', 41 );");
    EXPECT(result.size() == 5);

    result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable ORDER BY TextColumn, IntColumn;");
    EXPECT_EQ(result.size(), 5u);
    EXPECT_EQ(result[0].row[0].to_byte_string(), "Test_1");
    EXPECT_EQ(result[0].row[1].to_int<i32>(), 47);
    EXPECT_EQ(result[1].row[0].to_byte_string(), "Test_2");
    EXPECT_EQ(result[1].row[1].to_int<i32>(), 40);
    EXPECT_EQ(result[2].row[0].to_byte_string(), "Test_2");
    EXPECT_EQ(result[2].row[1].to_int<i32>(), 42);
    EXPECT_EQ(result[3].row[0].to_byte_string(), "Test_4");
    EXPECT_EQ(result[3].row[1].to_int<i32>(), 41);
    EXPECT_EQ(result[4].row[0].to_byte_string(), "Test_5");
    EXPECT_EQ(result[4].row[1].to_int<i32>(), 44);
}

TEST_CASE(select_with_order_by_column_not_in_result)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database,
        "INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES "
        "( 'Test_5', 44 ), "
        "( 'Test_2', 42 ), "
        "( 'Test_1', 47 ), "
        "( 'Test_3', 40 ), "
        "( 'Test_4', 41 );");
    EXPECT(result.size() == 5);

    result = execute(database, "SELECT TextColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
    EXPECT_EQ(result.size(), 5u);
    EXPECT_EQ(result[0].row[0].to_byte_string(), "Test_3");
    EXPECT_EQ(result[1].row[0].to_byte_string(), "Test_4");
    EXPECT_EQ(result[2].row[0].to_byte_string(), "Test_2");
    EXPECT_EQ(result[3].row[0].to_byte_string(), "Test_5");
    EXPECT_EQ(result[4].row[0].to_byte_string(), "Test_1");
}

TEST_CASE(select_with_limit)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    for (auto count = 0; count < 100; count++) {
        auto result = execute(database,
            ByteString::formatted("INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test_{}', {} );", count, count));
        EXPECT(result.size() == 1);
    }
    auto result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable LIMIT 10;");
    auto rows = result;
    EXPECT_EQ(rows.size(), 10u);
}

TEST_CASE(select_with_limit_and_offset)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    for (auto count = 0; count < 100; count++) {
        auto result = execute(database,
            ByteString::formatted("INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test_{}', {} );", count, count));
        EXPECT(result.size() == 1);
    }
    auto result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable LIMIT 10 OFFSET 10;");
    EXPECT_EQ(result.size(), 10u);
}

TEST_CASE(select_with_order_limit_and_offset)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    for (auto count = 0; count < 100; count++) {
        auto result = execute(database,
            ByteString::formatted("INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test_{}', {} );", count, count));
        EXPECT(result.size() == 1);
    }
    auto result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable ORDER BY IntColumn LIMIT 10 OFFSET 10;");
    EXPECT_EQ(result.size(), 10u);
    EXPECT_EQ(result[0].row[1].to_int<i32>(), 10);
    EXPECT_EQ(result[1].row[1].to_int<i32>(), 11);
    EXPECT_EQ(result[2].row[1].to_int<i32>(), 12);
    EXPECT_EQ(result[3].row[1].to_int<i32>(), 13);
    EXPECT_EQ(result[4].row[1].to_int<i32>(), 14);
    EXPECT_EQ(result[5].row[1].to_int<i32>(), 15);
    EXPECT_EQ(result[6].row[1].to_int<i32>(), 16);
    EXPECT_EQ(result[7].row[1].to_int<i32>(), 17);
    EXPECT_EQ(result[8].row[1].to_int<i32>(), 18);
    EXPECT_EQ(result[9].row[1].to_int<i32>(), 19);
}

TEST_CASE(select_with_limit_out_of_bounds)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    for (auto count = 0; count < 100; count++) {
        auto result = execute(database,
            ByteString::formatted("INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test_{}', {} );", count, count));
        EXPECT(result.size() == 1);
    }
    auto result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable LIMIT 500;");
    EXPECT_EQ(result.size(), 100u);
}

TEST_CASE(select_with_offset_out_of_bounds)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    for (auto count = 0; count < 100; count++) {
        auto result = execute(database,
            ByteString::formatted("INSERT INTO TestSchema.TestTable ( TextColumn, IntColumn ) VALUES ( 'Test_{}', {} );", count, count));
        EXPECT(result.size() == 1);
    }
    auto result = execute(database, "SELECT TextColumn, IntColumn FROM TestSchema.TestTable LIMIT 10 OFFSET 200;");
    EXPECT_EQ(result.size(), 0u);
}

TEST_CASE(describe_table)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);
    auto result = execute(database, "DESCRIBE TABLE TestSchema.TestTable;");
    EXPECT_EQ(result.size(), 2u);

    EXPECT_EQ(result[0].row[0].to_byte_string(), "TEXTCOLUMN");
    EXPECT_EQ(result[0].row[1].to_byte_string(), "text");

    EXPECT_EQ(result[1].row[0].to_byte_string(), "INTCOLUMN");
    EXPECT_EQ(result[1].row[1].to_byte_string(), "int");
}

TEST_CASE(binary_operator_execution)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);

    for (auto count = 0; count < 10; ++count) {
        auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
        EXPECT_EQ(result.size(), 1u);
    }

    auto compare_result = [](SQL::ResultSet const& result, Vector<int> const& expected) {
        EXPECT_EQ(result.command(), SQL::SQLCommand::Select);
        EXPECT_EQ(result.size(), expected.size());

        Vector<int> result_values;
        result_values.ensure_capacity(result.size());

        for (size_t i = 0; i < result.size(); ++i) {
            auto const& result_row = result.at(i).row;
            EXPECT_EQ(result_row.size(), 1u);

            auto result_column = result_row[0].to_int<i32>();
            result_values.append(result_column.value());
        }

        quick_sort(result_values);
        EXPECT_EQ(result_values, expected);
    };

    auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn + 1) < 5);");
    compare_result(result, { 0, 1, 2, 3 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn + 1) <= 5);");
    compare_result(result, { 0, 1, 2, 3, 4 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn - 1) > 4);");
    compare_result(result, { 6, 7, 8, 9 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn - 1) >= 4);");
    compare_result(result, { 5, 6, 7, 8, 9 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn * 2) < 10);");
    compare_result(result, { 0, 1, 2, 3, 4 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn * 2) <= 10);");
    compare_result(result, { 0, 1, 2, 3, 4, 5 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn / 3) > 2);");
    compare_result(result, { 7, 8, 9 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn / 3) >= 2);");
    compare_result(result, { 6, 7, 8, 9 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn % 2) = 0);");
    compare_result(result, { 0, 2, 4, 6, 8 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn % 2) = 1);");
    compare_result(result, { 1, 3, 5, 7, 9 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((1 << IntColumn) <= 32);");
    compare_result(result, { 0, 1, 2, 3, 4, 5 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((1024 >> IntColumn) >= 32);");
    compare_result(result, { 0, 1, 2, 3, 4, 5 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn | 1) != IntColumn);");
    compare_result(result, { 0, 2, 4, 6, 8 });

    result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable WHERE ((IntColumn & 1) = 1);");
    compare_result(result, { 1, 3, 5, 7, 9 });
}

TEST_CASE(binary_operator_failure)
{
    ScopeGuard guard([]() { unlink(db_name); });
    auto database = MUST(SQL::Database::create(db_name));
    MUST(database->open());
    create_table(database);

    for (auto count = 0; count < 10; ++count) {
        auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
        EXPECT_EQ(result.size(), 1u);
    }

    auto expect_failure = [](auto result, auto op) {
        EXPECT(result.is_error());

        auto error = result.release_error();
        EXPECT_EQ(error.error(), SQL::SQLErrorCode::NumericOperatorTypeMismatch);

        auto message = ByteString::formatted("NumericOperatorTypeMismatch: Cannot apply '{}' operator to non-numeric operands", op);
        EXPECT_EQ(error.error_string(), message);
    };

    auto result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn + TextColumn) < 5);");
    expect_failure(move(result), '+');

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn - TextColumn) < 5);");
    expect_failure(move(result), '-');

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn * TextColumn) < 5);");
    expect_failure(move(result), '*');

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn / TextColumn) < 5);");
    expect_failure(move(result), '/');

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn % TextColumn) < 5);");
    expect_failure(move(result), '%');

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn << TextColumn) < 5);");
    expect_failure(move(result), "<<"sv);

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn >> TextColumn) < 5);");
    expect_failure(move(result), ">>"sv);

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn | TextColumn) < 5);");
    expect_failure(move(result), '|');

    result = try_execute(database, "SELECT * FROM TestSchema.TestTable WHERE ((IntColumn & TextColumn) < 5);");
    expect_failure(move(result), '&');
}

TEST_CASE(describe_large_table_after_persist)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "CREATE TABLE Cookies ( name TEXT, value TEXT, same_site INTEGER, creation_time INTEGER, last_access_time INTEGER, expiry_time INTEGER, domain TEXT, path TEXT, secure INTEGER, http_only INTEGER, host_only INTEGER, persistent INTEGER );");
        EXPECT_EQ(result.command(), SQL::SQLCommand::Create);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "DESCRIBE TABLE Cookies;");
        EXPECT_EQ(result.size(), 12u);
    }
}

TEST_CASE(delete_single_row)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        create_table(database);
        for (auto count = 0; count < 10; ++count) {
            auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
            EXPECT_EQ(result.size(), 1u);
        }

        auto result = execute(database, "SELECT * FROM TestSchema.TestTable;");
        EXPECT_EQ(result.size(), 10u);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        execute(database, "DELETE FROM TestSchema.TestTable WHERE (IntColumn = 4);");

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 9u);

        for (auto i = 0u; i < 4; ++i)
            EXPECT_EQ(result[i].row[0], i);
        for (auto i = 5u; i < 9; ++i)
            EXPECT_EQ(result[i].row[0], i + 1);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 9u);

        for (auto i = 0u; i < 4; ++i)
            EXPECT_EQ(result[i].row[0], i);
        for (auto i = 5u; i < 9; ++i)
            EXPECT_EQ(result[i].row[0], i + 1);
    }
}

TEST_CASE(delete_multiple_rows)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        create_table(database);
        for (auto count = 0; count < 10; ++count) {
            auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
            EXPECT_EQ(result.size(), 1u);
        }

        auto result = execute(database, "SELECT * FROM TestSchema.TestTable;");
        EXPECT_EQ(result.size(), 10u);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        execute(database, "DELETE FROM TestSchema.TestTable WHERE (IntColumn >= 4);");

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 4u);

        for (auto i = 0u; i < result.size(); ++i)
            EXPECT_EQ(result[i].row[0], i);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 4u);

        for (auto i = 0u; i < result.size(); ++i)
            EXPECT_EQ(result[i].row[0], i);
    }
}

TEST_CASE(delete_all_rows)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        create_table(database);
        for (auto count = 0; count < 10; ++count) {
            auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
            EXPECT_EQ(result.size(), 1u);
        }

        auto result = execute(database, "SELECT * FROM TestSchema.TestTable;");
        EXPECT_EQ(result.size(), 10u);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        execute(database, "DELETE FROM TestSchema.TestTable;");

        auto result = execute(database, "SELECT * FROM TestSchema.TestTable;");
        EXPECT(result.is_empty());
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "SELECT * FROM TestSchema.TestTable;");
        EXPECT(result.is_empty());
    }
}

TEST_CASE(update_single_row)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        create_table(database);
        for (auto count = 0; count < 10; ++count) {
            auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
            EXPECT_EQ(result.size(), 1u);
        }

        execute(database, "UPDATE TestSchema.TestTable SET IntColumn=123456 WHERE (TextColumn = 'T3');");

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 10u);

        for (auto i = 0u; i < 10; ++i) {
            if (i < 3)
                EXPECT_EQ(result[i].row[0], i);
            else if (i < 9)
                EXPECT_EQ(result[i].row[0], i + 1);
            else
                EXPECT_EQ(result[i].row[0], 123456);
        }
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 10u);

        for (auto i = 0u; i < 10; ++i) {
            if (i < 3)
                EXPECT_EQ(result[i].row[0], i);
            else if (i < 9)
                EXPECT_EQ(result[i].row[0], i + 1);
            else
                EXPECT_EQ(result[i].row[0], 123456);
        }
    }
}

TEST_CASE(update_multiple_rows)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        create_table(database);
        for (auto count = 0; count < 10; ++count) {
            auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
            EXPECT_EQ(result.size(), 1u);
        }

        execute(database, "UPDATE TestSchema.TestTable SET IntColumn=123456 WHERE (IntColumn > 4);");

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 10u);

        for (auto i = 0u; i < 10; ++i) {
            if (i < 5)
                EXPECT_EQ(result[i].row[0], i);
            else
                EXPECT_EQ(result[i].row[0], 123456);
        }
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 10u);

        for (auto i = 0u; i < 10; ++i) {
            if (i < 5)
                EXPECT_EQ(result[i].row[0], i);
            else
                EXPECT_EQ(result[i].row[0], 123456);
        }
    }
}

TEST_CASE(update_all_rows)
{
    ScopeGuard guard([]() { unlink(db_name); });
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        create_table(database);
        for (auto count = 0; count < 10; ++count) {
            auto result = execute(database, ByteString::formatted("INSERT INTO TestSchema.TestTable VALUES ( 'T{}', {} );", count, count));
            EXPECT_EQ(result.size(), 1u);
        }

        execute(database, "UPDATE TestSchema.TestTable SET IntColumn=123456;");

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 10u);

        for (auto i = 0u; i < 10; ++i)
            EXPECT_EQ(result[i].row[0], 123456);
    }
    {
        auto database = MUST(SQL::Database::create(db_name));
        MUST(database->open());

        auto result = execute(database, "SELECT IntColumn FROM TestSchema.TestTable ORDER BY IntColumn;");
        EXPECT_EQ(result.size(), 10u);

        for (auto i = 0u; i < 10; ++i)
            EXPECT_EQ(result[i].row[0], 123456);
    }
}

}
