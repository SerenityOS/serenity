/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Volodymyr V. <vvmposeydon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <LibCodeComprehension/Types.h>
#include <LibGLSL/AST.h>
#include <LibGLSL/Lexer.h>
#include <LibGLSL/Preprocessor.h>

namespace GLSL {

class Parser final {
    AK_MAKE_NONCOPYABLE(Parser);

public:
    explicit Parser(Vector<Token> tokens, String const& filename);
    ~Parser() = default;

    ErrorOr<NonnullRefPtr<TranslationUnit>> parse();
    bool eof() const;

    RefPtr<TranslationUnit const> root_node() const { return m_root_node; }
    void print_tokens() const;
    Vector<Token> const& tokens() const { return m_tokens; }
    Vector<String> const& errors() const { return m_errors; }

private:
    enum class DeclarationType {
        Function,
        Variable,
        Struct,
    };
    ErrorOr<Optional<DeclarationType>> match_declaration_in_translation_unit();

    ErrorOr<bool> match_struct_declaration();
    ErrorOr<bool> match_function_declaration();
    ErrorOr<bool> match_variable_declaration();

    ErrorOr<bool> match_block_statement();

    ErrorOr<bool> match_expression();
    ErrorOr<bool> match_name();
    ErrorOr<bool> match_string_literal();
    ErrorOr<bool> match_numeric_literal();
    ErrorOr<bool> match_boolean_literal();

    ErrorOr<bool> match_type();

    ErrorOr<Vector<NonnullRefPtr<Declaration const>>> parse_declarations_in_translation_unit(ASTNode const& parent);
    ErrorOr<RefPtr<Declaration const>> parse_single_declaration_in_translation_unit(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<Declaration const>> parse_declaration(ASTNode const& parent, DeclarationType);
    ErrorOr<NonnullRefPtr<StructDeclaration const>> parse_struct_declaration(ASTNode const& parent);
    ErrorOr<Vector<NonnullRefPtr<Declaration const>>> parse_struct_members(StructDeclaration& parent);
    ErrorOr<NonnullRefPtr<FunctionDeclaration const>> parse_function_declaration(ASTNode const& parent);
    ErrorOr<Vector<NonnullRefPtr<Parameter const>>> parse_parameter_list(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<FunctionDefinition const>> parse_function_definition(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<VariableDeclaration const>> parse_variable_declaration(ASTNode const& parent, bool expect_semicolon = true);

    ErrorOr<NonnullRefPtr<Statement const>> parse_statement(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<BlockStatement const>> parse_block_statement(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<IfStatement const>> parse_if_statement(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<ForStatement const>> parse_for_statement(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<ReturnStatement const>> parse_return_statement(ASTNode const& parent);

    enum class Associativity {
        LeftToRight,
        RightToLeft
    };
    static HashMap<BinaryOp, Associativity> s_operator_associativity;

    ErrorOr<NonnullRefPtr<Expression const>> parse_expression(ASTNode const& parent, int min_precedence = 0, Associativity associativity = Associativity::LeftToRight);
    ErrorOr<NonnullRefPtr<Expression const>> parse_unary_expression(ASTNode const& parent);
    ErrorOr<Vector<NonnullRefPtr<Expression const>>> parse_function_call_args(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<Expression const>> parse_boolean_literal(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<Expression const>> parse_numeric_literal(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<Expression const>> parse_string_literal(ASTNode const& parent);
    ErrorOr<NonnullRefPtr<Name const>> parse_name(ASTNode const& parent, bool allow_sized_name = false);

    ErrorOr<NonnullRefPtr<Type const>> parse_type(ASTNode const& parent);

    bool match_unary_op();
    ErrorOr<UnaryOp> consume_unary_op();
    bool match_binary_op();
    ErrorOr<BinaryOp> peek_binary_op();
    bool match_storage_qualifier();
    ErrorOr<StorageTypeQualifier> consume_storage_qualifier();

    Token peek(size_t offset = 0) const;
    Optional<Token> peek(Token::Type) const;

    bool match(Token::Type);
    bool match_keyword(StringView);
    bool match_preprocessor();

    ErrorOr<Token> consume();
    ErrorOr<Token> consume(Token::Type);
    ErrorOr<Token> consume_keyword(StringView);
    ErrorOr<void> consume_preprocessor();

    Position position() const;
    Position previous_token_end() const;
    Optional<size_t> index_of_token_at(Position pos) const;
    Vector<Token> tokens_in_range(Position start, Position end) const;
    ErrorOr<String> text_in_range(Position start, Position end) const;

    ErrorOr<void> error(StringView message = {});

    template<class T, class... Args>
    NonnullRefPtr<T>
    create_ast_node(ASTNode const& parent, Position const& start, Optional<Position> end, Args&&... args)
    {
        auto node = adopt_ref(*new T(&parent, start, end, m_filename, forward<Args>(args)...));
        return node;
    }

    NonnullRefPtr<TranslationUnit>
    create_root_ast_node(Position const& start, Position end)
    {
        auto node = adopt_ref(*new TranslationUnit(nullptr, start, end, m_filename));
        m_root_node = node;
        return node;
    }

    DummyAstNode& get_dummy_node()
    {
        static NonnullRefPtr<DummyAstNode> dummy = adopt_ref(*new DummyAstNode(nullptr, {}, {}, {}));
        return dummy;
    }

    struct State {
        size_t token_index { 0 };
    };

    void save_state();
    void load_state();

    State m_state;
    Vector<State> m_saved_states;

    String m_filename;
    Vector<Token> m_tokens;
    RefPtr<TranslationUnit> m_root_node;
    Vector<String> m_errors;
};

}
