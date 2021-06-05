/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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
#include <LibSQL/Lexer.h>
#include <LibSQL/Parser.h>

namespace {

class ExpressionParser : public SQL::Parser {
public:
    explicit ExpressionParser(SQL::Lexer lexer)
        : SQL::Parser(move(lexer))
    {
    }

    NonnullRefPtr<SQL::Expression> parse()
    {
        return SQL::Parser::parse_expression();
    }
};

using ParseResult = AK::Result<NonnullRefPtr<SQL::Expression>, String>;

ParseResult parse(StringView sql)
{
    auto parser = ExpressionParser(SQL::Lexer(sql));
    auto expression = parser.parse();

    if (parser.has_errors()) {
        return parser.errors()[0].to_string();
    }

    return expression;
}

}

TEST_CASE(numeric_literal)
{
    auto validate = [](StringView sql, double expected_value) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::NumericLiteral>(*expression));

        const auto& literal = static_cast<const SQL::NumericLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("123", 123);
    validate("3.14", 3.14);
    validate("0xff", 255);
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
        EXPECT(is<SQL::StringLiteral>(*expression));

        const auto& literal = static_cast<const SQL::StringLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("''", "''");
    validate("'hello friends'", "'hello friends'");
}

TEST_CASE(blob_literal)
{
    EXPECT(parse("x'").is_error());
    EXPECT(parse("x'unterminated").is_error());

    auto validate = [](StringView sql, StringView expected_value) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::BlobLiteral>(*expression));

        const auto& literal = static_cast<const SQL::BlobLiteral&>(*expression);
        EXPECT_EQ(literal.value(), expected_value);
    };

    validate("x''", "x''");
    validate("x'hello friends'", "x'hello friends'");
}

TEST_CASE(null_literal)
{
    auto validate = [](StringView sql) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::NullLiteral>(*expression));
    };

    validate("NULL");
}

TEST_CASE(column_name)
{
    EXPECT(parse(".column").is_error());
    EXPECT(parse("table.").is_error());
    EXPECT(parse("schema.table.").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, StringView expected_column) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::ColumnNameExpression>(*expression));

        const auto& column = static_cast<const SQL::ColumnNameExpression&>(*expression);
        EXPECT_EQ(column.schema_name(), expected_schema);
        EXPECT_EQ(column.table_name(), expected_table);
        EXPECT_EQ(column.column_name(), expected_column);
    };

    validate("column", {}, {}, "column");
    validate("table.column", {}, "table", "column");
    validate("schema.table.column", "schema", "table", "column");
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

    auto validate = [](StringView sql, SQL::UnaryOperator expected_operator) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::UnaryOperatorExpression>(*expression));

        const auto& unary = static_cast<const SQL::UnaryOperatorExpression&>(*expression);
        EXPECT_EQ(unary.type(), expected_operator);

        const auto& secondary_expression = unary.expression();
        EXPECT(!is<SQL::ErrorExpression>(*secondary_expression));
    };

    validate("-15", SQL::UnaryOperator::Minus);
    validate("+15", SQL::UnaryOperator::Plus);
    validate("~15", SQL::UnaryOperator::BitwiseNot);
    validate("NOT 15", SQL::UnaryOperator::Not);
}

TEST_CASE(binary_operator)
{
    HashMap<StringView, SQL::BinaryOperator> operators {
        { "||", SQL::BinaryOperator::Concatenate },
        { "*", SQL::BinaryOperator::Multiplication },
        { "/", SQL::BinaryOperator::Division },
        { "%", SQL::BinaryOperator::Modulo },
        { "+", SQL::BinaryOperator::Plus },
        { "-", SQL::BinaryOperator::Minus },
        { "<<", SQL::BinaryOperator::ShiftLeft },
        { ">>", SQL::BinaryOperator::ShiftRight },
        { "&", SQL::BinaryOperator::BitwiseAnd },
        { "|", SQL::BinaryOperator::BitwiseOr },
        { "<", SQL::BinaryOperator::LessThan },
        { "<=", SQL::BinaryOperator::LessThanEquals },
        { ">", SQL::BinaryOperator::GreaterThan },
        { ">=", SQL::BinaryOperator::GreaterThanEquals },
        { "=", SQL::BinaryOperator::Equals },
        { "==", SQL::BinaryOperator::Equals },
        { "!=", SQL::BinaryOperator::NotEquals },
        { "<>", SQL::BinaryOperator::NotEquals },
        { "AND", SQL::BinaryOperator::And },
        { "OR", SQL::BinaryOperator::Or },
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

    auto validate = [](StringView sql, SQL::BinaryOperator expected_operator) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::BinaryOperatorExpression>(*expression));

        const auto& binary = static_cast<const SQL::BinaryOperatorExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*binary.lhs()));
        EXPECT(!is<SQL::ErrorExpression>(*binary.rhs()));
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
        EXPECT(is<SQL::ChainedExpression>(*expression));

        const auto& chain = static_cast<const SQL::ChainedExpression&>(*expression).expressions();
        EXPECT_EQ(chain.size(), expected_chain_size);

        for (const auto& chained_expression : chain)
            EXPECT(!is<SQL::ErrorExpression>(chained_expression));
    };

    validate("(15)", 1);
    validate("(15, 16)", 2);
    validate("(15, 16, column)", 3);
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
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::CastExpression>(*expression));

        const auto& cast = static_cast<const SQL::CastExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*cast.expression()));

        const auto& type_name = cast.type_name();
        EXPECT_EQ(type_name->name(), expected_type_name);
    };

    validate("CAST (15 AS int)", "int");
    validate("CAST ('NULL' AS null)", "null");
    validate("CAST (15 AS varchar(255))", "varchar");
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
        EXPECT(is<SQL::CaseExpression>(*expression));

        const auto& case_ = static_cast<const SQL::CaseExpression&>(*expression);

        const auto& case_expression = case_.case_expression();
        EXPECT_EQ(case_expression.is_null(), !expect_case_expression);
        if (case_expression)
            EXPECT(!is<SQL::ErrorExpression>(*case_expression));

        const auto& when_then_clauses = case_.when_then_clauses();
        EXPECT_EQ(when_then_clauses.size(), expected_when_then_size);
        for (const auto& when_then_clause : when_then_clauses) {
            EXPECT(!is<SQL::ErrorExpression>(*when_then_clause.when));
            EXPECT(!is<SQL::ErrorExpression>(*when_then_clause.then));
        }

        const auto& else_expression = case_.else_expression();
        EXPECT_EQ(else_expression.is_null(), !expect_else_expression);
        if (else_expression)
            EXPECT(!is<SQL::ErrorExpression>(*else_expression));
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
    EXPECT(parse("EXISTS (SELECT * FROM table").is_error());
    EXPECT(parse("NOT EXISTS").is_error());
    EXPECT(parse("NOT EXISTS (").is_error());
    EXPECT(parse("NOT EXISTS (SELECT").is_error());
    EXPECT(parse("NOT EXISTS (SELECT)").is_error());
    EXPECT(parse("NOT EXISTS (SELECT * FROM table").is_error());
    EXPECT(parse("(").is_error());
    EXPECT(parse("(SELECT").is_error());
    EXPECT(parse("(SELECT)").is_error());
    EXPECT(parse("(SELECT * FROM table").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::ExistsExpression>(*expression));

        const auto& exists = static_cast<const SQL::ExistsExpression&>(*expression);
        EXPECT_EQ(exists.invert_expression(), expected_invert_expression);
    };

    validate("EXISTS (SELECT * FROM table)", false);
    validate("NOT EXISTS (SELECT * FROM table)", true);
    validate("(SELECT * FROM table)", false);
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
        EXPECT(is<SQL::CollateExpression>(*expression));

        const auto& collate = static_cast<const SQL::CollateExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*collate.expression()));
        EXPECT_EQ(collate.collation_name(), expected_collation_name);
    };

    validate("15 COLLATE fifteen", "fifteen");
    validate("(15, 16) COLLATE chain", "chain");
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
        EXPECT(is<SQL::IsExpression>(*expression));

        const auto& is_ = static_cast<const SQL::IsExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*is_.lhs()));
        EXPECT(!is<SQL::ErrorExpression>(*is_.rhs()));
        EXPECT_EQ(is_.invert_expression(), expected_invert_expression);
    };

    validate("1 IS NULL", false);
    validate("1 IS NOT NULL", true);
}

TEST_CASE(match_expression)
{
    HashMap<StringView, SQL::MatchOperator> operators {
        { "LIKE", SQL::MatchOperator::Like },
        { "GLOB", SQL::MatchOperator::Glob },
        { "MATCH", SQL::MatchOperator::Match },
        { "REGEXP", SQL::MatchOperator::Regexp },
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

    auto validate = [](StringView sql, SQL::MatchOperator expected_operator, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::MatchExpression>(*expression));

        const auto& match = static_cast<const SQL::MatchExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*match.lhs()));
        EXPECT(!is<SQL::ErrorExpression>(*match.rhs()));
        EXPECT_EQ(match.type(), expected_operator);
        EXPECT_EQ(match.invert_expression(), expected_invert_expression);
    };

    for (auto op : operators) {
        StringBuilder builder;
        builder.append("1 ");
        builder.append(op.key);
        builder.append(" 1");
        validate(builder.build(), op.value, false);

        builder.clear();
        builder.append("1 NOT ");
        builder.append(op.key);
        builder.append(" 1");
        validate(builder.build(), op.value, true);
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
        EXPECT(is<SQL::NullExpression>(*expression));

        const auto& null = static_cast<const SQL::NullExpression&>(*expression);
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
        EXPECT(is<SQL::BetweenExpression>(*expression));

        const auto& between = static_cast<const SQL::BetweenExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*between.expression()));
        EXPECT(!is<SQL::ErrorExpression>(*between.lhs()));
        EXPECT(!is<SQL::ErrorExpression>(*between.rhs()));
        EXPECT_EQ(between.invert_expression(), expected_invert_expression);
    };

    validate("15 BETWEEN 10 AND 20", false);
    validate("15 NOT BETWEEN 10 AND 20", true);
}

TEST_CASE(in_table_expression)
{
    EXPECT(parse("IN").is_error());
    EXPECT(parse("IN table").is_error());
    EXPECT(parse("NOT IN").is_error());
    EXPECT(parse("NOT IN table").is_error());

    auto validate = [](StringView sql, StringView expected_schema, StringView expected_table, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::InTableExpression>(*expression));

        const auto& in = static_cast<const SQL::InTableExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.schema_name(), expected_schema);
        EXPECT_EQ(in.table_name(), expected_table);
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);
    };

    validate("15 IN table", {}, "table", false);
    validate("15 IN schema.table", "schema", "table", false);

    validate("15 NOT IN table", {}, "table", true);
    validate("15 NOT IN schema.table", "schema", "table", true);
}

TEST_CASE(in_chained_expression)
{
    EXPECT(parse("IN ()").is_error());
    EXPECT(parse("NOT IN ()").is_error());

    auto validate = [](StringView sql, size_t expected_chain_size, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::InChainedExpression>(*expression));

        const auto& in = static_cast<const SQL::InChainedExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.expression_chain()->expressions().size(), expected_chain_size);
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);

        for (const auto& chained_expression : in.expression_chain()->expressions())
            EXPECT(!is<SQL::ErrorExpression>(chained_expression));
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
    EXPECT(parse("IN (SELECT * FROM table, SELECT * FROM table);").is_error());
    EXPECT(parse("NOT IN (SELECT)").is_error());
    EXPECT(parse("NOT IN (SELECT * FROM table, SELECT * FROM table);").is_error());

    auto validate = [](StringView sql, bool expected_invert_expression) {
        auto result = parse(sql);
        EXPECT(!result.is_error());

        auto expression = result.release_value();
        EXPECT(is<SQL::InSelectionExpression>(*expression));

        const auto& in = static_cast<const SQL::InSelectionExpression&>(*expression);
        EXPECT(!is<SQL::ErrorExpression>(*in.expression()));
        EXPECT_EQ(in.invert_expression(), expected_invert_expression);
    };

    validate("15 IN (SELECT * FROM table)", false);
    validate("15 NOT IN (SELECT * FROM table)", true);
}

TEST_CASE(stack_limit)
{
    auto too_deep_expression = String::formatted("{:+^{}}1", "", SQL::Limits::maximum_expression_tree_depth);
    EXPECT(!parse(too_deep_expression.substring_view(1)).is_error());
    EXPECT(parse(too_deep_expression).is_error());
}
