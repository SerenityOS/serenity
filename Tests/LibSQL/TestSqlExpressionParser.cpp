/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashMap.h>
#include <AK/Result.h>
#include <AK/String.h>
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

using ParseResult = AK::Result<NonnullRefPtr<SQL::AST::Expression>, String>;

ParseResult parse(StringView sql)
{
    auto parser = ExpressionParser(SQL::AST::Lexer(sql));
    auto expression = parser.parse();

    if (parser.has_errors()) {
        return parser.errors()[0].to_string();
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
    // EXPECT(parse("1e").is_error());
    // EXPECT(parse("1a").is_error());
    // EXPECT(parse("0x").is_error());

    auto validate = [](StringView sql, double expected_value) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::NumericLiteral>(*expression));

        const auto& literal = static_cast<const SQL::AST::NumericLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("123", 123);
    validate("3.14", 3.14);
    validate("0xA", 10);
    validate("0xff", 255);
    validate("0x100", 256);
    validate("1e3", 1000);
}

TEST_CASE(string_literal)
{
    EXPECT(parse("'").is_error());
    EXPECT(parse("'unterminated").is_error());

    auto validate = [](StringView sql, StringView expected_value) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::StringLiteral>(*expression));

        const auto& literal = static_cast<const SQL::AST::StringLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("''", "");
    validate("'hello friends'", "hello friends");
    validate("'hello ''friends'''", "hello 'friends'");
}

TEST_CASE(blob_literal)
{
    EXPECT(parse("x'").is_error());
    EXPECT(parse("x'unterminated").is_error());
    EXPECT(parse("x'NOTHEX'").is_error());

    auto validate = [](StringView sql, StringView expected_value) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::BlobLiteral>(*expression));

        const auto& literal = static_cast<const SQL::AST::BlobLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("x''", "");
    validate("x'DEADC0DE'", "DEADC0DE");
}

TEST_CASE(null_literal)
{
    auto validate = [](StringView sql) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::NullLiteral>(*expression));
    };

    validate("NULL");
}

TEST_CASE(column_name)
{
    EXPECT(parse(".column_name").is_error());
    EXPECT(parse("table_name.").is_error());
    EXPECT(parse("schema_name.table_name.").is_error());
    EXPECT(parse("\"unterminated").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::ColumnNameExpression>(*expression));

        const auto& column = static_cast<const SQL::AST::ColumnNameExpression&>(*expression);
        EXPECT_EQ(column.schema_name(), expected_schema);
        EXPECT_EQ(column.table_name(), expected_table);
        EXPECT_EQ(column.column_name(), expected_column);
    };

    validate("column_name", {}, {}, "COLUMN_NAME");
    validate("table_name.column_name", {}, "TABLE_NAME", "COLUMN_NAME");
    validate("schema_name.table_name.column_name", "SCHEMA_NAME", "TABLE_NAME", "COLUMN_NAME");
    validate("\"Column_Name\"", {}, {}, "Column_Name");
    validate("\"Column\n_Name\"", {}, {}, "Column\n_Name");
}

TEST_CASE(unary_operator)
{
    EXPECT(parse("-").is_error());
    EXPECT(parse("--").is_error());
    EXPECT(parse("+").is_error());
    EXPECT(parse("++").is_error());
    EXPECT(parse("~").is_error());
    EXPECT(parse("~~").is_error());
    EXPECT(parse("NOT").is_error());

    auto validate = [](StringView sql, SQL::AST::UnaryOperator expected_operator) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::UnaryOperatorExpression>(*expression));

        const auto& unary = static_cast<const SQL::AST::UnaryOperatorExpression&>(*expression);
        EXPECT_EQ(unary.type(), expected_operator);

        const auto& secondary_expression = unary.expression();
        EXPECT(!is<SQL::AST::ErrorExpression>(*secondary_expression));
    };

    validate("-15", SQL::AST::UnaryOperator::Minus);
    validate("+15", SQL::AST::UnaryOperator::Plus);
    validate("~15", SQL::AST::UnaryOperator::BitwiseNot);
    validate("NOT 15", SQL::AST::UnaryOperator::Not);
}

TEST_CASE(binary_operator)
{
    HashMap<StringView, SQL::AST::BinaryOperator> operators {
        { "||", SQL::AST::BinaryOperator::Concatenate },
        { "*", SQL::AST::BinaryOperator::Multiplication },
        { "/", SQL::AST::BinaryOperator::Division },
        { "%", SQL::AST::BinaryOperator::Modulo },
        { "+", SQL::AST::BinaryOperator::Plus },
        { "-", SQL::AST::BinaryOperator::Minus },
        { "<<", SQL::AST::BinaryOperator::ShiftLeft },
        { ">>", SQL::AST::BinaryOperator::ShiftRight },
        { "&", SQL::AST::BinaryOperator::BitwiseAnd },
        { "|", SQL::AST::BinaryOperator::BitwiseOr },
        { "<", SQL::AST::BinaryOperator::LessThan },
        { "<=", SQL::AST::BinaryOperator::LessThanEquals },
        { ">", SQL::AST::BinaryOperator::GreaterThan },
        { ">=", SQL::AST::BinaryOperator::GreaterThanEquals },
        { "=", SQL::AST::BinaryOperator::Equals },
        { "==", SQL::AST::BinaryOperator::Equals },
        { "!=", SQL::AST::BinaryOperator::NotEquals },
        { "<>", SQL::AST::BinaryOperator::NotEquals },
        { "AND", SQL::AST::BinaryOperator::And },
        { "OR", SQL::AST::BinaryOperator::Or },
    };

    for (auto op : operators) {
        EXPECT(parse(op.key).is_error());

        StringBuilder builder;
        builder.append("1 ");
        builder.append(op.key);
        EXPECT(parse(builder.build()).is_error());

        builder.clear();

        if (op.key != "+" && op.key != "-") { // "+1" and "-1" are fine (unary operator).
            builder.append(op.key);
            builder.append(" 1");
            EXPECT(parse(builder.build()).is_error());
        }
    }

    auto validate = [](StringView sql, SQL::AST::BinaryOperator expected_operator) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::BinaryOperatorExpression>(*expression));

        const auto& binary = static_cast<const SQL::AST::BinaryOperatorExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*binary.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*binary.rhs()));
        EXPECT_EQ(binary.type(), expected_operator);
    };

    for (auto op : operators) {
        StringBuilder builder;
        builder.append("1 ");
        builder.append(op.key);
        builder.append(" 1");
        validate(builder.build(), op.value);
    }
}

TEST_CASE(chained_expression)
{
    EXPECT(parse("()").is_error());
    EXPECT(parse("(,)").is_error());
    EXPECT(parse("(15,)").is_error());

    auto validate = [](StringView sql, size_t expected_chain_size) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::ChainedExpression>(*expression));

        const auto& chain = static_cast<const SQL::AST::ChainedExpression&>(*expression).expressions();
        EXPECT_EQ(chain.size(), expected_chain_size);

        for (const auto& chained_expression : chain)
            EXPECT(!is<SQL::AST::ErrorExpression>(chained_expression));
    };

    validate("(15)", 1);
    validate("(15, 16)", 2);
    validate("(15, 16, column_name)", 3);
}

TEST_CASE(cast_expression)
{
    EXPECT(parse("CAST").is_error());
    EXPECT(parse("CAST (").is_error());
    EXPECT(parse("CAST ()").is_error());
    EXPECT(parse("CAST (15)").is_error());
    EXPECT(parse("CAST (15 AS").is_error());
    EXPECT(parse("CAST (15 AS)").is_error());
    EXPECT(parse("CAST (15 AS int").is_error());

    auto validate = [](StringView sql, StringView expected_type_name) {
        auto result = parse(sql);
        if (result.is_error())
            outln("{}: {}", sql, result.error());
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::CastExpression>(*expression));

        const auto& cast = static_cast<const SQL::AST::CastExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*cast.expression()));

        const auto& type_name = cast.type_name();
        EXPECT_EQ(type_name->name(), expected_type_name);
    };

    validate("CAST (15 AS int)", "INT");
    // FIXME The syntax in the test below fails on both sqlite3 and psql (PostgreSQL).
    // Also fails here because null is interpreted as the NULL keyword and not the
    // identifier null (which is not a type)
    // validate("CAST ('NULL' AS null)", "null");
    validate("CAST (15 AS varchar(255))", "VARCHAR");
}

TEST_CASE(case_expression)
{
    EXPECT(parse("CASE").is_error());
    EXPECT(parse("CASE END").is_error());
    EXPECT(parse("CASE 15").is_error());
    EXPECT(parse("CASE 15 END").is_error());
    EXPECT(parse("CASE WHEN").is_error());
    EXPECT(parse("CASE WHEN THEN").is_error());
    EXPECT(parse("CASE WHEN 15 THEN 16").is_error());
    EXPECT(parse("CASE WHEN 15 THEN 16 ELSE").is_error());
    EXPECT(parse("CASE WHEN 15 THEN 16 ELSE END").is_error());

    auto validate = [](StringView sql, bool expect_case_expression, size_t expected_when_then_size, bool expect_else_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::CaseExpression>(*expression));

        const auto& case_ = static_cast<const SQL::AST::CaseExpression&>(*expression);

        const auto& case_expression = case_.case_expression();
        EXPECT_EQ(case_expression.is_null(), !expect_case_expression);
        if (case_expression)
            EXPECT(!is<SQL::AST::ErrorExpression>(*case_expression));

        const auto& when_then_clauses = case_.when_then_clauses();
        EXPECT_EQ(when_then_clauses.size(), expected_when_then_size);
        for (const auto& when_then_clause : when_then_clauses) {
            EXPECT(!is<SQL::AST::ErrorExpression>(*when_then_clause.when));
            EXPECT(!is<SQL::AST::ErrorExpression>(*when_then_clause.then));
        }

        const auto& else_expression = case_.else_expression();
        EXPECT_EQ(else_expression.is_null(), !expect_else_expression);
        if (else_expression)
            EXPECT(!is<SQL::AST::ErrorExpression>(*else_expression));
    };

    validate("CASE WHEN 16 THEN 17 END", false, 1, false);
    validate("CASE WHEN 16 THEN 17 WHEN 18 THEN 19 END", false, 2, false);
    validate("CASE WHEN 16 THEN 17 WHEN 18 THEN 19 ELSE 20 END", false, 2, true);

    validate("CASE 15 WHEN 16 THEN 17 END", true, 1, false);
    validate("CASE 15 WHEN 16 THEN 17 WHEN 18 THEN 19 END", true, 2, false);
    validate("CASE 15 WHEN 16 THEN 17 WHEN 18 THEN 19 ELSE 20 END", true, 2, true);
}

TEST_CASE(exists_expression)
{
    EXPECT(parse("EXISTS").is_error());
    EXPECT(parse("EXISTS (").is_error());
    EXPECT(parse("EXISTS (SELECT").is_error());
    EXPECT(parse("EXISTS (SELECT)").is_error());
    EXPECT(parse("EXISTS (SELECT * FROM table_name").is_error());
    EXPECT(parse("NOT EXISTS").is_error());
    EXPECT(parse("NOT EXISTS (").is_error());
    EXPECT(parse("NOT EXISTS (SELECT").is_error());
    EXPECT(parse("NOT EXISTS (SELECT)").is_error());
    EXPECT(parse("NOT EXISTS (SELECT * FROM table_name").is_error());
    EXPECT(parse("(").is_error());
    EXPECT(parse("(SELECT").is_error());
    EXPECT(parse("(SELECT)").is_error());
    EXPECT(parse("(SELECT * FROM table_name").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::ExistsExpression>(*expression));

        const auto& exists = static_cast<const SQL::AST::ExistsExpression&>(*expression);
        EXPECT_EQ(exists.invert_expression(), expected_invert_expression);
    };

    validate("EXISTS (SELECT * FROM table_name)", false);
    validate("NOT EXISTS (SELECT * FROM table_name)", true);
    validate("(SELECT * FROM table_name)", false);
}

TEST_CASE(collate_expression)
{
    EXPECT(parse("COLLATE").is_error());
    EXPECT(parse("COLLATE name").is_error());
    EXPECT(parse("15 COLLATE").is_error());

    auto validate = [](StringView sql, StringView expected_collation_name) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::CollateExpression>(*expression));

        const auto& collate = static_cast<const SQL::AST::CollateExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*collate.expression()));
        EXPECT_EQ(collate.collation_name(), expected_collation_name);
    };

    validate("15 COLLATE fifteen", "FIFTEEN");
    validate("(15, 16) COLLATE \"chain\"", "chain");
}

TEST_CASE(is_expression)
{
    EXPECT(parse("IS").is_error());
    EXPECT(parse("IS 1").is_error());
    EXPECT(parse("1 IS").is_error());
    EXPECT(parse("IS NOT").is_error());
    EXPECT(parse("IS NOT 1").is_error());
    EXPECT(parse("1 IS NOT").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::IsExpression>(*expression));

        const auto& is_ = static_cast<const SQL::AST::IsExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*is_.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*is_.rhs()));
        EXPECT_EQ(is_.invert_expression(), expected_invert_expression);
    };

    validate("1 IS NULL", false);
    validate("1 IS NOT NULL", true);
}

TEST_CASE(match_expression)
{
    HashMap<StringView, SQL::AST::MatchOperator> operators {
        { "LIKE", SQL::AST::MatchOperator::Like },
        { "GLOB", SQL::AST::MatchOperator::Glob },
        { "MATCH", SQL::AST::MatchOperator::Match },
        { "REGEXP", SQL::AST::MatchOperator::Regexp },
    };

    for (auto op : operators) {
        EXPECT(parse(op.key).is_error());

        StringBuilder builder;
        builder.append("1 ");
        builder.append(op.key);
        EXPECT(parse(builder.build()).is_error());

        builder.clear();
        builder.append(op.key);
        builder.append(" 1");
        EXPECT(parse(builder.build()).is_error());
    }

    auto validate = [](StringView sql, SQL::AST::MatchOperator expected_operator, bool expected_invert_expression, bool expect_escape) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::MatchExpression>(*expression));

        const auto& match = static_cast<const SQL::AST::MatchExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*match.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*match.rhs()));
        EXPECT_EQ(match.type(), expected_operator);
        EXPECT_EQ(match.invert_expression(), expected_invert_expression);
        EXPECT(match.escape() || !expect_escape);
    };

    for (auto op : operators) {
        StringBuilder builder;
        builder.append("1 ");
        builder.append(op.key);
        builder.append(" 1");
        validate(builder.build(), op.value, false, false);

        builder.clear();
        builder.append("1 NOT ");
        builder.append(op.key);
        builder.append(" 1");
        validate(builder.build(), op.value, true, false);

        builder.clear();
        builder.append("1 NOT ");
        builder.append(op.key);
        builder.append(" 1 ESCAPE '+'");
        validate(builder.build(), op.value, true, true);
    }
}

TEST_CASE(null_expression)
{
    EXPECT(parse("ISNULL").is_error());
    EXPECT(parse("NOTNULL").is_error());
    EXPECT(parse("15 NOT").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::NullExpression>(*expression));

        const auto& null = static_cast<const SQL::AST::NullExpression&>(*expression);
        EXPECT_EQ(null.invert_expression(), expected_invert_expression);
    };

    validate("15 ISNULL", false);
    validate("15 NOTNULL", true);
    validate("15 NOT NULL", true);
}

TEST_CASE(between_expression)
{
    EXPECT(parse("BETWEEN").is_error());
    EXPECT(parse("NOT BETWEEN").is_error());
    EXPECT(parse("BETWEEN 10 AND 20").is_error());
    EXPECT(parse("NOT BETWEEN 10 AND 20").is_error());
    EXPECT(parse("15 BETWEEN 10").is_error());
    EXPECT(parse("15 BETWEEN 10 AND").is_error());
    EXPECT(parse("15 BETWEEN AND 20").is_error());
    EXPECT(parse("15 BETWEEN 10 OR 20").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::BetweenExpression>(*expression));

        const auto& between = static_cast<const SQL::AST::BetweenExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*between.expression()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*between.lhs()));
        EXPECT(!is<SQL::AST::ErrorExpression>(*between.rhs()));
        EXPECT_EQ(between.invert_expression(), expected_invert_expression);
    };

    validate("15 BETWEEN 10 AND 20", false);
    validate("15 NOT BETWEEN 10 AND 20", true);
}

TEST_CASE(in_table_expression)
{
    EXPECT(parse("IN").is_error());
    EXPECT(parse("IN table_name").is_error());
    EXPECT(parse("NOT IN").is_error());
    EXPECT(parse("NOT IN table_name").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::InTableExpression>(*expression));

        const auto& in = static_cast<const SQL::AST::InTableExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.schema_name(), expected_schema);
        EXPECT_EQ(in.table_name(), expected_table);
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);
    };

    validate("15 IN table_name", {}, "TABLE_NAME", false);
    validate("15 IN schema_name.table_name", "SCHEMA_NAME", "TABLE_NAME", false);

    validate("15 NOT IN table_name", {}, "TABLE_NAME", true);
    validate("15 NOT IN schema_name.table_name", "SCHEMA_NAME", "TABLE_NAME", true);
}

TEST_CASE(in_chained_expression)
{
    EXPECT(parse("IN ()").is_error());
    EXPECT(parse("NOT IN ()").is_error());

    auto validate = [](StringView sql, size_t expected_chain_size, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::InChainedExpression>(*expression));

        const auto& in = static_cast<const SQL::AST::InChainedExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.expression_chain()->expressions().size(), expected_chain_size);
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);

        for (const auto& chained_expression : in.expression_chain()->expressions())
            EXPECT(!is<SQL::AST::ErrorExpression>(chained_expression));
    };

    validate("15 IN ()", 0, false);
    validate("15 IN (15)", 1, false);
    validate("15 IN (15, 16)", 2, false);

    validate("15 NOT IN ()", 0, true);
    validate("15 NOT IN (15)", 1, true);
    validate("15 NOT IN (15, 16)", 2, true);
}

TEST_CASE(in_selection_expression)
{
    EXPECT(parse("IN (SELECT)").is_error());
    EXPECT(parse("IN (SELECT * FROM table_name, SELECT * FROM table_name);").is_error());
    EXPECT(parse("NOT IN (SELECT)").is_error());
    EXPECT(parse("NOT IN (SELECT * FROM table_name, SELECT * FROM table_name);").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::AST::InSelectionExpression>(*expression));

        const auto& in = static_cast<const SQL::AST::InSelectionExpression&>(*expression);
        EXPECT(!is<SQL::AST::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);
    };

    validate("15 IN (SELECT * FROM table_name)", false);
    validate("15 NOT IN (SELECT * FROM table_name)", true);
}

TEST_CASE(expression_tree_depth_limit)
{
    auto too_deep_expression = String::formatted("{:+^{}}1", "", SQL::AST::Limits::maximum_expression_tree_depth);
    EXPECT(!parse(too_deep_expression.substring_view(1)).is_error());
    EXPECT(parse(too_deep_expression).is_error());
}
