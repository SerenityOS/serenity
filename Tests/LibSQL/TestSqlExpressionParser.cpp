/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <AK/Result.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/TypeCasts.h>
#include <LibSQL/AST/Lexer.h>
#include <LibSQL/AST/Parser.h>

namespace {

class ExpressionParser : public SQL::AST::Parser {
public:
    explicit ExpressionParser(SQL::AST::Lexer lexer)
        : SQL::AST::Parser(move(lexer))
    {
    }

    NonnullRefPtr<SQL::AST::Expression> parse()
    {
        return SQL::AST::Parser::parse_expression();
    }
};

using ParseResult = AK::Result<NonnullRefPtr<SQL::AST::Expression>, ByteString>;

ParseResult parse(StringView sql)
{
    auto parser = ExpressionParser(SQL::AST::Lexer(sql));
    auto expression = parser.parse();

    if (parser.has_errors()) {
        return parser.errors()[0].to_byte_string();
    }

    return expression;
}

}

TEST_CASE(numeric_literal)
{
    // FIXME Right now the "1a" test fails (meaning the parse succeeds).
    //       This is obviously inconsistent.
    //       See the FIXME in lexer.cpp, method consume_exponent() about
    //       solutions.
    // EXPECT(parse("1e"sv).is_error());
    // EXPECT(parse("1a"sv).is_error());
    // EXPECT(parse("0x"sv).is_error());

    auto validate = [](StringView sql, double expected_value) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::NumericLiteral>(*expression));

        auto const& literal = static_cast<const SQL::AST::NumericLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("123"sv, 123);
    validate("3.14"sv, 3.14);
    validate("0xA"sv, 10);
    validate("0xff"sv, 255);
    validate("0x100"sv, 256);
    validate("1e3"sv, 1000);
}

TEST_CASE(string_literal)
{
    EXPECT(parse("'"sv).is_error());
    EXPECT(parse("'unterminated"sv).is_error());

    auto validate = [](StringView sql, StringView expected_value) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::StringLiteral>(*expression));

        auto const& literal = static_cast<const SQL::AST::StringLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("''"sv, ""sv);
    validate("'hello friends'"sv, "hello friends"sv);
    validate("'hello ''friends'''"sv, "hello 'friends'"sv);
}

TEST_CASE(blob_literal)
{
    EXPECT(parse("x'"sv).is_error());
    EXPECT(parse("x'unterminated"sv).is_error());
    EXPECT(parse("x'NOTHEX'"sv).is_error());

    auto validate = [](StringView sql, StringView expected_value) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::BlobLiteral>(*expression));

        auto const& literal = static_cast<const SQL::AST::BlobLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("x''"sv, ""sv);
    validate("x'DEADC0DE'"sv, "DEADC0DE"sv);
}

TEST_CASE(boolean_literal)
{
    auto validate = [](StringView sql, bool expected_value) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::BooleanLiteral>(*expression));

        auto const& literal = static_cast<SQL::AST::BooleanLiteral const&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("TRUE"sv, true);
    validate("FALSE"sv, false);
}

TEST_CASE(null_literal)
{
    auto validate = [](StringView sql) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::NullLiteral>(*expression));
    };

    validate("NULL"sv);
}

TEST_CASE(bind_parameter)
{
    auto validate = [](StringView sql) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::Placeholder>(*expression));
    };

    validate("?"sv);
}

TEST_CASE(column_name)
{
    EXPECT(parse(".column_name"sv).is_error());
    EXPECT(parse("table_name."sv).is_error());
    EXPECT(parse("schema_name.table_name."sv).is_error());
    EXPECT(parse("\"unterminated"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::ColumnNameExpression>(*expression));

        auto const& column = static_cast<const SQL::AST::ColumnNameExpression&>(*expression);
        EXPECT_EQ(column.schema_name(), expected_schema);
        EXPECT_EQ(column.table_name(), expected_table);
        EXPECT_EQ(column.column_name(), expected_column);
    };

    validate("column_name"sv, {}, {}, "COLUMN_NAME"sv);
    validate("table_name.column_name"sv, {}, "TABLE_NAME"sv, "COLUMN_NAME"sv);
    validate("schema_name.table_name.column_name"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, "COLUMN_NAME"sv);
    validate("\"Column_Name\""sv, {}, {}, "Column_Name"sv);
    validate("\"Column\n_Name\""sv, {}, {}, "Column\n_Name"sv);
}

TEST_CASE(unary_operator)
{
    EXPECT(parse("-"sv).is_error());
    EXPECT(parse("--"sv).is_error());
    EXPECT(parse("+"sv).is_error());
    EXPECT(parse("++"sv).is_error());
    EXPECT(parse("~"sv).is_error());
    EXPECT(parse("~~"sv).is_error());
    EXPECT(parse("NOT"sv).is_error());

    auto validate = [](StringView sql, SQL::AST::UnaryOperator expected_operator) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::UnaryOperatorExpression>(*expression));

        auto const& unary = static_cast<const SQL::AST::UnaryOperatorExpression&>(*expression);
        EXPECT_EQ(unary.type(), expected_operator);

        auto const& secondary_expression = unary.expression();
        EXPECT(!is<SQL::AST::ErrorExpression>(*secondary_expression));
    };

    validate("-15"sv, SQL::AST::UnaryOperator::Minus);
    validate("+15"sv, SQL::AST::UnaryOperator::Plus);
    validate("~15"sv, SQL::AST::UnaryOperator::BitwiseNot);
    validate("NOT 15"sv, SQL::AST::UnaryOperator::Not);
}

TEST_CASE(binary_operator)
{
    HashMap<StringView, SQL::AST::BinaryOperator> operators {
        { "||"sv, SQL::AST::BinaryOperator::Concatenate },
        { "*"sv, SQL::AST::BinaryOperator::Multiplication },
        { "/"sv, SQL::AST::BinaryOperator::Division },
        { "%"sv, SQL::AST::BinaryOperator::Modulo },
        { "+"sv, SQL::AST::BinaryOperator::Plus },
        { "-"sv, SQL::AST::BinaryOperator::Minus },
        { "<<"sv, SQL::AST::BinaryOperator::ShiftLeft },
        { ">>"sv, SQL::AST::BinaryOperator::ShiftRight },
        { "&"sv, SQL::AST::BinaryOperator::BitwiseAnd },
        { "|"sv, SQL::AST::BinaryOperator::BitwiseOr },
        { "<"sv, SQL::AST::BinaryOperator::LessThan },
        { "<="sv, SQL::AST::BinaryOperator::LessThanEquals },
        { ">"sv, SQL::AST::BinaryOperator::GreaterThan },
        { ">="sv, SQL::AST::BinaryOperator::GreaterThanEquals },
        { "="sv, SQL::AST::BinaryOperator::Equals },
        { "=="sv, SQL::AST::BinaryOperator::Equals },
        { "!="sv, SQL::AST::BinaryOperator::NotEquals },
        { "<>"sv, SQL::AST::BinaryOperator::NotEquals },
        { "AND"sv, SQL::AST::BinaryOperator::And },
        { "OR"sv, SQL::AST::BinaryOperator::Or },
    };

    for (auto op : operators) {
        EXPECT(parse(op.key).is_error());

        StringBuilder builder;
        builder.append("1 "sv);
        builder.append(op.key);
        EXPECT(parse(builder.to_byte_string()).is_error());

        builder.clear();

        if (op.key != "+" && op.key != "-") { // "+1" and "-1" are fine (unary operator).
            builder.append(op.key);
            builder.append(" 1"sv);
            EXPECT(parse(builder.to_byte_string()).is_error());
        }
    }

    auto validate = [](StringView sql, SQL::AST::BinaryOperator expected_operator) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::BinaryOperatorExpression>(*expression));

        auto const& binary = static_cast<const SQL::AST::BinaryOperatorExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*binary.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*binary.rhs()));
        EXPECT_EQ(binary.type(), expected_operator);
    };

    for (auto op : operators) {
        StringBuilder builder;
        builder.append("1 "sv);
        builder.append(op.key);
        builder.append(" 1"sv);
        validate(builder.to_byte_string(), op.value);
    }
}

TEST_CASE(chained_expression)
{
    EXPECT(parse("()"sv).is_error());
    EXPECT(parse("(,)"sv).is_error());
    EXPECT(parse("(15,)"sv).is_error());

    auto validate = [](StringView sql, size_t expected_chain_size) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::ChainedExpression>(*expression));

        auto const& chain = static_cast<const SQL::AST::ChainedExpression&>(*expression).expressions();
        EXPECT_EQ(chain.size(), expected_chain_size);

        for (auto const& chained_expression : chain)
            EXPECT(!is<SQL::AST::ErrorExpression>(chained_expression));
    };

    validate("(15)"sv, 1);
    validate("(15, 16)"sv, 2);
    validate("(15, 16, column_name)"sv, 3);
}

TEST_CASE(cast_expression)
{
    EXPECT(parse("CAST"sv).is_error());
    EXPECT(parse("CAST ("sv).is_error());
    EXPECT(parse("CAST ()"sv).is_error());
    EXPECT(parse("CAST (15)"sv).is_error());
    EXPECT(parse("CAST (15 AS"sv).is_error());
    EXPECT(parse("CAST (15 AS)"sv).is_error());
    EXPECT(parse("CAST (15 AS int"sv).is_error());

    auto validate = [](StringView sql, StringView expected_type_name) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::CastExpression>(*expression));

        auto const& cast = static_cast<const SQL::AST::CastExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*cast.expression()));

        auto const& type_name = cast.type_name();
        EXPECT_EQ(type_name->name(), expected_type_name);
    };

    validate("CAST (15 AS int)"sv, "INT"sv);
    // FIXME The syntax in the test below fails on both sqlite3 and psql (PostgreSQL).
    // Also fails here because null is interpreted as the NULL keyword and not the
    // identifier null (which is not a type)
    // validate("CAST ('NULL' AS null)"sv, "null"sv);
    validate("CAST (15 AS varchar(255))"sv, "VARCHAR"sv);
}

TEST_CASE(case_expression)
{
    EXPECT(parse("CASE"sv).is_error());
    EXPECT(parse("CASE END"sv).is_error());
    EXPECT(parse("CASE 15"sv).is_error());
    EXPECT(parse("CASE 15 END"sv).is_error());
    EXPECT(parse("CASE WHEN"sv).is_error());
    EXPECT(parse("CASE WHEN THEN"sv).is_error());
    EXPECT(parse("CASE WHEN 15 THEN 16"sv).is_error());
    EXPECT(parse("CASE WHEN 15 THEN 16 ELSE"sv).is_error());
    EXPECT(parse("CASE WHEN 15 THEN 16 ELSE END"sv).is_error());

    auto validate = [](StringView sql, bool expect_case_expression, size_t expected_when_then_size, bool expect_else_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::CaseExpression>(*expression));

        auto const& case_ = static_cast<const SQL::AST::CaseExpression&>(*expression);

        auto const& case_expression = case_.case_expression();
        EXPECT_EQ(case_expression.is_null(), !expect_case_expression);
        if (case_expression)
            EXPECT(!is<SQL::AST::ErrorExpression>(*case_expression));

        auto const& when_then_clauses = case_.when_then_clauses();
        EXPECT_EQ(when_then_clauses.size(), expected_when_then_size);
        for (auto const& when_then_clause : when_then_clauses) {
            EXPECT(!is<SQL::AST::ErrorExpression>(*when_then_clause.when));
            EXPECT(!is<SQL::AST::ErrorExpression>(*when_then_clause.then));
        }

        auto const& else_expression = case_.else_expression();
        EXPECT_EQ(else_expression.is_null(), !expect_else_expression);
        if (else_expression)
            EXPECT(!is<SQL::AST::ErrorExpression>(*else_expression));
    };

    validate("CASE WHEN 16 THEN 17 END"sv, false, 1, false);
    validate("CASE WHEN 16 THEN 17 WHEN 18 THEN 19 END"sv, false, 2, false);
    validate("CASE WHEN 16 THEN 17 WHEN 18 THEN 19 ELSE 20 END"sv, false, 2, true);

    validate("CASE 15 WHEN 16 THEN 17 END"sv, true, 1, false);
    validate("CASE 15 WHEN 16 THEN 17 WHEN 18 THEN 19 END"sv, true, 2, false);
    validate("CASE 15 WHEN 16 THEN 17 WHEN 18 THEN 19 ELSE 20 END"sv, true, 2, true);
}

TEST_CASE(exists_expression)
{
    EXPECT(parse("EXISTS"sv).is_error());
    EXPECT(parse("EXISTS ("sv).is_error());
    EXPECT(parse("EXISTS (SELECT"sv).is_error());
    EXPECT(parse("EXISTS (SELECT)"sv).is_error());
    EXPECT(parse("EXISTS (SELECT * FROM table_name"sv).is_error());
    EXPECT(parse("NOT EXISTS"sv).is_error());
    EXPECT(parse("NOT EXISTS ("sv).is_error());
    EXPECT(parse("NOT EXISTS (SELECT"sv).is_error());
    EXPECT(parse("NOT EXISTS (SELECT)"sv).is_error());
    EXPECT(parse("NOT EXISTS (SELECT * FROM table_name"sv).is_error());
    EXPECT(parse("("sv).is_error());
    EXPECT(parse("(SELECT"sv).is_error());
    EXPECT(parse("(SELECT)"sv).is_error());
    EXPECT(parse("(SELECT * FROM table_name"sv).is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::ExistsExpression>(*expression));

        auto const& exists = static_cast<const SQL::AST::ExistsExpression&>(*expression);
        EXPECT_EQ(exists.invert_expression(), expected_invert_expression);
    };

    validate("EXISTS (SELECT * FROM table_name)"sv, false);
    validate("NOT EXISTS (SELECT * FROM table_name)"sv, true);
    validate("(SELECT * FROM table_name)"sv, false);
}

TEST_CASE(collate_expression)
{
    EXPECT(parse("COLLATE"sv).is_error());
    EXPECT(parse("COLLATE name"sv).is_error());
    EXPECT(parse("15 COLLATE"sv).is_error());

    auto validate = [](StringView sql, StringView expected_collation_name) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::CollateExpression>(*expression));

        auto const& collate = static_cast<const SQL::AST::CollateExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*collate.expression()));
        EXPECT_EQ(collate.collation_name(), expected_collation_name);
    };

    validate("15 COLLATE fifteen"sv, "FIFTEEN"sv);
    validate("(15, 16) COLLATE \"chain\""sv, "chain"sv);
}

TEST_CASE(is_expression)
{
    EXPECT(parse("IS"sv).is_error());
    EXPECT(parse("IS 1"sv).is_error());
    EXPECT(parse("1 IS"sv).is_error());
    EXPECT(parse("IS NOT"sv).is_error());
    EXPECT(parse("IS NOT 1"sv).is_error());
    EXPECT(parse("1 IS NOT"sv).is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::IsExpression>(*expression));

        auto const& is_ = static_cast<const SQL::AST::IsExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*is_.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*is_.rhs()));
        EXPECT_EQ(is_.invert_expression(), expected_invert_expression);
    };

    validate("1 IS NULL"sv, false);
    validate("1 IS NOT NULL"sv, true);
}

TEST_CASE(match_expression)
{
    HashMap<StringView, SQL::AST::MatchOperator> operators {
        { "LIKE"sv, SQL::AST::MatchOperator::Like },
        { "GLOB"sv, SQL::AST::MatchOperator::Glob },
        { "MATCH"sv, SQL::AST::MatchOperator::Match },
        { "REGEXP"sv, SQL::AST::MatchOperator::Regexp },
    };

    for (auto op : operators) {
        EXPECT(parse(op.key).is_error());

        StringBuilder builder;
        builder.append("1 "sv);
        builder.append(op.key);
        EXPECT(parse(builder.to_byte_string()).is_error());

        builder.clear();
        builder.append(op.key);
        builder.append(" 1"sv);
        EXPECT(parse(builder.to_byte_string()).is_error());
    }

    auto validate = [](StringView sql, SQL::AST::MatchOperator expected_operator, bool expected_invert_expression, bool expect_escape) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::MatchExpression>(*expression));

        auto const& match = static_cast<const SQL::AST::MatchExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*match.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*match.rhs()));
        EXPECT_EQ(match.type(), expected_operator);
        EXPECT_EQ(match.invert_expression(), expected_invert_expression);
        EXPECT(match.escape() || !expect_escape);
    };

    for (auto op : operators) {
        StringBuilder builder;
        builder.append("1 "sv);
        builder.append(op.key);
        builder.append(" 1"sv);
        validate(builder.to_byte_string(), op.value, false, false);

        builder.clear();
        builder.append("1 NOT "sv);
        builder.append(op.key);
        builder.append(" 1"sv);
        validate(builder.to_byte_string(), op.value, true, false);

        builder.clear();
        builder.append("1 NOT "sv);
        builder.append(op.key);
        builder.append(" 1 ESCAPE '+'"sv);
        validate(builder.to_byte_string(), op.value, true, true);
    }
}

TEST_CASE(null_expression)
{
    EXPECT(parse("ISNULL"sv).is_error());
    EXPECT(parse("NOTNULL"sv).is_error());
    EXPECT(parse("15 NOT"sv).is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::NullExpression>(*expression));

        auto const& null = static_cast<const SQL::AST::NullExpression&>(*expression);
        EXPECT_EQ(null.invert_expression(), expected_invert_expression);
    };

    validate("15 ISNULL"sv, false);
    validate("15 NOTNULL"sv, true);
    validate("15 NOT NULL"sv, true);
}

TEST_CASE(between_expression)
{
    EXPECT(parse("BETWEEN"sv).is_error());
    EXPECT(parse("NOT BETWEEN"sv).is_error());
    EXPECT(parse("BETWEEN 10 AND 20"sv).is_error());
    EXPECT(parse("NOT BETWEEN 10 AND 20"sv).is_error());
    EXPECT(parse("15 BETWEEN 10"sv).is_error());
    EXPECT(parse("15 BETWEEN 10 AND"sv).is_error());
    EXPECT(parse("15 BETWEEN AND 20"sv).is_error());
    EXPECT(parse("15 BETWEEN 10 OR 20"sv).is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::BetweenExpression>(*expression));

        auto const& between = static_cast<const SQL::AST::BetweenExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*between.expression()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*between.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*between.rhs()));
        EXPECT_EQ(between.invert_expression(), expected_invert_expression);
    };

    validate("15 BETWEEN 10 AND 20"sv, false);
    validate("15 NOT BETWEEN 10 AND 20"sv, true);
}

TEST_CASE(in_table_expression)
{
    EXPECT(parse("IN"sv).is_error());
    EXPECT(parse("IN table_name"sv).is_error());
    EXPECT(parse("NOT IN"sv).is_error());
    EXPECT(parse("NOT IN table_name"sv).is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::InTableExpression>(*expression));

        auto const& in = static_cast<const SQL::AST::InTableExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.schema_name(), expected_schema);
        EXPECT_EQ(in.table_name(), expected_table);
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);
    };

    validate("15 IN table_name"sv, {}, "TABLE_NAME"sv, false);
    validate("15 IN schema_name.table_name"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, false);

    validate("15 NOT IN table_name"sv, {}, "TABLE_NAME"sv, true);
    validate("15 NOT IN schema_name.table_name"sv, "SCHEMA_NAME"sv, "TABLE_NAME"sv, true);
}

TEST_CASE(in_chained_expression)
{
    EXPECT(parse("IN ()"sv).is_error());
    EXPECT(parse("NOT IN ()"sv).is_error());

    auto validate = [](StringView sql, size_t expected_chain_size, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::InChainedExpression>(*expression));

        auto const& in = static_cast<const SQL::AST::InChainedExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.expression_chain()->expressions().size(), expected_chain_size);
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);

        for (auto const& chained_expression : in.expression_chain()->expressions())
            EXPECT(!is<SQL::AST::ErrorExpression>(chained_expression));
    };

    validate("15 IN ()"sv, 0, false);
    validate("15 IN (15)"sv, 1, false);
    validate("15 IN (15, 16)"sv, 2, false);

    validate("15 NOT IN ()"sv, 0, true);
    validate("15 NOT IN (15)"sv, 1, true);
    validate("15 NOT IN (15, 16)"sv, 2, true);
}

TEST_CASE(in_selection_expression)
{
    EXPECT(parse("IN (SELECT)"sv).is_error());
    EXPECT(parse("IN (SELECT * FROM table_name, SELECT * FROM table_name);"sv).is_error());
    EXPECT(parse("NOT IN (SELECT)"sv).is_error());
    EXPECT(parse("NOT IN (SELECT * FROM table_name, SELECT * FROM table_name);"sv).is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto expression = TRY_OR_FAIL(parse(sql));
        EXPECT(is<SQL::AST::InSelectionExpression>(*expression));

        auto const& in = static_cast<const SQL::AST::InSelectionExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);
    };

    validate("15 IN (SELECT * FROM table_name)"sv, false);
    validate("15 NOT IN (SELECT * FROM table_name)"sv, true);
}

TEST_CASE(expression_tree_depth_limit)
{
    auto too_deep_expression = ByteString::formatted("{:+^{}}1", "", SQL::AST::Limits::maximum_expression_tree_depth);
    EXPECT(!parse(too_deep_expression.substring_view(1)).is_error());
    EXPECT(parse(too_deep_expression).is_error());
}
