/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <LibCodeComprehension/Types.h>
#include <LibCpp/AST.h>
#include <LibCpp/Lexer.h>
#include <LibCpp/Preprocessor.h>

namespace Cpp {

class Parser final {
    AK_MAKE_NONCOPYABLE(Parser);

public:
    explicit Parser(Vector<Token> tokens, ByteString const& filename);
    ~Parser() = default;

    NonnullRefPtr<TranslationUnit> parse();
    bool eof() const;

    RefPtr<ASTNode const> node_at(Position) const;
    Optional<size_t> index_of_node_at(Position) const;
    Optional<Token> token_at(Position) const;
    Optional<size_t> index_of_token_at(Position) const;
    RefPtr<TranslationUnit const> root_node() const { return m_root_node; }
    ByteString text_of_node(ASTNode const&) const;
    StringView text_of_token(Cpp::Token const& token) const;
    void print_tokens() const;
    Vector<Token> const& tokens() const { return m_tokens; }
    Vector<ByteString> const& errors() const { return m_errors; }

    Vector<CodeComprehension::TodoEntry> get_todo_entries() const;

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
        UsingNamespace,
        Typedef,
        UsingType,
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
    bool match_keyword(ByteString const&);
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
    bool match_using_namespace_declaration();
    bool match_typedef_declaration();
    bool match_using_type_declaration();

    Optional<Vector<NonnullRefPtr<Parameter const>>> parse_parameter_list(ASTNode const& parent);
    Optional<Token> consume_whitespace();
    void consume_preprocessor();

    NonnullRefPtr<Declaration const> parse_declaration(ASTNode const& parent, DeclarationType);
    NonnullRefPtr<FunctionDeclaration const> parse_function_declaration(ASTNode const& parent);
    NonnullRefPtr<FunctionDefinition const> parse_function_definition(ASTNode const& parent);
    NonnullRefPtr<Statement const> parse_statement(ASTNode const& parent);
    NonnullRefPtr<VariableDeclaration const> parse_variable_declaration(ASTNode const& parent, bool expect_semicolon = true);
    NonnullRefPtr<Expression const> parse_expression(ASTNode const& parent);
    NonnullRefPtr<Expression const> parse_primary_expression(ASTNode const& parent);
    NonnullRefPtr<Expression const> parse_secondary_expression(ASTNode const& parent, NonnullRefPtr<Expression const> lhs);
    NonnullRefPtr<StringLiteral const> parse_string_literal(ASTNode const& parent);
    NonnullRefPtr<ReturnStatement const> parse_return_statement(ASTNode const& parent);
    NonnullRefPtr<EnumDeclaration const> parse_enum_declaration(ASTNode const& parent);
    NonnullRefPtr<StructOrClassDeclaration const> parse_class_declaration(ASTNode const& parent);
    NonnullRefPtr<Expression const> parse_literal(ASTNode const& parent);
    NonnullRefPtr<UnaryExpression const> parse_unary_expression(ASTNode const& parent);
    NonnullRefPtr<BooleanLiteral const> parse_boolean_literal(ASTNode const& parent);
    NonnullRefPtr<Type const> parse_type(ASTNode const& parent);
    NonnullRefPtr<BinaryExpression const> parse_binary_expression(ASTNode const& parent, NonnullRefPtr<Expression const> lhs, BinaryOp);
    NonnullRefPtr<AssignmentExpression const> parse_assignment_expression(ASTNode const& parent, NonnullRefPtr<Expression const> lhs, AssignmentOp);
    NonnullRefPtr<ForStatement const> parse_for_statement(ASTNode const& parent);
    NonnullRefPtr<BlockStatement const> parse_block_statement(ASTNode const& parent);
    NonnullRefPtr<Comment const> parse_comment(ASTNode const& parent);
    NonnullRefPtr<IfStatement const> parse_if_statement(ASTNode const& parent);
    NonnullRefPtr<NamespaceDeclaration const> parse_namespace_declaration(ASTNode const& parent, bool is_nested_namespace = false);
    Vector<NonnullRefPtr<Declaration const>> parse_declarations_in_translation_unit(ASTNode const& parent);
    RefPtr<Declaration const> parse_single_declaration_in_translation_unit(ASTNode const& parent);
    Vector<NonnullRefPtr<Type const>> parse_template_arguments(ASTNode const& parent);
    NonnullRefPtr<Name const> parse_name(ASTNode const& parent);
    NonnullRefPtr<CppCastExpression const> parse_cpp_cast_expression(ASTNode const& parent);
    NonnullRefPtr<SizeofExpression const> parse_sizeof_expression(ASTNode const& parent);
    NonnullRefPtr<BracedInitList const> parse_braced_init_list(ASTNode const& parent);
    NonnullRefPtr<CStyleCastExpression const> parse_c_style_cast_expression(ASTNode const& parent);
    Vector<NonnullRefPtr<Declaration const>> parse_class_members(StructOrClassDeclaration& parent);
    NonnullRefPtr<Constructor const> parse_constructor(ASTNode const& parent);
    NonnullRefPtr<Destructor const> parse_destructor(ASTNode const& parent);
    NonnullRefPtr<UsingNamespaceDeclaration const> parse_using_namespace_declaration(ASTNode const& parent);
    NonnullRefPtr<TypedefDeclaration const> parse_typedef_declaration(ASTNode const& parent);
    NonnullRefPtr<TypedefDeclaration const> parse_using_type_declaration(ASTNode const& parent);

    bool match(Token::Type);
    Token consume(Token::Type);
    Token consume();
    Token consume_keyword(ByteString const&);
    Token peek(size_t offset = 0) const;
    Optional<Token> peek(Token::Type) const;
    Position position() const;
    Position previous_token_end() const;
    ByteString text_in_range(Position start, Position end) const;

    void save_state();
    void load_state();

    struct State {
        size_t token_index { 0 };
        Vector<NonnullRefPtr<ASTNode>> state_nodes;
    };

    void error(StringView message = {});

    template<class T, class... Args>
    NonnullRefPtr<T>
    create_ast_node(ASTNode const& parent, Position const& start, Optional<Position> end, Args&&... args)
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
    create_root_ast_node(Position const& start, Position end)
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

    ByteString m_filename;
    Vector<Token> m_tokens;
    State m_state;
    Vector<State> m_saved_states;
    RefPtr<TranslationUnit> m_root_node;
    Vector<ByteString> m_errors;
    Vector<NonnullRefPtr<ASTNode>> m_nodes;
};

}
