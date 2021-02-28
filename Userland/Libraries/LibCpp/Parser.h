/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#include "AK/NonnullRefPtr.h"
#include "AST.h"
#include <LibCpp/Lexer.h>

namespace Cpp {

class Parser final {
public:
    explicit Parser(const StringView& program, const String& filename);
    ~Parser() = default;

    NonnullRefPtr<TranslationUnit> parse();
    bool eof() const;

    RefPtr<ASTNode> eof_node() const;
    RefPtr<ASTNode> node_at(Position) const;
    Optional<Token> token_at(Position) const;
    RefPtr<const TranslationUnit> root_node() const { return m_root_node; }
    StringView text_of_node(const ASTNode&) const;
    StringView text_of_token(const Cpp::Token& token) const;
    void print_tokens() const;
    Vector<String> errors() const { return m_errors; }

private:
    enum class DeclarationType {
        Function,
        Variable,
        Enum,
        Struct,
    };

    bool done();

    Optional<DeclarationType> match_declaration();
    Optional<DeclarationType> match_declaration_in_translation_unit();
    Optional<DeclarationType> match_declaration_in_function_definition();
    bool match_function_declaration();
    bool match_comment();
    bool match_preprocessor();
    bool match_whitespace();
    bool match_variable_declaration();
    bool match_expression();
    bool match_function_call();
    bool match_secondary_expression();
    bool match_enum_declaration();
    bool match_struct_declaration();
    bool match_literal();
    bool match_unary_expression();
    bool match_boolean_literal();
    bool match_keyword(const String&);
    bool match_block_statement();

    Optional<NonnullRefPtrVector<Parameter>> parse_parameter_list(ASTNode& parent);
    Optional<Token> consume_whitespace();
    void consume_preprocessor();

    NonnullRefPtr<Declaration> parse_declaration(ASTNode& parent, DeclarationType);
    NonnullRefPtr<FunctionDeclaration> parse_function_declaration(ASTNode& parent);
    NonnullRefPtr<FunctionDefinition> parse_function_definition(ASTNode& parent);
    NonnullRefPtr<Statement> parse_statement(ASTNode& parent);
    NonnullRefPtr<VariableDeclaration> parse_variable_declaration(ASTNode& parent);
    NonnullRefPtr<Expression> parse_expression(ASTNode& parent);
    NonnullRefPtr<Expression> parse_primary_expression(ASTNode& parent);
    NonnullRefPtr<Expression> parse_secondary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs);
    NonnullRefPtr<FunctionCall> parse_function_call(ASTNode& parent);
    NonnullRefPtr<StringLiteral> parse_string_literal(ASTNode& parent);
    NonnullRefPtr<ReturnStatement> parse_return_statement(ASTNode& parent);
    NonnullRefPtr<EnumDeclaration> parse_enum_declaration(ASTNode& parent);
    NonnullRefPtr<StructOrClassDeclaration> parse_struct_or_class_declaration(ASTNode& parent, StructOrClassDeclaration::Type);
    NonnullRefPtr<MemberDeclaration> parse_member_declaration(ASTNode& parent);
    NonnullRefPtr<Expression> parse_literal(ASTNode& parent);
    NonnullRefPtr<UnaryExpression> parse_unary_expression(ASTNode& parent);
    NonnullRefPtr<BooleanLiteral> parse_boolean_literal(ASTNode& parent);
    NonnullRefPtr<Type> parse_type(ASTNode& parent);
    NonnullRefPtr<BinaryExpression> parse_binary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs, BinaryOp);
    NonnullRefPtr<AssignmentExpression> parse_assignment_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs, AssignmentOp);
    NonnullRefPtr<ForStatement> parse_for_statement(ASTNode& parent);
    NonnullRefPtr<BlockStatement> parse_block_statement(ASTNode& parent);
    NonnullRefPtr<Comment> parse_comment(ASTNode& parent);
    NonnullRefPtr<IfStatement> parse_if_statement(ASTNode& parent);

    bool match(Token::Type);
    Token consume(Token::Type);
    Token consume();
    Token consume_keyword(const String&);
    Token peek() const;
    Optional<Token> peek(Token::Type) const;
    Position position() const;
    StringView text_of_range(Position start, Position end) const;

    void save_state();
    void load_state();

    enum class Context {
        InTranslationUnit,
        InFunctionDefinition,
    };

    struct State {
        Context context { Context::InTranslationUnit };
        size_t token_index { 0 };
    };

    void error(StringView message = {});

    size_t node_span_size(const ASTNode& node) const;

    template<class T, class... Args>
    NonnullRefPtr<T>
    create_ast_node(ASTNode& parent, const Position& start, Optional<Position> end, Args&&... args)
    {
        auto node = adopt(*new T(&parent, start, end, m_filename, forward<Args>(args)...));
        m_nodes.append(node);
        return node;
    }

    NonnullRefPtr<TranslationUnit>
    create_root_ast_node(const Position& start, Position end)
    {
        auto node = adopt(*new TranslationUnit(nullptr, start, end, m_filename));
        m_nodes.append(node);
        m_root_node = node;
        return node;
    }

    StringView m_program;
    Vector<StringView> m_lines;
    String m_filename;
    Vector<Token> m_tokens;
    State m_state;
    Vector<State> m_saved_states;
    RefPtr<TranslationUnit> m_root_node;
    NonnullRefPtrVector<ASTNode> m_nodes;
    Vector<String> m_errors;
    Vector<StringView> parse_type_qualifiers();
};

}
