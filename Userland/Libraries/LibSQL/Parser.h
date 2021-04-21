/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibSQL/AST.h>
#include <LibSQL/Lexer.h>
#include <LibSQL/Token.h>

namespace SQL {

class Parser {
    struct Position {
        size_t line { 0 };
        size_t column { 0 };
    };

    struct Error {
        String message;
        Position position;

        String to_string() const
        {
            return String::formatted("{} (line: {}, column: {})", message, position.line, position.column);
        }
    };

public:
    explicit Parser(Lexer lexer);

    NonnullRefPtr<Statement> next_statement();

    bool has_errors() const { return m_parser_state.m_errors.size(); }
    const Vector<Error>& errors() const { return m_parser_state.m_errors; }

protected:
    NonnullRefPtr<Expression> parse_expression(); // Protected for unit testing.

private:
    struct ParserState {
        explicit ParserState(Lexer);

        Lexer m_lexer;
        Token m_token;
        Vector<Error> m_errors;
    };

    NonnullRefPtr<Statement> parse_statement();
    NonnullRefPtr<Statement> parse_statement_with_expression_list(RefPtr<CommonTableExpressionList>);
    NonnullRefPtr<CreateTable> parse_create_table_statement();
    NonnullRefPtr<DropTable> parse_drop_table_statement();
    NonnullRefPtr<Delete> parse_delete_statement(RefPtr<CommonTableExpressionList>);
    NonnullRefPtr<CommonTableExpressionList> parse_common_table_expression_list();

    NonnullRefPtr<Expression> parse_primary_expression();
    NonnullRefPtr<Expression> parse_secondary_expression(NonnullRefPtr<Expression> primary);
    bool match_secondary_expression() const;
    Optional<NonnullRefPtr<Expression>> parse_literal_value_expression();
    Optional<NonnullRefPtr<Expression>> parse_column_name_expression();
    Optional<NonnullRefPtr<Expression>> parse_unary_operator_expression();
    Optional<NonnullRefPtr<Expression>> parse_binary_operator_expression(NonnullRefPtr<Expression> lhs);
    Optional<NonnullRefPtr<Expression>> parse_chained_expression();
    Optional<NonnullRefPtr<Expression>> parse_cast_expression();
    Optional<NonnullRefPtr<Expression>> parse_case_expression();
    Optional<NonnullRefPtr<Expression>> parse_collate_expression(NonnullRefPtr<Expression> expression);
    Optional<NonnullRefPtr<Expression>> parse_is_expression(NonnullRefPtr<Expression> expression);
    Optional<NonnullRefPtr<Expression>> parse_match_expression(NonnullRefPtr<Expression> lhs, bool invert_expression);
    Optional<NonnullRefPtr<Expression>> parse_null_expression(NonnullRefPtr<Expression> expression, bool invert_expression);
    Optional<NonnullRefPtr<Expression>> parse_between_expression(NonnullRefPtr<Expression> expression, bool invert_expression);
    Optional<NonnullRefPtr<Expression>> parse_in_expression(NonnullRefPtr<Expression> expression, bool invert_expression);

    NonnullRefPtr<ColumnDefinition> parse_column_definition();
    NonnullRefPtr<TypeName> parse_type_name();
    NonnullRefPtr<SignedNumber> parse_signed_number();
    NonnullRefPtr<CommonTableExpression> parse_common_table_expression();
    NonnullRefPtr<QualifiedTableName> parse_qualified_table_name();
    NonnullRefPtr<ReturningClause> parse_returning_clause();

    Token consume();
    Token consume(TokenType type);
    bool consume_if(TokenType type);
    bool match(TokenType type) const;

    void expected(StringView what);
    void syntax_error(String message);

    Position position() const;

    ParserState m_parser_state;
};

}
