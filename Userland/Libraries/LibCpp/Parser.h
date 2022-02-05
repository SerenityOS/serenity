/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/NonnullRefPtr.h"
#include "AST.h"
#include "Preprocessor.h"
#include <AK/Noncopyable.h>
#include <LibCpp/Lexer.h>

namespace Cpp {

class Parser final {
    AK_MAKE_NONCOPYABLE(Parser);

public:
    explicit Parser(Vector<Token> tokens, String const& filename);
    ~Parser() = default;

    NonnullRefPtr<TranslationUnit> parse();
    bool eof() const;

    RefPtr<ASTNode> node_at(Position) const;
    Optional<size_t> index_of_node_at(Position) const;
    Optional<Token> token_at(Position) const;
    Optional<size_t> index_of_token_at(Position) const;
    RefPtr<const TranslationUnit> root_node() const { return m_root_node; }
    String text_of_node(const ASTNode&) const;
    StringView text_of_token(const Cpp::Token& token) const;
    void print_tokens() const;
    Vector<Token> const& tokens() const { return m_tokens; }
    const Vector<String>& errors() const { return m_errors; }

    struct TodoEntry {
        String content;
        String filename;
        size_t line { 0 };
        size_t column { 0 };
    };
    Vector<TodoEntry> get_todo_entries() const;

    Vector<Token> tokens_in_range(Position start, Position end) const;

private:
    enum class DeclarationType {
        Function,
        Variable,
        Enum,
        Class,
        Namespace,
        Constructor,
        Destructor,
    };

    Optional<DeclarationType> match_declaration_in_translation_unit();
    Optional<Parser::DeclarationType> match_class_member(StringView class_name);

    bool match_function_declaration();
    bool match_comment();
    bool match_preprocessor();
    bool match_whitespace();
    bool match_variable_declaration();
    bool match_expression();
    bool match_secondary_expression();
    bool match_enum_declaration();
    bool match_class_declaration();
    bool match_literal();
    bool match_unary_expression();
    bool match_boolean_literal();
    bool match_keyword(const String&);
    bool match_block_statement();
    bool match_namespace_declaration();
    bool match_template_arguments();
    bool match_name();
    bool match_cpp_cast_expression();
    bool match_c_style_cast_expression();
    bool match_sizeof_expression();
    bool match_braced_init_list();
    bool match_type();
    bool match_named_type();
    bool match_access_specifier();
    bool match_constructor(StringView class_name);
    bool match_destructor(StringView class_name);

    Optional<NonnullRefPtrVector<Parameter>> parse_parameter_list(ASTNode& parent);
    Optional<Token> consume_whitespace();
    void consume_preprocessor();

    NonnullRefPtr<Declaration> parse_declaration(ASTNode& parent, DeclarationType);
    NonnullRefPtr<FunctionDeclaration> parse_function_declaration(ASTNode& parent);
    NonnullRefPtr<FunctionDefinition> parse_function_definition(ASTNode& parent);
    NonnullRefPtr<Statement> parse_statement(ASTNode& parent);
    NonnullRefPtr<VariableDeclaration> parse_variable_declaration(ASTNode& parent, bool expect_semicolon = true);
    NonnullRefPtr<Expression> parse_expression(ASTNode& parent);
    NonnullRefPtr<Expression> parse_primary_expression(ASTNode& parent);
    NonnullRefPtr<Expression> parse_secondary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs);
    NonnullRefPtr<FunctionCall> parse_function_call(ASTNode& parent);
    NonnullRefPtr<StringLiteral> parse_string_literal(ASTNode& parent);
    NonnullRefPtr<ReturnStatement> parse_return_statement(ASTNode& parent);
    NonnullRefPtr<EnumDeclaration> parse_enum_declaration(ASTNode& parent);
    NonnullRefPtr<StructOrClassDeclaration> parse_class_declaration(ASTNode& parent);
    NonnullRefPtr<Expression> parse_literal(ASTNode& parent);
    NonnullRefPtr<UnaryExpression> parse_unary_expression(ASTNode& parent);
    NonnullRefPtr<BooleanLiteral> parse_boolean_literal(ASTNode& parent);
    NonnullRefPtr<Type> parse_type(ASTNode& parent);
    NonnullRefPtr<NamedType> parse_named_type(ASTNode& parent);
    NonnullRefPtr<BinaryExpression> parse_binary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs, BinaryOp);
    NonnullRefPtr<AssignmentExpression> parse_assignment_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs, AssignmentOp);
    NonnullRefPtr<ForStatement> parse_for_statement(ASTNode& parent);
    NonnullRefPtr<BlockStatement> parse_block_statement(ASTNode& parent);
    NonnullRefPtr<Comment> parse_comment(ASTNode& parent);
    NonnullRefPtr<IfStatement> parse_if_statement(ASTNode& parent);
    NonnullRefPtr<NamespaceDeclaration> parse_namespace_declaration(ASTNode& parent, bool is_nested_namespace = false);
    NonnullRefPtrVector<Declaration> parse_declarations_in_translation_unit(ASTNode& parent);
    RefPtr<Declaration> parse_single_declaration_in_translation_unit(ASTNode& parent);
    NonnullRefPtrVector<Type> parse_template_arguments(ASTNode& parent);
    NonnullRefPtr<Name> parse_name(ASTNode& parent);
    NonnullRefPtr<CppCastExpression> parse_cpp_cast_expression(ASTNode& parent);
    NonnullRefPtr<SizeofExpression> parse_sizeof_expression(ASTNode& parent);
    NonnullRefPtr<BracedInitList> parse_braced_init_list(ASTNode& parent);
    NonnullRefPtr<CStyleCastExpression> parse_c_style_cast_expression(ASTNode& parent);
    NonnullRefPtrVector<Declaration> parse_class_members(StructOrClassDeclaration& parent);
    NonnullRefPtr<Constructor> parse_constructor(ASTNode& parent);
    NonnullRefPtr<Destructor> parse_destructor(ASTNode& parent);

    bool match(Token::Type);
    Token consume(Token::Type);
    Token consume();
    Token consume_keyword(const String&);
    Token peek(size_t offset = 0) const;
    Optional<Token> peek(Token::Type) const;
    Position position() const;
    Position previous_token_end() const;
    String text_in_range(Position start, Position end) const;

    void save_state();
    void load_state();

    struct State {
        size_t token_index { 0 };
        NonnullRefPtrVector<ASTNode> state_nodes;
    };

    void error(StringView message = {});

    template<class T, class... Args>
    NonnullRefPtr<T>
    create_ast_node(ASTNode& parent, const Position& start, Optional<Position> end, Args&&... args)
    {
        auto node = adopt_ref(*new T(&parent, start, end, m_filename, forward<Args>(args)...));

        if (m_saved_states.is_empty()) {
            m_nodes.append(node);
        } else {
            m_state.state_nodes.append(node);
        }

        return node;
    }

    NonnullRefPtr<TranslationUnit>
    create_root_ast_node(const Position& start, Position end)
    {
        auto node = adopt_ref(*new TranslationUnit(nullptr, start, end, m_filename));
        m_nodes.append(node);
        m_root_node = node;
        return node;
    }

    DummyAstNode& get_dummy_node()
    {
        static NonnullRefPtr<DummyAstNode> dummy = adopt_ref(*new DummyAstNode(nullptr, {}, {}, {}));
        return dummy;
    }

    bool match_attribute_specification();
    void consume_attribute_specification();
    void consume_access_specifier();
    bool match_ellipsis();
    Vector<StringView> parse_type_qualifiers();
    Vector<StringView> parse_function_qualifiers();

    enum class CtorOrDtor {
        Ctor,
        Dtor,
    };
    void parse_constructor_or_destructor_impl(FunctionDeclaration&, CtorOrDtor);

    String m_filename;
    Vector<Token> m_tokens;
    State m_state;
    Vector<State> m_saved_states;
    RefPtr<TranslationUnit> m_root_node;
    Vector<String> m_errors;
    NonnullRefPtrVector<ASTNode> m_nodes;
};

}
