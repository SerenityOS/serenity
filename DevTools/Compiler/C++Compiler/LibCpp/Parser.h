/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
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
#include "Option.h"
#include <AK/ByteBuffer.h>

namespace Cpp {
class Parser {
public:
    static TranslationUnit parse(const Cpp::Option& options);

private:
    struct TypeSpecifier {
        bool is_void { false };
        bool is_int { false };
    };

    struct Declarator {
        String name;
        NonnullRefPtrVector<Variable> parameters;
    };

    explicit Parser(const String&);
    static ByteBuffer get_input_file_content(const String& filename);
    [[nodiscard]] Token get_next_token_skip_comment_and_whitespaces();
    [[nodiscard]] Token peek();
    Token consume(Token::Type expected_type);
    void consume();
    [[noreturn]] void parse_error(StringView message);

    TranslationUnit parse_translation_unit();
    Optional<String> parse_unqualified_id();
    Optional<String> parse_id_expression();
    Optional<String> parse_declarator_id();
    Optional<String> parse_noptr_declarator();
    Optional<Declarator> parse_declarator();
    NonnullRefPtr<Function> parse_function_definition();
    NonnullRefPtr<Function> parse_declaration();
    NonnullRefPtrVector<Function> parse_declaration_sequence();
    void expect(Token::Type expected_type);
    NonnullRefPtr<Type> parse_decl_specifier_seq();
    TypeSpecifier parse_decl_specifier();
    TypeSpecifier parse_defining_type_specifier();
    void parse_type_specifier(TypeSpecifier& type_specifier);
    void parse_simple_type_specifier(TypeSpecifier& type_specifier);
    NonnullRefPtr<Variable> parse_parameter_declaration();
    NonnullRefPtrVector<Variable> parse_parameter_declaration_list();
    NonnullRefPtrVector<Variable> parse_parameter_declaration_clause();
    NonnullRefPtrVector<Variable> parse_parameters_and_qualifiers();

    ByteBuffer m_file_content;
    Lexer m_lexer;
    Optional<Token> m_saved_token;
    TranslationUnit m_tu;
    NonnullRefPtrVector<ASTNode> parse_compound_statement();
    NonnullRefPtrVector<ASTNode> parse_function_body();
    NonnullRefPtr<Expression> parse_primary_expression();
    NonnullRefPtr<Expression> parse_postfix_expression();
    NonnullRefPtr<Expression> parse_unary_expression();
    NonnullRefPtr<Expression> parse_cast_expression();
    NonnullRefPtr<Expression> parse_pm_expression();
    NonnullRefPtr<Expression> parse_multiplicative_expression();
    NonnullRefPtr<Expression> parse_additive_expression();
    NonnullRefPtr<Expression> parse_shift_expression();
    NonnullRefPtr<Expression> parse_compare_expression();
    NonnullRefPtr<Expression> parse_relational_expression();
    NonnullRefPtr<Expression> parse_equality_expression();
    NonnullRefPtr<Expression> parse_and_expression();
    NonnullRefPtr<Expression> parse_exclusive_or_operation();
    NonnullRefPtr<Expression> parse_inclusive_or_expression();
    NonnullRefPtr<Expression> parse_logical_and_expression();
    NonnullRefPtr<Expression> parse_logical_or_expression();
    NonnullRefPtr<Expression> parse_assignment_expression();
    NonnullRefPtr<Expression> parse_expression();
    NonnullRefPtr<Expression> parse_expr_or_braced_init_list();
    NonnullRefPtr<Statement> parse_jump_statement();
    NonnullRefPtr<Statement> parse_statement();
    NonnullRefPtr<Statement> parse_statement_seq();
};
}