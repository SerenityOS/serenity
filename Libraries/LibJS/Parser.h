/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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

#include "AST.h"
#include "Lexer.h"
#include <AK/NonnullRefPtr.h>

namespace JS {

enum class Associativity {
    Left,
    Right
};

class Parser {
public:
    explicit Parser(Lexer lexer);

    NonnullRefPtr<Program> parse_program();

    template<typename FunctionNodeType>
    NonnullRefPtr<FunctionNodeType> parse_function_node();

    NonnullRefPtr<Statement> parse_statement();
    NonnullRefPtr<BlockStatement> parse_block_statement();
    NonnullRefPtr<ReturnStatement> parse_return_statement();
    NonnullRefPtr<VariableDeclaration> parse_variable_declaration();
    NonnullRefPtr<ForStatement> parse_for_statement();
    NonnullRefPtr<IfStatement> parse_if_statement();
    NonnullRefPtr<ThrowStatement> parse_throw_statement();
    NonnullRefPtr<TryStatement> parse_try_statement();
    NonnullRefPtr<CatchClause> parse_catch_clause();

    NonnullRefPtr<Expression> parse_expression(int min_precedence, Associativity associate = Associativity::Right);
    NonnullRefPtr<Expression> parse_primary_expression();
    NonnullRefPtr<Expression> parse_unary_prefixed_expression();
    NonnullRefPtr<ObjectExpression> parse_object_expression();
    NonnullRefPtr<ArrayExpression> parse_array_expression();
    NonnullRefPtr<Expression> parse_secondary_expression(NonnullRefPtr<Expression>, int min_precedence, Associativity associate = Associativity::Right);
    NonnullRefPtr<CallExpression> parse_call_expression(NonnullRefPtr<Expression>);

    bool has_errors() const { return m_has_errors; }

private:
    int operator_precedence(TokenType) const;
    Associativity operator_associativity(TokenType) const;
    bool match_expression() const;
    bool match_unary_prefixed_expression() const;
    bool match_secondary_expression() const;
    bool match_statement() const;
    bool match_variable_declaration() const;
    bool match(TokenType type) const;
    bool done() const;
    void expected(const char* what);
    Token consume();
    Token consume(TokenType type);

    Lexer m_lexer;
    Token m_current_token;
    bool m_has_errors = false;
};
}
