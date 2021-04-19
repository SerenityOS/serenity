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

TEST_MAIN(SqlParser)
