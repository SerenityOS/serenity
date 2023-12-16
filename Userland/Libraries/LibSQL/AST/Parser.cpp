/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include <AK/ScopeGuard.h>
#include <AK/TypeCasts.h>

namespace SQL::AST {

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
}

NonnullRefPtr<Statement> Parser::next_statement()
{
    auto terminate_statement = [this](auto statement) {
        consume(TokenType::SemiColon);
        return statement;
    };

    if (match(TokenType::With)) {
        auto common_table_expression_list = parse_common_table_expression_list();
        if (!common_table_expression_list)
            return create_ast_node<ErrorStatement>();

        return terminate_statement(parse_statement_with_expression_list(move(common_table_expression_list)));
    }

    return terminate_statement(parse_statement());
}

NonnullRefPtr<Statement> Parser::parse_statement()
{
    switch (m_parser_state.m_token.type()) {
    case TokenType::Create:
        consume();
        if (match(TokenType::Schema))
            return parse_create_schema_statement();
        else
            return parse_create_table_statement();
    case TokenType::Alter:
        return parse_alter_table_statement();
    case TokenType::Drop:
        return parse_drop_table_statement();
    case TokenType::Describe:
        return parse_describe_table_statement();
    case TokenType::Insert:
        return parse_insert_statement({});
    case TokenType::Update:
        return parse_update_statement({});
    case TokenType::Delete:
        return parse_delete_statement({});
    case TokenType::Select:
        return parse_select_statement({});
    default:
        expected("CREATE, ALTER, DROP, DESCRIBE, INSERT, UPDATE, DELETE, or SELECT"sv);
        return create_ast_node<ErrorStatement>();
    }
}

NonnullRefPtr<Statement> Parser::parse_statement_with_expression_list(RefPtr<CommonTableExpressionList> common_table_expression_list)
{
    switch (m_parser_state.m_token.type()) {
    case TokenType::Insert:
        return parse_insert_statement(move(common_table_expression_list));
    case TokenType::Update:
        return parse_update_statement(move(common_table_expression_list));
    case TokenType::Delete:
        return parse_delete_statement(move(common_table_expression_list));
    case TokenType::Select:
        return parse_select_statement(move(common_table_expression_list));
    default:
        expected("INSERT, UPDATE, DELETE, or SELECT"sv);
        return create_ast_node<ErrorStatement>();
    }
}

NonnullRefPtr<CreateSchema> Parser::parse_create_schema_statement()
{
    consume(TokenType::Schema);

    bool is_error_if_exists = true;
    if (consume_if(TokenType::If)) {
        consume(TokenType::Not);
        consume(TokenType::Exists);
        is_error_if_exists = false;
    }

    ByteString schema_name = consume(TokenType::Identifier).value();
    return create_ast_node<CreateSchema>(move(schema_name), is_error_if_exists);
}

NonnullRefPtr<CreateTable> Parser::parse_create_table_statement()
{
    // https://sqlite.org/lang_createtable.html

    bool is_temporary = false;
    if (consume_if(TokenType::Temp) || consume_if(TokenType::Temporary))
        is_temporary = true;

    consume(TokenType::Table);

    bool is_error_if_table_exists = true;
    if (consume_if(TokenType::If)) {
        consume(TokenType::Not);
        consume(TokenType::Exists);
        is_error_if_table_exists = false;
    }

    ByteString schema_name;
    ByteString table_name;
    parse_schema_and_table_name(schema_name, table_name);

    if (consume_if(TokenType::As)) {
        auto select_statement = parse_select_statement({});
        return create_ast_node<CreateTable>(move(schema_name), move(table_name), move(select_statement), is_temporary, is_error_if_table_exists);
    }

    Vector<NonnullRefPtr<ColumnDefinition>> column_definitions;
    parse_comma_separated_list(true, [&]() { column_definitions.append(parse_column_definition()); });

    // FIXME: Parse "table-constraint".

    return create_ast_node<CreateTable>(move(schema_name), move(table_name), move(column_definitions), is_temporary, is_error_if_table_exists);
}

NonnullRefPtr<AlterTable> Parser::parse_alter_table_statement()
{
    // https://sqlite.org/lang_altertable.html
    consume(TokenType::Alter);
    consume(TokenType::Table);

    ByteString schema_name;
    ByteString table_name;
    parse_schema_and_table_name(schema_name, table_name);

    if (consume_if(TokenType::Add)) {
        consume_if(TokenType::Column); // COLUMN is optional.
        auto column = parse_column_definition();
        return create_ast_node<AddColumn>(move(schema_name), move(table_name), move(column));
    }

    if (consume_if(TokenType::Drop)) {
        consume_if(TokenType::Column); // COLUMN is optional.
        auto column = consume(TokenType::Identifier).value();
        return create_ast_node<DropColumn>(move(schema_name), move(table_name), move(column));
    }

    consume(TokenType::Rename);

    if (consume_if(TokenType::To)) {
        auto new_table_name = consume(TokenType::Identifier).value();
        return create_ast_node<RenameTable>(move(schema_name), move(table_name), move(new_table_name));
    }

    consume_if(TokenType::Column); // COLUMN is optional.
    auto column_name = consume(TokenType::Identifier).value();
    consume(TokenType::To);
    auto new_column_name = consume(TokenType::Identifier).value();
    return create_ast_node<RenameColumn>(move(schema_name), move(table_name), move(column_name), move(new_column_name));
}

NonnullRefPtr<DropTable> Parser::parse_drop_table_statement()
{
    // https://sqlite.org/lang_droptable.html
    consume(TokenType::Drop);
    consume(TokenType::Table);

    bool is_error_if_table_does_not_exist = true;
    if (consume_if(TokenType::If)) {
        consume(TokenType::Exists);
        is_error_if_table_does_not_exist = false;
    }

    ByteString schema_name;
    ByteString table_name;
    parse_schema_and_table_name(schema_name, table_name);

    return create_ast_node<DropTable>(move(schema_name), move(table_name), is_error_if_table_does_not_exist);
}

NonnullRefPtr<DescribeTable> Parser::parse_describe_table_statement()
{
    consume(TokenType::Describe);
    consume(TokenType::Table);

    auto table_name = parse_qualified_table_name();

    return create_ast_node<DescribeTable>(move(table_name));
}

NonnullRefPtr<Insert> Parser::parse_insert_statement(RefPtr<CommonTableExpressionList> common_table_expression_list)
{
    // https://sqlite.org/lang_insert.html
    consume(TokenType::Insert);
    auto conflict_resolution = parse_conflict_resolution();
    consume(TokenType::Into);

    ByteString schema_name;
    ByteString table_name;
    parse_schema_and_table_name(schema_name, table_name);

    ByteString alias;
    if (consume_if(TokenType::As))
        alias = consume(TokenType::Identifier).value();

    Vector<ByteString> column_names;
    if (match(TokenType::ParenOpen))
        parse_comma_separated_list(true, [&]() { column_names.append(consume(TokenType::Identifier).value()); });

    Vector<NonnullRefPtr<ChainedExpression>> chained_expressions;
    RefPtr<Select> select_statement;

    if (consume_if(TokenType::Values)) {
        parse_comma_separated_list(false, [&]() {
            if (auto chained_expression = parse_chained_expression()) {
                auto* chained_expr = verify_cast<ChainedExpression>(chained_expression.ptr());
                if ((column_names.size() > 0) && (chained_expr->expressions().size() != column_names.size())) {
                    syntax_error("Number of expressions does not match number of columns");
                } else {
                    chained_expressions.append(static_ptr_cast<ChainedExpression>(chained_expression.release_nonnull()));
                }
            } else {
                expected("Chained expression"sv);
            }
        });
    } else if (match(TokenType::Select)) {
        select_statement = parse_select_statement({});
    } else {
        consume(TokenType::Default);
        consume(TokenType::Values);
    }

    RefPtr<ReturningClause> returning_clause;
    if (match(TokenType::Returning))
        returning_clause = parse_returning_clause();

    // FIXME: Parse 'upsert-clause'.

    if (!chained_expressions.is_empty())
        return create_ast_node<Insert>(move(common_table_expression_list), conflict_resolution, move(schema_name), move(table_name), move(alias), move(column_names), move(chained_expressions));
    if (!select_statement.is_null())
        return create_ast_node<Insert>(move(common_table_expression_list), conflict_resolution, move(schema_name), move(table_name), move(alias), move(column_names), move(select_statement));

    return create_ast_node<Insert>(move(common_table_expression_list), conflict_resolution, move(schema_name), move(table_name), move(alias), move(column_names));
}

NonnullRefPtr<Update> Parser::parse_update_statement(RefPtr<CommonTableExpressionList> common_table_expression_list)
{
    // https://sqlite.org/lang_update.html
    consume(TokenType::Update);
    auto conflict_resolution = parse_conflict_resolution();
    auto qualified_table_name = parse_qualified_table_name();
    consume(TokenType::Set);

    Vector<Update::UpdateColumns> update_columns;
    parse_comma_separated_list(false, [&]() {
        Vector<ByteString> column_names;
        if (match(TokenType::ParenOpen)) {
            parse_comma_separated_list(true, [&]() { column_names.append(consume(TokenType::Identifier).value()); });
        } else {
            column_names.append(consume(TokenType::Identifier).value());
        }

        consume(TokenType::Equals);
        update_columns.append({ move(column_names), parse_expression() });
    });

    Vector<NonnullRefPtr<TableOrSubquery>> table_or_subquery_list;
    if (consume_if(TokenType::From)) {
        // FIXME: Parse join-clause.
        parse_comma_separated_list(false, [&]() { table_or_subquery_list.append(parse_table_or_subquery()); });
    }

    RefPtr<Expression> where_clause;
    if (consume_if(TokenType::Where))
        where_clause = parse_expression();

    RefPtr<ReturningClause> returning_clause;
    if (match(TokenType::Returning))
        returning_clause = parse_returning_clause();

    return create_ast_node<Update>(move(common_table_expression_list), conflict_resolution, move(qualified_table_name), move(update_columns), move(table_or_subquery_list), move(where_clause), move(returning_clause));
}

NonnullRefPtr<Delete> Parser::parse_delete_statement(RefPtr<CommonTableExpressionList> common_table_expression_list)
{
    // https://sqlite.org/lang_delete.html
    consume(TokenType::Delete);
    consume(TokenType::From);
    auto qualified_table_name = parse_qualified_table_name();

    RefPtr<Expression> where_clause;
    if (consume_if(TokenType::Where))
        where_clause = parse_expression();

    RefPtr<ReturningClause> returning_clause;
    if (match(TokenType::Returning))
        returning_clause = parse_returning_clause();

    return create_ast_node<Delete>(move(common_table_expression_list), move(qualified_table_name), move(where_clause), move(returning_clause));
}

NonnullRefPtr<Select> Parser::parse_select_statement(RefPtr<CommonTableExpressionList> common_table_expression_list)
{
    // https://sqlite.org/lang_select.html
    consume(TokenType::Select);

    bool select_all = !consume_if(TokenType::Distinct);
    consume_if(TokenType::All); // ALL is the default, so ignore it if specified.

    Vector<NonnullRefPtr<ResultColumn>> result_column_list;
    parse_comma_separated_list(false, [&]() { result_column_list.append(parse_result_column()); });

    Vector<NonnullRefPtr<TableOrSubquery>> table_or_subquery_list;
    if (consume_if(TokenType::From)) {
        // FIXME: Parse join-clause.
        parse_comma_separated_list(false, [&]() { table_or_subquery_list.append(parse_table_or_subquery()); });
    }

    RefPtr<Expression> where_clause;
    if (consume_if(TokenType::Where))
        where_clause = parse_expression();

    RefPtr<GroupByClause> group_by_clause;
    if (consume_if(TokenType::Group)) {
        consume(TokenType::By);

        Vector<NonnullRefPtr<Expression>> group_by_list;
        parse_comma_separated_list(false, [&]() { group_by_list.append(parse_expression()); });

        if (!group_by_list.is_empty()) {
            RefPtr<Expression> having_clause;
            if (consume_if(TokenType::Having))
                having_clause = parse_expression();

            group_by_clause = create_ast_node<GroupByClause>(move(group_by_list), move(having_clause));
        }
    }

    // FIXME: Parse 'WINDOW window-name AS window-defn'.
    // FIXME: Parse 'compound-operator'.

    Vector<NonnullRefPtr<OrderingTerm>> ordering_term_list;
    if (consume_if(TokenType::Order)) {
        consume(TokenType::By);
        parse_comma_separated_list(false, [&]() { ordering_term_list.append(parse_ordering_term()); });
    }

    RefPtr<LimitClause> limit_clause;
    if (consume_if(TokenType::Limit)) {
        auto limit_expression = parse_expression();

        RefPtr<Expression> offset_expression;
        if (consume_if(TokenType::Offset)) {
            offset_expression = parse_expression();
        } else if (consume_if(TokenType::Comma)) {
            // Note: The limit clause may instead be defined as "offset-expression, limit-expression", effectively reversing the
            // order of the expressions. SQLite notes "this is counter-intuitive" and "to avoid confusion, programmers are strongly
            // encouraged to ... avoid using a LIMIT clause with a comma-separated offset."
            syntax_error("LIMIT clauses of the form 'LIMIT <expr>, <expr>' are not supported");
        }

        limit_clause = create_ast_node<LimitClause>(move(limit_expression), move(offset_expression));
    }

    return create_ast_node<Select>(move(common_table_expression_list), select_all, move(result_column_list), move(table_or_subquery_list), move(where_clause), move(group_by_clause), move(ordering_term_list), move(limit_clause));
}

RefPtr<CommonTableExpressionList> Parser::parse_common_table_expression_list()
{
    consume(TokenType::With);
    bool recursive = consume_if(TokenType::Recursive);

    Vector<NonnullRefPtr<CommonTableExpression>> common_table_expression;
    parse_comma_separated_list(false, [&]() { common_table_expression.append(parse_common_table_expression()); });

    if (common_table_expression.is_empty()) {
        expected("Common table expression list"sv);
        return {};
    }

    return create_ast_node<CommonTableExpressionList>(recursive, move(common_table_expression));
}

NonnullRefPtr<Expression> Parser::parse_expression()
{
    if (++m_parser_state.m_current_expression_depth > Limits::maximum_expression_tree_depth) {
        syntax_error(ByteString::formatted("Exceeded maximum expression tree depth of {}", Limits::maximum_expression_tree_depth));
        return create_ast_node<ErrorExpression>();
    }

    // https://sqlite.org/lang_expr.html
    auto expression = parse_primary_expression();

    if (match_secondary_expression())
        expression = parse_secondary_expression(move(expression));

    // FIXME: Parse 'function-name'.
    // FIXME: Parse 'raise-function'.

    --m_parser_state.m_current_expression_depth;
    return expression;
}

NonnullRefPtr<Expression> Parser::parse_primary_expression()
{
    if (auto expression = parse_literal_value_expression())
        return expression.release_nonnull();

    if (auto expression = parse_bind_parameter_expression())
        return expression.release_nonnull();

    if (auto expression = parse_column_name_expression())
        return expression.release_nonnull();

    if (auto expression = parse_unary_operator_expression())
        return expression.release_nonnull();

    if (auto expression = parse_cast_expression())
        return expression.release_nonnull();

    if (auto expression = parse_case_expression())
        return expression.release_nonnull();

    if (auto invert_expression = consume_if(TokenType::Not); invert_expression || consume_if(TokenType::Exists)) {
        if (auto expression = parse_exists_expression(invert_expression))
            return expression.release_nonnull();

        expected("Exists expression"sv);
    }

    if (consume_if(TokenType::ParenOpen)) {
        // Encountering a Select token at this point means this must be an ExistsExpression with no EXISTS keyword.
        if (match(TokenType::Select)) {
            auto select_statement = parse_select_statement({});
            consume(TokenType::ParenClose);
            return create_ast_node<ExistsExpression>(move(select_statement), false);
        }

        if (auto expression = parse_chained_expression(false)) {
            consume(TokenType::ParenClose);
            return expression.release_nonnull();
        }

        expected("Chained expression"sv);
    }

    expected("Primary Expression"sv);
    consume();

    return create_ast_node<ErrorExpression>();
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(NonnullRefPtr<Expression> primary)
{
    if (auto expression = parse_binary_operator_expression(primary))
        return expression.release_nonnull();

    if (auto expression = parse_collate_expression(primary))
        return expression.release_nonnull();

    if (auto expression = parse_is_expression(primary))
        return expression.release_nonnull();

    bool invert_expression = false;
    if (consume_if(TokenType::Not))
        invert_expression = true;

    if (auto expression = parse_match_expression(primary, invert_expression))
        return expression.release_nonnull();

    if (auto expression = parse_null_expression(primary, invert_expression))
        return expression.release_nonnull();

    if (auto expression = parse_between_expression(primary, invert_expression))
        return expression.release_nonnull();

    if (auto expression = parse_in_expression(primary, invert_expression))
        return expression.release_nonnull();

    expected("Secondary Expression"sv);
    consume();

    return create_ast_node<ErrorExpression>();
}

bool Parser::match_secondary_expression() const
{
    return match(TokenType::Not)
        || match(TokenType::DoublePipe)
        || match(TokenType::Asterisk)
        || match(TokenType::Divide)
        || match(TokenType::Modulus)
        || match(TokenType::Plus)
        || match(TokenType::Minus)
        || match(TokenType::ShiftLeft)
        || match(TokenType::ShiftRight)
        || match(TokenType::Ampersand)
        || match(TokenType::Pipe)
        || match(TokenType::LessThan)
        || match(TokenType::LessThanEquals)
        || match(TokenType::GreaterThan)
        || match(TokenType::GreaterThanEquals)
        || match(TokenType::Equals)
        || match(TokenType::EqualsEquals)
        || match(TokenType::NotEquals1)
        || match(TokenType::NotEquals2)
        || match(TokenType::And)
        || match(TokenType::Or)
        || match(TokenType::Collate)
        || match(TokenType::Is)
        || match(TokenType::Like)
        || match(TokenType::Glob)
        || match(TokenType::Match)
        || match(TokenType::Regexp)
        || match(TokenType::Isnull)
        || match(TokenType::Notnull)
        || match(TokenType::Between)
        || match(TokenType::In);
}

RefPtr<Expression> Parser::parse_literal_value_expression()
{
    if (match(TokenType::NumericLiteral)) {
        auto value = consume().double_value();
        return create_ast_node<NumericLiteral>(value);
    }
    if (match(TokenType::StringLiteral)) {
        // TODO: Should the surrounding ' ' be removed here?
        auto value = consume().value();
        return create_ast_node<StringLiteral>(value);
    }
    if (match(TokenType::BlobLiteral)) {
        // TODO: Should the surrounding x' ' be removed here?
        auto value = consume().value();
        return create_ast_node<BlobLiteral>(value);
    }
    if (consume_if(TokenType::True))
        return create_ast_node<BooleanLiteral>(true);
    if (consume_if(TokenType::False))
        return create_ast_node<BooleanLiteral>(false);
    if (consume_if(TokenType::Null))
        return create_ast_node<NullLiteral>();

    return {};
}

// https://sqlite.org/lang_expr.html#varparam
RefPtr<Expression> Parser::parse_bind_parameter_expression()
{
    // FIXME: Support ?NNN, :AAAA, @AAAA, and $AAAA forms.
    if (consume_if(TokenType::Placeholder)) {
        auto parameter = m_parser_state.m_bound_parameters;
        if (++m_parser_state.m_bound_parameters > Limits::maximum_bound_parameters)
            syntax_error(ByteString::formatted("Exceeded maximum number of bound parameters {}", Limits::maximum_bound_parameters));

        return create_ast_node<Placeholder>(parameter);
    }

    return {};
}

RefPtr<Expression> Parser::parse_column_name_expression(Optional<ByteString> with_parsed_identifier, bool with_parsed_period)
{
    if (!with_parsed_identifier.has_value() && !match(TokenType::Identifier))
        return {};

    ByteString first_identifier;
    if (!with_parsed_identifier.has_value())
        first_identifier = consume(TokenType::Identifier).value();
    else
        first_identifier = with_parsed_identifier.release_value();

    ByteString schema_name;
    ByteString table_name;
    ByteString column_name;

    if (with_parsed_period || consume_if(TokenType::Period)) {
        ByteString second_identifier = consume(TokenType::Identifier).value();

        if (consume_if(TokenType::Period)) {
            schema_name = move(first_identifier);
            table_name = move(second_identifier);
            column_name = consume(TokenType::Identifier).value();
        } else {
            table_name = move(first_identifier);
            column_name = move(second_identifier);
        }
    } else {
        column_name = move(first_identifier);
    }

    return create_ast_node<ColumnNameExpression>(move(schema_name), move(table_name), move(column_name));
}

RefPtr<Expression> Parser::parse_unary_operator_expression()
{
    if (consume_if(TokenType::Minus))
        return create_ast_node<UnaryOperatorExpression>(UnaryOperator::Minus, parse_expression());

    if (consume_if(TokenType::Plus))
        return create_ast_node<UnaryOperatorExpression>(UnaryOperator::Plus, parse_expression());

    if (consume_if(TokenType::Tilde))
        return create_ast_node<UnaryOperatorExpression>(UnaryOperator::BitwiseNot, parse_expression());

    if (consume_if(TokenType::Not)) {
        if (match(TokenType::Exists))
            return parse_exists_expression(true);
        else
            return create_ast_node<UnaryOperatorExpression>(UnaryOperator::Not, parse_expression());
    }

    return {};
}

RefPtr<Expression> Parser::parse_binary_operator_expression(NonnullRefPtr<Expression> lhs)
{
    if (consume_if(TokenType::DoublePipe))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Concatenate, move(lhs), parse_expression());

    if (consume_if(TokenType::Asterisk))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Multiplication, move(lhs), parse_expression());

    if (consume_if(TokenType::Divide))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Division, move(lhs), parse_expression());

    if (consume_if(TokenType::Modulus))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Modulo, move(lhs), parse_expression());

    if (consume_if(TokenType::Plus))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Plus, move(lhs), parse_expression());

    if (consume_if(TokenType::Minus))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Minus, move(lhs), parse_expression());

    if (consume_if(TokenType::ShiftLeft))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::ShiftLeft, move(lhs), parse_expression());

    if (consume_if(TokenType::ShiftRight))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::ShiftRight, move(lhs), parse_expression());

    if (consume_if(TokenType::Ampersand))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::BitwiseAnd, move(lhs), parse_expression());

    if (consume_if(TokenType::Pipe))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::BitwiseOr, move(lhs), parse_expression());

    if (consume_if(TokenType::LessThan))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::LessThan, move(lhs), parse_expression());

    if (consume_if(TokenType::LessThanEquals))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::LessThanEquals, move(lhs), parse_expression());

    if (consume_if(TokenType::GreaterThan))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::GreaterThan, move(lhs), parse_expression());

    if (consume_if(TokenType::GreaterThanEquals))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::GreaterThanEquals, move(lhs), parse_expression());

    if (consume_if(TokenType::Equals) || consume_if(TokenType::EqualsEquals))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Equals, move(lhs), parse_expression());

    if (consume_if(TokenType::NotEquals1) || consume_if(TokenType::NotEquals2))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::NotEquals, move(lhs), parse_expression());

    if (consume_if(TokenType::And))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::And, move(lhs), parse_expression());

    if (consume_if(TokenType::Or))
        return create_ast_node<BinaryOperatorExpression>(BinaryOperator::Or, move(lhs), parse_expression());

    return {};
}

RefPtr<Expression> Parser::parse_chained_expression(bool surrounded_by_parentheses)
{
    if (surrounded_by_parentheses && !consume_if(TokenType::ParenOpen))
        return {};

    Vector<NonnullRefPtr<Expression>> expressions;
    parse_comma_separated_list(false, [&]() { expressions.append(parse_expression()); });

    if (surrounded_by_parentheses)
        consume(TokenType::ParenClose);

    return create_ast_node<ChainedExpression>(move(expressions));
}

RefPtr<Expression> Parser::parse_cast_expression()
{
    if (!match(TokenType::Cast))
        return {};

    consume(TokenType::Cast);
    consume(TokenType::ParenOpen);
    auto expression = parse_expression();
    consume(TokenType::As);
    auto type_name = parse_type_name();
    consume(TokenType::ParenClose);

    return create_ast_node<CastExpression>(move(expression), move(type_name));
}

RefPtr<Expression> Parser::parse_case_expression()
{
    if (!match(TokenType::Case))
        return {};

    consume();

    RefPtr<Expression> case_expression;
    if (!match(TokenType::When)) {
        case_expression = parse_expression();
    }

    Vector<CaseExpression::WhenThenClause> when_then_clauses;

    do {
        consume(TokenType::When);
        auto when = parse_expression();
        consume(TokenType::Then);
        auto then = parse_expression();

        when_then_clauses.append({ move(when), move(then) });

        if (!match(TokenType::When))
            break;
    } while (!match(TokenType::Eof));

    RefPtr<Expression> else_expression;
    if (consume_if(TokenType::Else))
        else_expression = parse_expression();

    consume(TokenType::End);
    return create_ast_node<CaseExpression>(move(case_expression), move(when_then_clauses), move(else_expression));
}

RefPtr<Expression> Parser::parse_exists_expression(bool invert_expression)
{
    if (!(match(TokenType::Exists) || match(TokenType::ParenOpen)))
        return {};

    consume_if(TokenType::Exists);
    consume(TokenType::ParenOpen);

    auto select_statement = parse_select_statement({});
    consume(TokenType::ParenClose);

    return create_ast_node<ExistsExpression>(move(select_statement), invert_expression);
}

RefPtr<Expression> Parser::parse_collate_expression(NonnullRefPtr<Expression> expression)
{
    if (!match(TokenType::Collate))
        return {};

    consume();
    ByteString collation_name = consume(TokenType::Identifier).value();

    return create_ast_node<CollateExpression>(move(expression), move(collation_name));
}

RefPtr<Expression> Parser::parse_is_expression(NonnullRefPtr<Expression> expression)
{
    if (!match(TokenType::Is))
        return {};

    consume();

    bool invert_expression = false;
    if (match(TokenType::Not)) {
        consume();
        invert_expression = true;
    }

    auto rhs = parse_expression();
    return create_ast_node<IsExpression>(move(expression), move(rhs), invert_expression);
}

RefPtr<Expression> Parser::parse_match_expression(NonnullRefPtr<Expression> lhs, bool invert_expression)
{
    auto parse_escape = [this]() {
        RefPtr<Expression> escape;
        if (consume_if(TokenType::Escape)) {
            escape = parse_expression();
        }
        return escape;
    };

    if (consume_if(TokenType::Like)) {
        NonnullRefPtr<Expression> rhs = parse_expression();
        RefPtr<Expression> escape = parse_escape();
        return create_ast_node<MatchExpression>(MatchOperator::Like, move(lhs), move(rhs), move(escape), invert_expression);
    }

    if (consume_if(TokenType::Glob)) {
        NonnullRefPtr<Expression> rhs = parse_expression();
        RefPtr<Expression> escape = parse_escape();
        return create_ast_node<MatchExpression>(MatchOperator::Glob, move(lhs), move(rhs), move(escape), invert_expression);
    }

    if (consume_if(TokenType::Match)) {
        NonnullRefPtr<Expression> rhs = parse_expression();
        RefPtr<Expression> escape = parse_escape();
        return create_ast_node<MatchExpression>(MatchOperator::Match, move(lhs), move(rhs), move(escape), invert_expression);
    }

    if (consume_if(TokenType::Regexp)) {
        NonnullRefPtr<Expression> rhs = parse_expression();
        RefPtr<Expression> escape = parse_escape();
        return create_ast_node<MatchExpression>(MatchOperator::Regexp, move(lhs), move(rhs), move(escape), invert_expression);
    }

    return {};
}

RefPtr<Expression> Parser::parse_null_expression(NonnullRefPtr<Expression> expression, bool invert_expression)
{
    if (!match(TokenType::Isnull) && !match(TokenType::Notnull) && !(invert_expression && match(TokenType::Null)))
        return {};

    auto type = consume().type();
    invert_expression |= (type == TokenType::Notnull);

    return create_ast_node<NullExpression>(move(expression), invert_expression);
}

RefPtr<Expression> Parser::parse_between_expression(NonnullRefPtr<Expression> expression, bool invert_expression)
{
    if (!match(TokenType::Between))
        return {};

    consume();

    auto nested = parse_expression();
    if (!is<BinaryOperatorExpression>(*nested)) {
        expected("Binary Expression"sv);
        return create_ast_node<ErrorExpression>();
    }

    auto const& binary_expression = static_cast<BinaryOperatorExpression const&>(*nested);
    if (binary_expression.type() != BinaryOperator::And) {
        expected("AND Expression"sv);
        return create_ast_node<ErrorExpression>();
    }

    return create_ast_node<BetweenExpression>(move(expression), binary_expression.lhs(), binary_expression.rhs(), invert_expression);
}

RefPtr<Expression> Parser::parse_in_expression(NonnullRefPtr<Expression> expression, bool invert_expression)
{
    if (!match(TokenType::In))
        return {};

    consume();

    if (consume_if(TokenType::ParenOpen)) {
        if (match(TokenType::Select)) {
            auto select_statement = parse_select_statement({});
            return create_ast_node<InSelectionExpression>(move(expression), move(select_statement), invert_expression);
        }

        // FIXME: Consolidate this with parse_chained_expression(). That method consumes the opening paren as
        //        well, and also requires at least one expression (whereas this allows for an empty chain).
        Vector<NonnullRefPtr<Expression>> expressions;
        if (!match(TokenType::ParenClose))
            parse_comma_separated_list(false, [&]() { expressions.append(parse_expression()); });

        consume(TokenType::ParenClose);

        auto chain = create_ast_node<ChainedExpression>(move(expressions));
        return create_ast_node<InChainedExpression>(move(expression), move(chain), invert_expression);
    }

    ByteString schema_name;
    ByteString table_name;
    parse_schema_and_table_name(schema_name, table_name);

    if (match(TokenType::ParenOpen)) {
        // FIXME: Parse "table-function".
        return {};
    }

    return create_ast_node<InTableExpression>(move(expression), move(schema_name), move(table_name), invert_expression);
}

NonnullRefPtr<ColumnDefinition> Parser::parse_column_definition()
{
    // https://sqlite.org/syntax/column-def.html
    auto name = consume(TokenType::Identifier).value();

    auto type_name = match(TokenType::Identifier)
        ? parse_type_name()
        // https://www.sqlite.org/datatype3.html: If no type is specified then the column has affinity BLOB.
        : create_ast_node<TypeName>("BLOB", Vector<NonnullRefPtr<SignedNumber>> {});

    // FIXME: Parse "column-constraint".

    return create_ast_node<ColumnDefinition>(move(name), move(type_name));
}

NonnullRefPtr<TypeName> Parser::parse_type_name()
{
    // https: //sqlite.org/syntax/type-name.html
    auto name = consume(TokenType::Identifier).value();
    Vector<NonnullRefPtr<SignedNumber>> signed_numbers;

    if (consume_if(TokenType::ParenOpen)) {
        signed_numbers.append(parse_signed_number());

        if (consume_if(TokenType::Comma))
            signed_numbers.append(parse_signed_number());

        consume(TokenType::ParenClose);
    }

    return create_ast_node<TypeName>(move(name), move(signed_numbers));
}

NonnullRefPtr<SignedNumber> Parser::parse_signed_number()
{
    // https://sqlite.org/syntax/signed-number.html
    bool is_positive = true;

    if (consume_if(TokenType::Plus))
        is_positive = true;
    else if (consume_if(TokenType::Minus))
        is_positive = false;

    if (match(TokenType::NumericLiteral)) {
        auto number = consume(TokenType::NumericLiteral).double_value();
        return create_ast_node<SignedNumber>(is_positive ? number : (number * -1));
    }

    expected("NumericLiteral"sv);
    return create_ast_node<SignedNumber>(0);
}

NonnullRefPtr<CommonTableExpression> Parser::parse_common_table_expression()
{
    // https://sqlite.org/syntax/common-table-expression.html
    auto table_name = consume(TokenType::Identifier).value();

    Vector<ByteString> column_names;
    if (match(TokenType::ParenOpen))
        parse_comma_separated_list(true, [&]() { column_names.append(consume(TokenType::Identifier).value()); });

    consume(TokenType::As);
    consume(TokenType::ParenOpen);
    auto select_statement = parse_select_statement({});
    consume(TokenType::ParenClose);

    return create_ast_node<CommonTableExpression>(move(table_name), move(column_names), move(select_statement));
}

NonnullRefPtr<QualifiedTableName> Parser::parse_qualified_table_name()
{
    // https://sqlite.org/syntax/qualified-table-name.html
    ByteString schema_name;
    ByteString table_name;
    parse_schema_and_table_name(schema_name, table_name);

    ByteString alias;
    if (consume_if(TokenType::As))
        alias = consume(TokenType::Identifier).value();

    // Note: The qualified-table-name spec may include an "INDEXED BY index-name" or "NOT INDEXED" clause. This is a SQLite extension
    // "designed to help detect undesirable query plan changes during regression testing", and "application developers are admonished
    // to omit all use of INDEXED BY during application design, implementation, testing, and tuning". Our implementation purposefully
    // omits parsing INDEXED BY for now until there is good reason to add support.

    return create_ast_node<QualifiedTableName>(move(schema_name), move(table_name), move(alias));
}

NonnullRefPtr<ReturningClause> Parser::parse_returning_clause()
{
    // https://sqlite.org/syntax/returning-clause.html
    consume(TokenType::Returning);

    if (consume_if(TokenType::Asterisk))
        return create_ast_node<ReturningClause>();

    Vector<ReturningClause::ColumnClause> columns;
    parse_comma_separated_list(false, [&]() {
        auto expression = parse_expression();

        ByteString column_alias;
        if (consume_if(TokenType::As) || match(TokenType::Identifier))
            column_alias = consume(TokenType::Identifier).value();

        columns.append({ move(expression), move(column_alias) });
    });

    return create_ast_node<ReturningClause>(move(columns));
}

NonnullRefPtr<ResultColumn> Parser::parse_result_column()
{
    // https://sqlite.org/syntax/result-column.html
    if (consume_if(TokenType::Asterisk))
        return create_ast_node<ResultColumn>();

    // If we match an identifier now, we don't know whether it is a table-name of the form "table-name.*", or if it is the start of a
    // column-name-expression, until we try to parse the asterisk. So if we consume an identifier and a period, but don't find an
    // asterisk, hold onto that information to form a column-name-expression later.
    Optional<ByteString> table_name;
    bool parsed_period = false;

    if (match(TokenType::Identifier)) {
        table_name = consume().value();
        parsed_period = consume_if(TokenType::Period);
        if (parsed_period && consume_if(TokenType::Asterisk))
            return create_ast_node<ResultColumn>(table_name.release_value());
    }

    auto expression = !table_name.has_value()
        ? parse_expression()
        : static_cast<NonnullRefPtr<Expression>>(*parse_column_name_expression(move(table_name), parsed_period));

    ByteString column_alias;
    if (consume_if(TokenType::As) || match(TokenType::Identifier))
        column_alias = consume(TokenType::Identifier).value();

    return create_ast_node<ResultColumn>(move(expression), move(column_alias));
}

NonnullRefPtr<TableOrSubquery> Parser::parse_table_or_subquery()
{
    if (++m_parser_state.m_current_subquery_depth > Limits::maximum_subquery_depth)
        syntax_error(ByteString::formatted("Exceeded maximum subquery depth of {}", Limits::maximum_subquery_depth));

    ScopeGuard guard([&]() { --m_parser_state.m_current_subquery_depth; });

    // https://sqlite.org/syntax/table-or-subquery.html
    if (match(TokenType::Identifier)) {
        ByteString schema_name;
        ByteString table_name;
        parse_schema_and_table_name(schema_name, table_name);

        ByteString table_alias;
        if (consume_if(TokenType::As) || match(TokenType::Identifier))
            table_alias = consume(TokenType::Identifier).value();

        return create_ast_node<TableOrSubquery>(move(schema_name), move(table_name), move(table_alias));
    }

    // FIXME: Parse join-clause.

    Vector<NonnullRefPtr<TableOrSubquery>> subqueries;
    parse_comma_separated_list(true, [&]() { subqueries.append(parse_table_or_subquery()); });

    return create_ast_node<TableOrSubquery>(move(subqueries));
}

NonnullRefPtr<OrderingTerm> Parser::parse_ordering_term()
{
    // https://sqlite.org/syntax/ordering-term.html
    auto expression = parse_expression();

    ByteString collation_name;
    if (is<CollateExpression>(*expression)) {
        auto const& collate = static_cast<CollateExpression const&>(*expression);
        collation_name = collate.collation_name();
        expression = collate.expression();
    } else if (consume_if(TokenType::Collate)) {
        collation_name = consume(TokenType::Identifier).value();
    }

    Order order = consume_if(TokenType::Desc) ? Order::Descending : Order::Ascending;
    consume_if(TokenType::Asc); // ASC is the default, so ignore it if specified.

    Nulls nulls = order == Order::Ascending ? Nulls::First : Nulls::Last;
    if (consume_if(TokenType::Nulls)) {
        if (consume_if(TokenType::First))
            nulls = Nulls::First;
        else if (consume_if(TokenType::Last))
            nulls = Nulls::Last;
        else
            expected("FIRST or LAST"sv);
    }

    return create_ast_node<OrderingTerm>(move(expression), move(collation_name), order, nulls);
}

void Parser::parse_schema_and_table_name(ByteString& schema_name, ByteString& table_name)
{
    ByteString schema_or_table_name = consume(TokenType::Identifier).value();

    if (consume_if(TokenType::Period)) {
        schema_name = move(schema_or_table_name);
        table_name = consume(TokenType::Identifier).value();
    } else {
        table_name = move(schema_or_table_name);
    }
}

ConflictResolution Parser::parse_conflict_resolution()
{
    // https://sqlite.org/lang_conflict.html
    if (consume_if(TokenType::Or)) {
        if (consume_if(TokenType::Abort))
            return ConflictResolution::Abort;
        if (consume_if(TokenType::Fail))
            return ConflictResolution::Fail;
        if (consume_if(TokenType::Ignore))
            return ConflictResolution::Ignore;
        if (consume_if(TokenType::Replace))
            return ConflictResolution::Replace;
        if (consume_if(TokenType::Rollback))
            return ConflictResolution::Rollback;

        expected("ABORT, FAIL, IGNORE, REPLACE, or ROLLBACK"sv);
    }

    return ConflictResolution::Abort;
}

Token Parser::consume()
{
    auto old_token = m_parser_state.m_token;
    m_parser_state.m_token = m_parser_state.m_lexer.next();
    return old_token;
}

Token Parser::consume(TokenType expected_type)
{
    if (!match(expected_type)) {
        expected(Token::name(expected_type));
    }
    return consume();
}

bool Parser::consume_if(TokenType expected_type)
{
    if (!match(expected_type))
        return false;

    consume();
    return true;
}

bool Parser::match(TokenType type) const
{
    return m_parser_state.m_token.type() == type;
}

void Parser::expected(StringView what)
{
    syntax_error(ByteString::formatted("Unexpected token {}, expected {}", m_parser_state.m_token.name(), what));
}

void Parser::syntax_error(ByteString message)
{
    m_parser_state.m_errors.append({ move(message), position() });
}

SourcePosition Parser::position() const
{
    return m_parser_state.m_token.start_position();
}

Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_token(m_lexer.next())
{
}

}
