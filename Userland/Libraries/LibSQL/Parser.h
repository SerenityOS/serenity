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

    NonnullRefPtr<CreateTable> parse_create_table_statement();
    NonnullRefPtr<DropTable> parse_drop_table_statement();

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
