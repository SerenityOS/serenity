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
    };
};

class Parser {
public:
    explicit Parser(Lexer lexer);

    NonnullRefPtr<Program> parse_program();

    template<typename FunctionNodeType>
    NonnullRefPtr<FunctionNodeType> parse_function_node(u8 parse_options = FunctionNodeParseOptions::CheckForFunctionAndName);
    Vector<FunctionNode::Parameter> parse_function_parameters(int& function_length, u8 parse_options = 0);

    NonnullRefPtr<Declaration> parse_declaration();
    NonnullRefPtr<Statement> parse_statement();
    NonnullRefPtr<BlockStatement> parse_block_statement();
    NonnullRefPtr<BlockStatement> parse_block_statement(bool& is_strict);
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
    NonnullRefPtr<Expression> parse_primary_expression();
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
    NonnullRefPtr<Expression> parse_property_key();
    NonnullRefPtr<AssignmentExpression> parse_assignment_expression(AssignmentOp, NonnullRefPtr<Expression> lhs, int min_precedence, Associativity);

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

    bool has_errors() const { return m_parser_state.m_errors.size(); }
    const Vector<Error>& errors() const { return m_parser_state.m_errors; }
    void print_errors() const
    {
        for (auto& error : m_parser_state.m_errors) {
            auto hint = error.source_location_hint(m_parser_state.m_lexer.source());
            if (!hint.is_empty())
                warnln("{}", hint);
            warnln("SyntaxError: {}", error.to_string());
        }
    }

private:
    friend class ScopePusher;

    Associativity operator_associativity(TokenType) const;
    bool match_expression() const;
    bool match_unary_prefixed_expression() const;
    bool match_secondary_expression(const Vector<TokenType>& forbidden = {}) const;
    bool match_statement() const;
    bool match_declaration() const;
    bool match_variable_declaration() const;
    bool match_identifier_name() const;
    bool match_property_key() const;
    bool match(TokenType type) const;
    bool done() const;
    void expected(const char* what);
    void syntax_error(const String& message, Optional<Position> = {});
    Token consume();
    Token consume(TokenType type);
    Token consume_and_validate_numeric_literal();
    void consume_or_insert_semicolon();
    void save_state();
    void load_state();
    void discard_saved_state();
    Position position() const;

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

    struct ParserState {
        Lexer m_lexer;
        Token m_current_token;
        Vector<Error> m_errors;
        Vector<NonnullRefPtrVector<VariableDeclaration>> m_var_scopes;
        Vector<NonnullRefPtrVector<VariableDeclaration>> m_let_scopes;
        Vector<NonnullRefPtrVector<FunctionDeclaration>> m_function_scopes;
        HashTable<StringView> m_labels_in_scope;
        bool m_strict_mode { false };
        bool m_allow_super_property_lookup { false };
        bool m_allow_super_constructor_call { false };
        bool m_in_function_context { false };
        bool m_in_arrow_function_context { false };
        bool m_in_break_context { false };
        bool m_in_continue_context { false };
        bool m_string_legacy_octal_escape_sequence_in_scope { false };

        explicit ParserState(Lexer);
    };

    Vector<Position> m_rule_starts;
    ParserState m_parser_state;
    Vector<ParserState> m_saved_state;
};
}
