/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/TestSuite.h>

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
    EXPECT(parse("CREATE TABLE test ( column1 varchar()").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(abc)").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123 )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123,  )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(123, ) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(.) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(.abc) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(0x) )").is_error());
    EXPECT(parse("CREATE TABLE test ( column1 varchar(0xzzz) )").is_error());

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

    validate("CREATE TABLE test ( column1 int );", {}, "test", { { "column1", "int" } });
    validate("CREATE TABLE test ( column1 varchar );", {}, "test", { { "column1", "varchar" } });
    validate("CREATE TABLE test ( column1 varchar(255) );", {}, "test", { { "column1", "varchar", { 255 } } });
    validate("CREATE TABLE test ( column1 varchar(255, 123) );", {}, "test", { { "column1", "varchar", { 255, 123 } } });
    validate("CREATE TABLE test ( column1 varchar(255, -123) );", {}, "test", { { "column1", "varchar", { 255, -123 } } });
    validate("CREATE TABLE test ( column1 varchar(0xff) );", {}, "test", { { "column1", "varchar", { 255 } } });
    validate("CREATE TABLE test ( column1 varchar(3.14) );", {}, "test", { { "column1", "varchar", { 3.14 } } });
    validate("CREATE TABLE test ( column1 varchar(1e3) );", {}, "test", { { "column1", "varchar", { 1000 } } });
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

TEST_CASE(delete_)
{
    EXPECT(parse("DELETE").is_error());
    EXPECT(parse("DELETE FROM").is_error());
    EXPECT(parse("DELETE FROM table").is_error());
    EXPECT(parse("DELETE FROM table WHERE").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15 RETURNING").is_error());
    EXPECT(parse("DELETE FROM table WHERE 15 RETURNING *").is_error());
    EXPECT(parse("DELETE FROM table WHERE (');").is_error());
    EXPECT(parse("WITH DELETE FROM table;").is_error());
    EXPECT(parse("WITH table DELETE FROM table;").is_error());
    EXPECT(parse("WITH table AS DELETE FROM table;").is_error());
    EXPECT(parse("WITH RECURSIVE table DELETE FROM table;").is_error());
    EXPECT(parse("WITH RECURSIVE table AS DELETE FROM table;").is_error());

    struct SelectedTable {
        bool recursive { false };
        StringView table_name {};
        Vector<StringView> column_names {};
    };

    auto validate = [](StringView sql, SelectedTable expected_selected_table, StringView expected_schema, StringView expected_table, StringView expected_alias, bool expect_where_clause, bool expect_returning_clause, Vector<StringView> expected_returned_column_aliases) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto statement = result.release_value();
        EXPECT(is<SQL::Delete>(*statement));

        const auto& delete_ = static_cast<const SQL::Delete&>(*statement);
        EXPECT_EQ(delete_.recursive(), expected_selected_table.recursive);

        const auto& common_table_expression = delete_.common_table_expression();
        EXPECT_EQ(common_table_expression.is_null(), expected_selected_table.table_name.is_empty());
        if (common_table_expression) {
            EXPECT_EQ(common_table_expression->table_name(), expected_selected_table.table_name);
            EXPECT_EQ(common_table_expression->column_names().size(), expected_selected_table.column_names.size());
            for (size_t i = 0; i < common_table_expression->column_names().size(); ++i)
                EXPECT_EQ(common_table_expression->column_names()[i], expected_selected_table.column_names[i]);
        }

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

    validate("DELETE FROM table;", {}, {}, "table", {}, false, false, {});
    validate("DELETE FROM schema.table;", {}, "schema", "table", {}, false, false, {});
    validate("DELETE FROM schema.table AS alias;", {}, "schema", "table", "alias", false, false, {});
    validate("DELETE FROM table WHERE (1 == 1);", {}, {}, "table", {}, true, false, {});
    validate("DELETE FROM table RETURNING *;", {}, {}, "table", {}, false, true, {});
    validate("DELETE FROM table RETURNING column;", {}, {}, "table", {}, false, true, { {} });
    validate("DELETE FROM table RETURNING column AS alias;", {}, {}, "table", {}, false, true, { "alias" });
    validate("DELETE FROM table RETURNING column1 AS alias1, column2 AS alias2;", {}, {}, "table", {}, false, true, { "alias1", "alias2" });

    // FIXME: When parsing of SELECT statements are supported, the common-table-expressions below will become invalid due to the empty "AS ()" clause.
    validate("WITH table AS () DELETE FROM table;", { false, "table", {} }, {}, "table", {}, false, false, {});
    validate("WITH table (column) AS () DELETE FROM table;", { false, "table", { "column" } }, {}, "table", {}, false, false, {});
    validate("WITH table (column1, column2) AS () DELETE FROM table;", { false, "table", { "column1", "column2" } }, {}, "table", {}, false, false, {});
    validate("WITH RECURSIVE table AS () DELETE FROM table;", { true, "table", {} }, {}, "table", {}, false, false, {});
}

TEST_MAIN(SqlStatementParser)
