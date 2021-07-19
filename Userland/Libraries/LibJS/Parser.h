/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashTable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Lexer.h>
#include <LibJS/SourceRange.h>
#include <stdio.h>

namespace JS {

enum class Associativity {
    Left,
    Right
};

struct FunctionNodeParseOptions {
    enum {
        CheckForFunctionAndName = 1 << 0,
        AllowSuperPropertyLookup = 1 << 1,
        AllowSuperConstructorCall = 1 << 2,
        IsGetterFunction = 1 << 3,
        IsSetterFunction = 1 << 4,
        IsArrowFunction = 1 << 5,
        IsGeneratorFunction = 1 << 6,
    };
};

class Parser {
public:
    explicit Parser(Lexer lexer);

    NonnullRefPtr<Program> parse_program(bool starts_in_strict_mode = false);

    template<typename FunctionNodeType>
    NonnullRefPtr<FunctionNodeType> parse_function_node(u8 parse_options = FunctionNodeParseOptions::CheckForFunctionAndName);
    Vector<FunctionNode::Parameter> parse_formal_parameters(int& function_length, u8 parse_options = 0);
    RefPtr<BindingPattern> parse_binding_pattern();

    struct PrimaryExpressionParseResult {
        NonnullRefPtr<Expression> result;
        bool should_continue_parsing_as_expression { true };
    };

    NonnullRefPtr<Declaration> parse_declaration();
    NonnullRefPtr<Statement> parse_statement();
    NonnullRefPtr<BlockStatement> parse_block_statement();
    NonnullRefPtr<BlockStatement> parse_block_statement(bool& is_strict, bool error_on_binding = false);
    NonnullRefPtr<ReturnStatement> parse_return_statement();
    NonnullRefPtr<VariableDeclaration> parse_variable_declaration(bool for_loop_variable_declaration = false);
    NonnullRefPtr<Statement> parse_for_statement();
    NonnullRefPtr<Statement> parse_for_in_of_statement(NonnullRefPtr<ASTNode> lhs);
    NonnullRefPtr<IfStatement> parse_if_statement();
    NonnullRefPtr<ThrowStatement> parse_throw_statement();
    NonnullRefPtr<TryStatement> parse_try_statement();
    NonnullRefPtr<CatchClause> parse_catch_clause();
    NonnullRefPtr<SwitchStatement> parse_switch_statement();
    NonnullRefPtr<SwitchCase> parse_switch_case();
    NonnullRefPtr<BreakStatement> parse_break_statement();
    NonnullRefPtr<ContinueStatement> parse_continue_statement();
    NonnullRefPtr<DoWhileStatement> parse_do_while_statement();
    NonnullRefPtr<WhileStatement> parse_while_statement();
    NonnullRefPtr<WithStatement> parse_with_statement();
    NonnullRefPtr<DebuggerStatement> parse_debugger_statement();
    NonnullRefPtr<ConditionalExpression> parse_conditional_expression(NonnullRefPtr<Expression> test);
    NonnullRefPtr<Expression> parse_expression(int min_precedence, Associativity associate = Associativity::Right, const Vector<TokenType>& forbidden = {});
    PrimaryExpressionParseResult parse_primary_expression();
    NonnullRefPtr<Expression> parse_unary_prefixed_expression();
    NonnullRefPtr<RegExpLiteral> parse_regexp_literal();
    NonnullRefPtr<ObjectExpression> parse_object_expression();
    NonnullRefPtr<ArrayExpression> parse_array_expression();
    NonnullRefPtr<StringLiteral> parse_string_literal(const Token& token, bool in_template_literal = false);
    NonnullRefPtr<TemplateLiteral> parse_template_literal(bool is_tagged);
    NonnullRefPtr<Expression> parse_secondary_expression(NonnullRefPtr<Expression>, int min_precedence, Associativity associate = Associativity::Right);
    NonnullRefPtr<CallExpression> parse_call_expression(NonnullRefPtr<Expression>);
    NonnullRefPtr<NewExpression> parse_new_expression();
    NonnullRefPtr<ClassDeclaration> parse_class_declaration();
    NonnullRefPtr<ClassExpression> parse_class_expression(bool expect_class_name);
    NonnullRefPtr<YieldExpression> parse_yield_expression();
    NonnullRefPtr<Expression> parse_property_key();
    NonnullRefPtr<AssignmentExpression> parse_assignment_expression(AssignmentOp, NonnullRefPtr<Expression> lhs, int min_precedence, Associativity);
    NonnullRefPtr<Identifier> parse_identifier();

    RefPtr<FunctionExpression> try_parse_arrow_function_expression(bool expect_parens);
    RefPtr<Statement> try_parse_labelled_statement();
    RefPtr<MetaProperty> try_parse_new_target_expression();

    struct Error {
        String message;
        Optional<Position> position;

        String to_string() const
        {
            if (!position.has_value())
                return message;
            return String::formatted("{} (line: {}, column: {})", message, position.value().line, position.value().column);
        }

        String source_location_hint(const StringView& source, const char spacer = ' ', const char indicator = '^') const
        {
            if (!position.has_value())
                return {};
            // We need to modify the source to match what the lexer considers one line - normalizing
            // line terminators to \n is easier than splitting using all different LT characters.
            String source_string { source };
            source_string.replace("\r\n", "\n");
            source_string.replace("\r", "\n");
            source_string.replace(LINE_SEPARATOR, "\n");
            source_string.replace(PARAGRAPH_SEPARATOR, "\n");
            StringBuilder builder;
            builder.append(source_string.split_view('\n', true)[position.value().line - 1]);
            builder.append('\n');
            for (size_t i = 0; i < position.value().column - 1; ++i)
                builder.append(spacer);
            builder.append(indicator);
            return builder.build();
        }
    };

    bool has_errors() const { return m_state.errors.size(); }
    const Vector<Error>& errors() const { return m_state.errors; }
    void print_errors() const
    {
        for (auto& error : m_state.errors) {
            auto hint = error.source_location_hint(m_state.lexer.source());
            if (!hint.is_empty())
                warnln("{}", hint);
            warnln("SyntaxError: {}", error.to_string());
        }
    }

    struct TokenMemoization {
        bool try_parse_arrow_function_expression_failed;
    };

private:
    friend class ScopePusher;

    Associativity operator_associativity(TokenType) const;
    bool match_expression() const;
    bool match_unary_prefixed_expression() const;
    bool match_secondary_expression(const Vector<TokenType>& forbidden = {}) const;
    bool match_statement() const;
    bool match_declaration() const;
    bool match_variable_declaration() const;
    bool match_identifier() const;
    bool match_identifier_name() const;
    bool match_property_key() const;
    bool match(TokenType type) const;
    bool done() const;
    void expected(const char* what);
    void syntax_error(const String& message, Optional<Position> = {});
    Token consume();
    Token consume_identifier();
    Token consume_identifier_reference();
    Token consume(TokenType type);
    Token consume_and_validate_numeric_literal();
    void consume_or_insert_semicolon();
    void save_state();
    void load_state();
    void discard_saved_state();
    Position position() const;

    void check_identifier_name_for_assignment_validity(StringView, bool force_strict = false);

    bool try_parse_arrow_function_expression_failed_at_position(const Position&) const;
    void set_try_parse_arrow_function_expression_failed_at_position(const Position&, bool);

    struct RulePosition {
        AK_MAKE_NONCOPYABLE(RulePosition);
        AK_MAKE_NONMOVABLE(RulePosition);

    public:
        RulePosition(Parser& parser, Position position)
            : m_parser(parser)
            , m_position(position)
        {
            m_parser.m_rule_starts.append(position);
        }

        ~RulePosition()
        {
            auto last = m_parser.m_rule_starts.take_last();
            VERIFY(last.line == m_position.line);
            VERIFY(last.column == m_position.column);
        }

        const Position& position() const { return m_position; }

    private:
        Parser& m_parser;
        Position m_position;
    };

    [[nodiscard]] RulePosition push_start() { return { *this, position() }; }

    struct Scope : public RefCounted<Scope> {
        enum Type {
            Function,
            Block,
        };
        struct HoistableDeclaration {
            NonnullRefPtr<FunctionDeclaration> declaration;
            NonnullRefPtr<Scope> scope; // where it is actually declared
        };

        Type type;
        RefPtr<Scope> parent;

        NonnullRefPtrVector<FunctionDeclaration> function_declarations;
        Vector<HoistableDeclaration> hoisted_function_declarations;

        HashTable<FlyString> lexical_declarations;

        explicit Scope(Type, RefPtr<Scope>);
        RefPtr<Scope> get_current_function_scope();
    };

    struct ParserState {
        Lexer lexer;
        Token current_token;
        Vector<Error> errors;
        Vector<NonnullRefPtrVector<VariableDeclaration>> var_scopes;
        Vector<NonnullRefPtrVector<VariableDeclaration>> let_scopes;
        RefPtr<Scope> current_scope;

        Vector<Vector<FunctionNode::Parameter>&> function_parameters;

        HashTable<StringView> labels_in_scope;
        bool strict_mode { false };
        bool allow_super_property_lookup { false };
        bool allow_super_constructor_call { false };
        bool in_function_context { false };
        bool in_generator_function_context { false };
        bool in_arrow_function_context { false };
        bool in_break_context { false };
        bool in_continue_context { false };
        bool string_legacy_octal_escape_sequence_in_scope { false };

        explicit ParserState(Lexer);
    };

    class PositionKeyTraits {
    public:
        static int hash(const Position& position)
        {
            return int_hash(position.line) ^ int_hash(position.column);
        }

        static bool equals(const Position& a, const Position& b)
        {
            return a.column == b.column && a.line == b.line;
        }
    };

    Vector<Position> m_rule_starts;
    ParserState m_state;
    FlyString m_filename;
    Vector<ParserState> m_saved_state;
    HashMap<Position, TokenMemoization, PositionKeyTraits> m_token_memoizations;
};
}
