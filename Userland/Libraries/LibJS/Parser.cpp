/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/HashTable.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibRegex/Regex.h>

namespace JS {

class ScopePusher {

private:
    ScopePusher(Parser& parser, ScopeNode* node, bool is_top_level)
        : m_parser(parser)
        , m_is_top_level(is_top_level)
    {
        m_parent_scope = exchange(m_parser.m_state.current_scope_pusher, this);
        VERIFY(node || (m_parent_scope && !is_top_level));
        if (!node)
            m_node = m_parent_scope->m_node;
        else
            m_node = node;
        VERIFY(m_node);

        if (!is_top_level)
            m_top_level_scope = m_parent_scope->m_top_level_scope;
        else
            m_top_level_scope = this;
    }

public:
    static ScopePusher function_scope(Parser& parser, FunctionBody& function_body, Vector<FunctionDeclaration::Parameter> const& parameters)
    {
        ScopePusher scope_pusher(parser, &function_body, true);
        scope_pusher.m_function_parameters = parameters;
        for (auto& parameter : parameters) {
            parameter.binding.visit(
                [&](FlyString const& name) {
                    scope_pusher.m_forbidden_lexical_names.set(name);
                },
                [&](NonnullRefPtr<BindingPattern> const& binding_pattern) {
                    binding_pattern->for_each_bound_name([&](auto const& name) {
                        scope_pusher.m_forbidden_lexical_names.set(name);
                    });
                });
        }
        return scope_pusher;
    }

    static ScopePusher program_scope(Parser& parser, Program& program)
    {
        return ScopePusher(parser, &program, true);
    }

    static ScopePusher block_scope(Parser& parser, ScopeNode& node)
    {
        return ScopePusher(parser, &node, false);
    }

    static ScopePusher for_loop_scope(Parser& parser, RefPtr<ASTNode> const& init)
    {
        ScopePusher scope_pusher(parser, nullptr, false);
        if (init && is<VariableDeclaration>(*init)) {
            auto& variable_declaration = static_cast<VariableDeclaration const&>(*init);
            if (variable_declaration.declaration_kind() != DeclarationKind::Var) {
                variable_declaration.for_each_bound_name([&](auto const& name) {
                    scope_pusher.m_forbidden_var_names.set(name);
                });
            }
        }

        return scope_pusher;
    }

    static ScopePusher catch_scope(Parser& parser, RefPtr<BindingPattern> const& pattern, FlyString const& parameter)
    {
        ScopePusher scope_pusher(parser, nullptr, false);
        if (pattern) {
            pattern->for_each_bound_name([&](auto const& name) {
                scope_pusher.m_forbidden_var_names.set(name);
            });
        } else if (!parameter.is_empty()) {
            scope_pusher.m_var_names.set(parameter);
        }
        return scope_pusher;
    }

    static ScopePusher static_init_block_scope(Parser& parser, ScopeNode& node)
    {
        return ScopePusher(parser, &node, true);
    }

    static ScopePusher class_field_scope(Parser& parser)
    {
        return ScopePusher(parser, nullptr, false);
    }

    void add_declaration(NonnullRefPtr<Declaration> declaration)
    {
        if (declaration->is_lexical_declaration()) {
            declaration->for_each_bound_name([&](auto const& name) {
                if (m_var_names.contains(name) || m_forbidden_lexical_names.contains(name) || m_function_names.contains(name))
                    throw_identifier_declared(name, declaration);

                if (m_lexical_names.set(name) != AK::HashSetResult::InsertedNewEntry)
                    throw_identifier_declared(name, declaration);
            });

            m_node->add_lexical_declaration(move(declaration));
        } else if (!declaration->is_function_declaration()) {
            declaration->for_each_bound_name([&](auto const& name) {
                ScopePusher* pusher = this;
                while (true) {
                    if (pusher->m_lexical_names.contains(name)
                        || pusher->m_function_names.contains(name)
                        || pusher->m_forbidden_var_names.contains(name))
                        throw_identifier_declared(name, declaration);

                    pusher->m_var_names.set(name);
                    if (pusher->m_is_top_level)
                        break;

                    VERIFY(pusher->m_parent_scope != nullptr);
                    pusher = pusher->m_parent_scope;
                }
                VERIFY(pusher->m_is_top_level && pusher->m_node);
                pusher->m_node->add_var_scoped_declaration(declaration);
            });

            VERIFY(m_top_level_scope);
            m_top_level_scope->m_node->add_var_scoped_declaration(move(declaration));
        } else {
            if (m_is_top_level && m_parser.m_program_type == Program::Type::Script) {
                declaration->for_each_bound_name([&](auto const& name) {
                    m_var_names.set(name);
                });
                m_node->add_var_scoped_declaration(move(declaration));
            } else {
                VERIFY(is<FunctionDeclaration>(*declaration));
                auto& function_declaration = static_cast<FunctionDeclaration const&>(*declaration);
                auto& function_name = function_declaration.name();
                if (m_var_names.contains(function_name) || m_lexical_names.contains(function_name))
                    throw_identifier_declared(function_name, declaration);

                if (function_declaration.kind() != FunctionKind::Regular || m_parser.m_state.strict_mode) {
                    if (m_function_names.contains(function_name))
                        throw_identifier_declared(function_name, declaration);

                    m_lexical_names.set(function_name);
                    m_node->add_lexical_declaration(move(declaration));
                    return;
                }

                m_function_names.set(function_name);
                if (!m_lexical_names.contains(function_name))
                    m_functions_to_hoist.append(static_ptr_cast<FunctionDeclaration>(declaration));

                m_node->add_lexical_declaration(move(declaration));
            }
        }
    }

    ScopePusher const* last_function_scope() const
    {
        for (auto scope_ptr = this; scope_ptr; scope_ptr = scope_ptr->m_parent_scope) {
            if (scope_ptr->m_function_parameters.has_value())
                return scope_ptr;
        }
        return nullptr;
    }

    Vector<FunctionDeclaration::Parameter> const& function_parameters() const
    {
        return *m_function_parameters;
    }

    ScopePusher* parent_scope() { return m_parent_scope; }
    ScopePusher const* parent_scope() const { return m_parent_scope; }

    [[nodiscard]] bool has_declaration(StringView name) const
    {
        return m_lexical_names.contains(name) || m_var_names.contains(name) || !m_functions_to_hoist.find_if([&name](auto& function) { return function->name() == name; }).is_end();
    }

    bool contains_direct_call_to_eval() const { return m_contains_direct_call_to_eval; }
    bool contains_access_to_arguments_object() const { return m_contains_access_to_arguments_object; }
    void set_contains_direct_call_to_eval() { m_contains_direct_call_to_eval = true; }
    void set_contains_access_to_arguments_object() { m_contains_access_to_arguments_object = true; }

    ~ScopePusher()
    {
        VERIFY(m_is_top_level || m_parent_scope);

        if (!m_contains_access_to_arguments_object) {
            for (auto& it : m_identifier_and_argument_index_associations) {
                for (auto& identifier : it.value) {
                    if (!has_declaration(identifier.string()))
                        identifier.set_lexically_bound_function_argument_index(it.key);
                }
            }
        }

        for (size_t i = 0; i < m_functions_to_hoist.size(); i++) {
            auto const& function_declaration = m_functions_to_hoist[i];
            if (m_lexical_names.contains(function_declaration.name()) || m_forbidden_var_names.contains(function_declaration.name()))
                continue;
            if (m_is_top_level)
                m_node->add_hoisted_function(move(m_functions_to_hoist[i]));
            else
                m_parent_scope->m_functions_to_hoist.append(move(m_functions_to_hoist[i]));
        }

        if (m_parent_scope && !m_function_parameters.has_value()) {
            m_parent_scope->m_contains_access_to_arguments_object |= m_contains_access_to_arguments_object;
            m_parent_scope->m_contains_direct_call_to_eval |= m_contains_direct_call_to_eval;
        }

        VERIFY(m_parser.m_state.current_scope_pusher == this);
        m_parser.m_state.current_scope_pusher = m_parent_scope;
    }

    void associate_identifier_with_argument_index(NonnullRefPtr<Identifier> identifier, size_t index)
    {
        m_identifier_and_argument_index_associations.ensure(index).append(move(identifier));
    }

private:
    void throw_identifier_declared(FlyString const& name, NonnullRefPtr<Declaration> const& declaration)
    {
        m_parser.syntax_error(String::formatted("Identifier '{}' already declared", name), declaration->source_range().start);
    }

    Parser& m_parser;
    ScopeNode* m_node { nullptr };
    bool m_is_top_level { false };

    ScopePusher* m_parent_scope { nullptr };
    ScopePusher* m_top_level_scope { nullptr };

    HashTable<FlyString> m_lexical_names;
    HashTable<FlyString> m_var_names;
    HashTable<FlyString> m_function_names;

    HashTable<FlyString> m_forbidden_lexical_names;
    HashTable<FlyString> m_forbidden_var_names;
    NonnullRefPtrVector<FunctionDeclaration> m_functions_to_hoist;

    Optional<Vector<FunctionDeclaration::Parameter>> m_function_parameters;
    HashMap<size_t, NonnullRefPtrVector<Identifier>> m_identifier_and_argument_index_associations;

    bool m_contains_access_to_arguments_object { false };
    bool m_contains_direct_call_to_eval { false };
};

class OperatorPrecedenceTable {
public:
    constexpr OperatorPrecedenceTable()
        : m_token_precedence()
    {
        for (size_t i = 0; i < array_size(m_operator_precedence); ++i) {
            auto& op = m_operator_precedence[i];
            m_token_precedence[static_cast<size_t>(op.token)] = op.precedence;
        }
    }

    constexpr int get(TokenType token) const
    {
        int p = m_token_precedence[static_cast<size_t>(token)];
        if (p == 0) {
            warnln("Internal Error: No precedence for operator {}", Token::name(token));
            VERIFY_NOT_REACHED();
            return -1;
        }

        return p;
    }

private:
    int m_token_precedence[cs_num_of_js_tokens];

    struct OperatorPrecedence {
        TokenType token;
        int precedence;
    };

    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Operator_Precedence
    static constexpr const OperatorPrecedence m_operator_precedence[] = {
        { TokenType::Period, 20 },
        { TokenType::BracketOpen, 20 },
        { TokenType::ParenOpen, 20 },
        { TokenType::QuestionMarkPeriod, 20 },

        { TokenType::New, 19 },

        { TokenType::PlusPlus, 18 },
        { TokenType::MinusMinus, 18 },

        { TokenType::ExclamationMark, 17 },
        { TokenType::Tilde, 17 },
        { TokenType::Typeof, 17 },
        { TokenType::Void, 17 },
        { TokenType::Delete, 17 },
        { TokenType::Await, 17 },

        { TokenType::DoubleAsterisk, 16 },

        { TokenType::Asterisk, 15 },
        { TokenType::Slash, 15 },
        { TokenType::Percent, 15 },

        { TokenType::Plus, 14 },
        { TokenType::Minus, 14 },

        { TokenType::ShiftLeft, 13 },
        { TokenType::ShiftRight, 13 },
        { TokenType::UnsignedShiftRight, 13 },

        { TokenType::LessThan, 12 },
        { TokenType::LessThanEquals, 12 },
        { TokenType::GreaterThan, 12 },
        { TokenType::GreaterThanEquals, 12 },
        { TokenType::In, 12 },
        { TokenType::Instanceof, 12 },

        { TokenType::EqualsEquals, 11 },
        { TokenType::ExclamationMarkEquals, 11 },
        { TokenType::EqualsEqualsEquals, 11 },
        { TokenType::ExclamationMarkEqualsEquals, 11 },

        { TokenType::Ampersand, 10 },

        { TokenType::Caret, 9 },

        { TokenType::Pipe, 8 },

        { TokenType::DoubleQuestionMark, 7 },

        { TokenType::DoubleAmpersand, 6 },

        { TokenType::DoublePipe, 5 },

        { TokenType::QuestionMark, 4 },

        { TokenType::Equals, 3 },
        { TokenType::PlusEquals, 3 },
        { TokenType::MinusEquals, 3 },
        { TokenType::DoubleAsteriskEquals, 3 },
        { TokenType::AsteriskEquals, 3 },
        { TokenType::SlashEquals, 3 },
        { TokenType::PercentEquals, 3 },
        { TokenType::ShiftLeftEquals, 3 },
        { TokenType::ShiftRightEquals, 3 },
        { TokenType::UnsignedShiftRightEquals, 3 },
        { TokenType::AmpersandEquals, 3 },
        { TokenType::CaretEquals, 3 },
        { TokenType::PipeEquals, 3 },
        { TokenType::DoubleAmpersandEquals, 3 },
        { TokenType::DoublePipeEquals, 3 },
        { TokenType::DoubleQuestionMarkEquals, 3 },

        { TokenType::Yield, 2 },

        { TokenType::Comma, 1 },
    };
};

constexpr OperatorPrecedenceTable g_operator_precedence;

Parser::ParserState::ParserState(Lexer l, Program::Type program_type)
    : lexer(move(l))
{
    if (program_type == Program::Type::Module)
        lexer.disallow_html_comments();
    current_token = lexer.next();
}

Parser::Parser(Lexer lexer, Program::Type program_type)
    : m_state(move(lexer), program_type)
    , m_program_type(program_type)
{
}

Associativity Parser::operator_associativity(TokenType type) const
{
    switch (type) {
    case TokenType::Period:
    case TokenType::BracketOpen:
    case TokenType::ParenOpen:
    case TokenType::QuestionMarkPeriod:
    case TokenType::Asterisk:
    case TokenType::Slash:
    case TokenType::Percent:
    case TokenType::Plus:
    case TokenType::Minus:
    case TokenType::ShiftLeft:
    case TokenType::ShiftRight:
    case TokenType::UnsignedShiftRight:
    case TokenType::LessThan:
    case TokenType::LessThanEquals:
    case TokenType::GreaterThan:
    case TokenType::GreaterThanEquals:
    case TokenType::In:
    case TokenType::Instanceof:
    case TokenType::EqualsEquals:
    case TokenType::ExclamationMarkEquals:
    case TokenType::EqualsEqualsEquals:
    case TokenType::ExclamationMarkEqualsEquals:
    case TokenType::Typeof:
    case TokenType::Void:
    case TokenType::Delete:
    case TokenType::Await:
    case TokenType::Ampersand:
    case TokenType::Caret:
    case TokenType::Pipe:
    case TokenType::DoubleQuestionMark:
    case TokenType::DoubleAmpersand:
    case TokenType::DoublePipe:
    case TokenType::Comma:
        return Associativity::Left;
    default:
        return Associativity::Right;
    }
}

bool Parser::parse_directive(ScopeNode& body)
{
    bool found_use_strict = false;
    while (!done() && match(TokenType::StringLiteral)) {
        // It cannot be a labelled function since we hit a string literal.
        auto statement = parse_statement(AllowLabelledFunction::No);
        body.append(statement);

        VERIFY(is<ExpressionStatement>(*statement));
        auto& expression = static_cast<ExpressionStatement const&>(*statement).expression();

        if (!is<StringLiteral>(expression))
            break;

        auto& string_literal = static_cast<StringLiteral const&>(expression);
        if (string_literal.is_use_strict_directive()) {
            found_use_strict = true;

            if (m_state.string_legacy_octal_escape_sequence_in_scope)
                syntax_error("Octal escape sequence in string literal not allowed in strict mode");
            break;
        }
    }

    m_state.string_legacy_octal_escape_sequence_in_scope = false;
    return found_use_strict;
}

NonnullRefPtr<Program> Parser::parse_program(bool starts_in_strict_mode)
{
    auto rule_start = push_start();
    auto program = adopt_ref(*new Program({ m_filename, rule_start.position(), position() }, m_program_type));
    ScopePusher program_scope = ScopePusher::program_scope(*this, *program);

    if (m_program_type == Program::Type::Script)
        parse_script(program, starts_in_strict_mode);
    else
        parse_module(program);

    program->source_range().end = position();
    return program;
}

void Parser::parse_script(Program& program, bool starts_in_strict_mode)
{
    bool strict_before = m_state.strict_mode;
    if (starts_in_strict_mode)
        m_state.strict_mode = true;

    bool has_use_strict = parse_directive(program);

    if (m_state.strict_mode || has_use_strict) {
        program.set_strict_mode();
        m_state.strict_mode = true;
    }

    parse_statement_list(program, AllowLabelledFunction::Yes);
    if (!done()) {
        expected("statement or declaration");
        consume();
    }

    m_state.strict_mode = strict_before;
}

void Parser::parse_module(Program& program)
{
    TemporaryChange strict_mode_rollback(m_state.strict_mode, true);
    TemporaryChange await_expression_valid_rollback(m_state.await_expression_is_valid, true);

    // Since strict mode is already enabled we skip any directive parsing.
    while (!done()) {
        parse_statement_list(program, AllowLabelledFunction::Yes);

        if (done())
            break;

        if (match_export_or_import()) {
            VERIFY(m_state.current_token.type() == TokenType::Export || m_state.current_token.type() == TokenType::Import);
            if (m_state.current_token.type() == TokenType::Export)
                program.append_export(parse_export_statement(program));
            else
                program.append_import(parse_import_statement(program));

        } else {
            expected("statement or declaration");
            consume();
        }
    }

    for (auto& export_statement : program.exports()) {
        if (export_statement.has_statement())
            continue;
        for (auto& entry : export_statement.entries()) {
            if (entry.kind == ExportStatement::ExportEntry::ModuleRequest)
                return;

            auto const& exported_name = entry.local_or_import_name;
            bool found = false;
            program.for_each_lexically_declared_name([&](auto const& name) {
                if (name == exported_name) {
                    found = true;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
            if (found)
                continue;
            program.for_each_var_declared_name([&](auto const& name) {
                if (name == exported_name) {
                    found = true;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
            if (!found)
                syntax_error(String::formatted("'{}' is not declared", exported_name));
        }
    }
}

NonnullRefPtr<Declaration> Parser::parse_declaration()
{
    auto rule_start = push_start();
    if (m_state.current_token.type() == TokenType::Async && next_token().type() == TokenType::Function)
        return parse_function_node<FunctionDeclaration>();
    switch (m_state.current_token.type()) {
    case TokenType::Class:
        return parse_class_declaration();
    case TokenType::Function:
        return parse_function_node<FunctionDeclaration>();
    case TokenType::Let:
    case TokenType::Const:
        return parse_variable_declaration();
    default:
        expected("declaration");
        consume();
        return create_ast_node<ErrorDeclaration>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

NonnullRefPtr<Statement> Parser::parse_statement(AllowLabelledFunction allow_labelled_function)
{
    auto rule_start = push_start();
    auto type = m_state.current_token.type();
    switch (type) {
    case TokenType::CurlyOpen:
        return parse_block_statement();
    case TokenType::Return:
        return parse_return_statement();
    case TokenType::Var: {
        auto declaration = parse_variable_declaration();
        m_state.current_scope_pusher->add_declaration(declaration);
        return declaration;
    }
    case TokenType::For:
        return parse_for_statement();
    case TokenType::If:
        return parse_if_statement();
    case TokenType::Throw:
        return parse_throw_statement();
    case TokenType::Try:
        return parse_try_statement();
    case TokenType::Break:
        return parse_break_statement();
    case TokenType::Continue:
        return parse_continue_statement();
    case TokenType::Switch:
        return parse_switch_statement();
    case TokenType::Do:
        return parse_do_while_statement();
    case TokenType::While:
        return parse_while_statement();
    case TokenType::With:
        if (m_state.strict_mode)
            syntax_error("'with' statement not allowed in strict mode");
        return parse_with_statement();
    case TokenType::Debugger:
        return parse_debugger_statement();
    case TokenType::Semicolon:
        consume();
        return create_ast_node<EmptyStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
    case TokenType::Slash:
    case TokenType::SlashEquals:
        m_state.current_token = m_state.lexer.force_slash_as_regex();
        [[fallthrough]];
    default:
        if (match_invalid_escaped_keyword())
            syntax_error("Keyword must not contain escaped characters");

        if (match_identifier_name()) {
            auto result = try_parse_labelled_statement(allow_labelled_function);
            if (!result.is_null())
                return result.release_nonnull();
        }
        if (match_expression()) {
            if (match(TokenType::Async)) {
                auto lookahead_token = next_token();
                if (lookahead_token.type() == TokenType::Function && !lookahead_token.trivia_contains_line_terminator())
                    syntax_error("Async function declaration not allowed in single-statement context");
            } else if (match(TokenType::Function) || match(TokenType::Class)) {
                syntax_error(String::formatted("{} declaration not allowed in single-statement context", m_state.current_token.name()));
            } else if (match(TokenType::Let) && next_token().type() == TokenType::BracketOpen) {
                syntax_error(String::formatted("let followed by [ is not allowed in single-statement context"));
            }

            auto expr = parse_expression(0);
            consume_or_insert_semicolon();
            return create_ast_node<ExpressionStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expr));
        }
        expected("statement");
        consume();
        return create_ast_node<ErrorStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

bool Parser::match_invalid_escaped_keyword() const
{
    if (m_state.current_token.type() != TokenType::EscapedKeyword)
        return false;
    auto token_value = m_state.current_token.value();
    if (token_value == "await"sv)
        return m_program_type == Program::Type::Module || m_state.await_expression_is_valid;
    if (token_value == "async"sv)
        return false;
    if (token_value == "yield"sv)
        return m_state.in_generator_function_context;
    if (m_state.strict_mode)
        return true;
    return token_value != "let"sv;
}

static constexpr AK::Array<StringView, 9> strict_reserved_words = { "implements", "interface", "let", "package", "private", "protected", "public", "static", "yield" };

static bool is_strict_reserved_word(StringView str)
{
    return any_of(strict_reserved_words, [&str](StringView word) {
        return word == str;
    });
}

static bool is_simple_parameter_list(Vector<FunctionNode::Parameter> const& parameters)
{
    return all_of(parameters, [](FunctionNode::Parameter const& parameter) {
        return !parameter.is_rest && parameter.default_value.is_null() && parameter.binding.has<FlyString>();
    });
}

RefPtr<FunctionExpression> Parser::try_parse_arrow_function_expression(bool expect_parens, bool is_async)
{
    if (is_async)
        VERIFY(match(TokenType::Async));

    if (!expect_parens && !is_async) {
        // NOTE: This is a fast path where we try to fail early in case this can't possibly
        //       be a match. The idea is to avoid the expensive parser state save/load mechanism.
        //       The logic is duplicated below in the "real" !expect_parens branch.
        if (!match_identifier() && !match(TokenType::Yield) && !match(TokenType::Await))
            return nullptr;
        auto token = next_token();
        if (token.trivia_contains_line_terminator())
            return nullptr;
        if (token.type() != TokenType::Arrow)
            return nullptr;
    }

    save_state();
    auto rule_start = push_start();

    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    auto function_kind = FunctionKind::Regular;

    if (is_async) {
        consume(TokenType::Async);
        function_kind = FunctionKind::Async;
        if (m_state.current_token.trivia_contains_line_terminator())
            return nullptr;

        // Since we have async it can be followed by paren open in the expect_parens case
        // so we also consume that token.
        if (expect_parens) {
            VERIFY(match(TokenType::ParenOpen));
            consume(TokenType::ParenOpen);
        }
    }

    Vector<FunctionNode::Parameter> parameters;
    i32 function_length = -1;
    if (expect_parens) {
        // We have parens around the function parameters and can re-use the same parsing
        // logic used for regular functions: multiple parameters, default values, rest
        // parameter, maybe a trailing comma. If we have a new syntax error afterwards we
        // check if it's about a wrong token (something like duplicate parameter name must
        // not abort), know parsing failed and rollback the parser state.
        auto previous_syntax_errors = m_state.errors.size();
        TemporaryChange in_async_context(m_state.await_expression_is_valid, is_async || m_state.await_expression_is_valid);

        parameters = parse_formal_parameters(function_length, FunctionNodeParseOptions::IsArrowFunction | (is_async ? FunctionNodeParseOptions::IsAsyncFunction : 0));
        if (m_state.errors.size() > previous_syntax_errors && m_state.errors[previous_syntax_errors].message.starts_with("Unexpected token"))
            return nullptr;
        if (!match(TokenType::ParenClose))
            return nullptr;
        consume();
    } else {
        // No parens - this must be an identifier followed by arrow. That's it.
        if (!match_identifier() && !match(TokenType::Yield) && !match(TokenType::Await))
            return nullptr;
        auto token = consume_identifier_reference();
        if (m_state.strict_mode && token.value().is_one_of("arguments"sv, "eval"sv))
            syntax_error("BindingIdentifier may not be 'arguments' or 'eval' in strict mode");
        if (is_async && token.value() == "await"sv)
            syntax_error("'await' is a reserved identifier in async functions");
        parameters.append({ FlyString { token.value() }, {} });
    }
    // If there's a newline between the closing paren and arrow it's not a valid arrow function,
    // ASI should kick in instead (it'll then fail with "Unexpected token Arrow")
    if (m_state.current_token.trivia_contains_line_terminator())
        return nullptr;
    if (!match(TokenType::Arrow))
        return nullptr;
    consume();

    if (function_length == -1)
        function_length = parameters.size();

    auto old_labels_in_scope = move(m_state.labels_in_scope);
    ScopeGuard guard([&]() {
        m_state.labels_in_scope = move(old_labels_in_scope);
    });

    bool contains_direct_call_to_eval = false;

    auto function_body_result = [&]() -> RefPtr<FunctionBody> {
        TemporaryChange change(m_state.in_arrow_function_context, true);
        TemporaryChange async_context_change(m_state.await_expression_is_valid, is_async);
        TemporaryChange in_class_static_init_block_change(m_state.in_class_static_init_block, false);

        if (match(TokenType::CurlyOpen)) {
            // Parse a function body with statements
            return parse_function_body(parameters, function_kind, contains_direct_call_to_eval);
        }
        if (match_expression()) {
            // Parse a function body which returns a single expression

            // FIXME: We synthesize a block with a return statement
            // for arrow function bodies which are a single expression.
            // Esprima generates a single "ArrowFunctionExpression"
            // with a "body" property.
            auto return_block = create_ast_node<FunctionBody>({ m_state.current_token.filename(), rule_start.position(), position() });
            ScopePusher function_scope = ScopePusher::function_scope(*this, return_block, parameters);
            auto return_expression = parse_expression(2);
            return_block->append<ReturnStatement>({ m_filename, rule_start.position(), position() }, move(return_expression));
            if (m_state.strict_mode)
                return_block->set_strict_mode();
            contains_direct_call_to_eval = function_scope.contains_direct_call_to_eval();
            return return_block;
        }
        // Invalid arrow function body
        return nullptr;
    }();

    if (function_body_result.is_null())
        return nullptr;

    state_rollback_guard.disarm();
    discard_saved_state();
    auto body = function_body_result.release_nonnull();

    if (body->in_strict_mode()) {
        for (auto& parameter : parameters) {
            parameter.binding.visit(
                [&](FlyString const& name) {
                    check_identifier_name_for_assignment_validity(name, true);
                },
                [&](auto const&) {});
        }
    }

    return create_ast_node<FunctionExpression>(
        { m_state.current_token.filename(), rule_start.position(), position() }, "", move(body),
        move(parameters), function_length, function_kind, body->in_strict_mode(),
        /* might_need_arguments_object */ false, contains_direct_call_to_eval, /* is_arrow_function */ true);
}

RefPtr<Statement> Parser::try_parse_labelled_statement(AllowLabelledFunction allow_function)
{
    {
        // NOTE: This is a fast path where we try to fail early to avoid the expensive save_state+load_state.
        if (next_token().type() != TokenType::Colon)
            return {};
    }

    save_state();
    auto rule_start = push_start();
    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    if (m_state.current_token.value() == "yield"sv && (m_state.strict_mode || m_state.in_generator_function_context)) {
        return {};
    }

    if (m_state.current_token.value() == "await"sv && (m_program_type == Program::Type::Module || m_state.await_expression_is_valid || m_state.in_class_static_init_block)) {
        return {};
    }

    auto identifier = [&] {
        if (m_state.current_token.value() == "await"sv) {
            return consume().value();
        }
        return consume_identifier_reference().value();
    }();
    if (!match(TokenType::Colon))
        return {};
    consume(TokenType::Colon);

    if (!match_statement())
        return {};

    state_rollback_guard.disarm();
    discard_saved_state();

    if (m_state.strict_mode && identifier == "let"sv) {
        syntax_error("Strict mode reserved word 'let' is not allowed in label", rule_start.position());
        return {};
    }

    if (match(TokenType::Function) && (allow_function == AllowLabelledFunction::No || m_state.strict_mode)) {
        syntax_error("Not allowed to declare a function here");
        return {};
    }

    if (m_state.labels_in_scope.contains(identifier))
        syntax_error(String::formatted("Label '{}' has already been declared", identifier));

    RefPtr<Statement> labelled_statement;

    auto is_iteration_statement = false;

    if (match(TokenType::Function)) {
        m_state.labels_in_scope.set(identifier, {});
        auto function_declaration = parse_function_node<FunctionDeclaration>();
        VERIFY(m_state.current_scope_pusher);
        m_state.current_scope_pusher->add_declaration(function_declaration);
        if (function_declaration->kind() == FunctionKind::Generator)
            syntax_error("Generator functions cannot be defined in labelled statements");
        if (function_declaration->kind() == FunctionKind::Async)
            syntax_error("Async functions cannot be defined in labelled statements");

        labelled_statement = move(function_declaration);
    } else {
        m_state.labels_in_scope.set(identifier, {});
        labelled_statement = parse_statement(allow_function);
        if (is<IterationStatement>(*labelled_statement)) {
            is_iteration_statement = true;
            static_cast<IterationStatement&>(*labelled_statement).add_label(identifier);
        } else if (is<LabelableStatement>(*labelled_statement)) {
            static_cast<LabelableStatement&>(*labelled_statement).add_label(identifier);
        }
    }

    if (!is_iteration_statement) {
        if (auto entry = m_state.labels_in_scope.find(identifier); entry != m_state.labels_in_scope.end() && entry->value.has_value())
            syntax_error("labelled continue statement cannot use non iterating statement", m_state.labels_in_scope.get(identifier).value());
    }

    m_state.labels_in_scope.remove(identifier);

    return labelled_statement.release_nonnull();
}

RefPtr<MetaProperty> Parser::try_parse_new_target_expression()
{
    // Optimization which skips the save/load state.
    if (next_token().type() != TokenType::Period)
        return {};

    save_state();
    auto rule_start = push_start();
    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    consume(TokenType::New);
    consume(TokenType::Period);
    if (!match(TokenType::Identifier))
        return {};
    // The string 'target' cannot have escapes so we check original value.
    if (consume().original_value() != "target"sv)
        return {};

    state_rollback_guard.disarm();
    discard_saved_state();
    return create_ast_node<MetaProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, MetaProperty::Type::NewTarget);
}

RefPtr<MetaProperty> Parser::try_parse_import_meta_expression()
{
    // Optimization which skips the save/load state.
    if (next_token().type() != TokenType::Period)
        return {};

    save_state();
    auto rule_start = push_start();
    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    consume(TokenType::Import);
    consume(TokenType::Period);
    if (!match(TokenType::Identifier))
        return {};
    // The string 'meta' cannot have escapes so we check original value.
    if (consume().original_value() != "meta"sv)
        return {};

    state_rollback_guard.disarm();
    discard_saved_state();
    return create_ast_node<MetaProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, MetaProperty::Type::ImportMeta);
}

NonnullRefPtr<ImportCall> Parser::parse_import_call()
{
    auto rule_start = push_start();

    // We use the extended definition:
    //  ImportCall[Yield, Await]:
    //      import(AssignmentExpression[+In, ?Yield, ?Await] ,opt)
    //      import(AssignmentExpression[+In, ?Yield, ?Await] ,AssignmentExpression[+In, ?Yield, ?Await] ,opt)
    // From https://tc39.es/proposal-import-assertions/#sec-evaluate-import-call

    consume(TokenType::Import);
    consume(TokenType::ParenOpen);
    auto argument = parse_expression(2);

    RefPtr<Expression> options;
    if (match(TokenType::Comma)) {
        consume(TokenType::Comma);

        if (!match(TokenType::ParenClose)) {
            options = parse_expression(2);

            // Second optional comma
            if (match(TokenType::Comma))
                consume(TokenType::Comma);
        }
    }

    consume(TokenType::ParenClose);

    return create_ast_node<ImportCall>({ m_state.current_token.filename(), rule_start.position(), position() }, move(argument), move(options));
}

NonnullRefPtr<ClassDeclaration> Parser::parse_class_declaration()
{
    auto rule_start = push_start();
    return create_ast_node<ClassDeclaration>({ m_state.current_token.filename(), rule_start.position(), position() }, parse_class_expression(true));
}

NonnullRefPtr<ClassExpression> Parser::parse_class_expression(bool expect_class_name)
{
    auto rule_start = push_start();
    // Classes are always in strict mode.
    TemporaryChange strict_mode_rollback(m_state.strict_mode, true);

    consume(TokenType::Class);

    NonnullRefPtrVector<ClassElement> elements;
    RefPtr<Expression> super_class;
    RefPtr<FunctionExpression> constructor;
    HashTable<FlyString> found_private_names;

    String class_name = expect_class_name || match_identifier() || match(TokenType::Yield) || match(TokenType::Await)
        ? consume_identifier_reference().value().to_string()
        : "";

    check_identifier_name_for_assignment_validity(class_name, true);
    if (m_state.in_class_static_init_block && class_name == "await"sv)
        syntax_error("Identifier must not be a reserved word in modules ('await')");

    if (match(TokenType::Extends)) {
        consume();
        auto [expression, should_continue_parsing] = parse_primary_expression();

        // Basically a (much) simplified parse_secondary_expression().
        for (;;) {
            if (match(TokenType::TemplateLiteralStart)) {
                auto template_literal = parse_template_literal(true);
                expression = create_ast_node<TaggedTemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(template_literal));
                continue;
            }
            if (match(TokenType::BracketOpen) || match(TokenType::Period) || match(TokenType::ParenOpen)) {
                auto precedence = g_operator_precedence.get(m_state.current_token.type());
                expression = parse_secondary_expression(move(expression), precedence);
                continue;
            }
            break;
        }

        super_class = move(expression);
        (void)should_continue_parsing;
    }

    consume(TokenType::CurlyOpen);

    HashTable<StringView> referenced_private_names;
    HashTable<StringView>* outer_referenced_private_names = m_state.referenced_private_names;
    m_state.referenced_private_names = &referenced_private_names;
    ScopeGuard restore_private_name_table = [&] {
        m_state.referenced_private_names = outer_referenced_private_names;
    };

    while (!done() && !match(TokenType::CurlyClose)) {
        RefPtr<Expression> property_key;
        bool is_static = false;
        bool is_constructor = false;
        bool is_generator = false;
        bool is_async = false;
        auto method_kind = ClassMethod::Kind::Method;

        if (match(TokenType::Semicolon)) {
            consume();
            continue;
        }

        if (match(TokenType::Async)) {
            auto lookahead_token = next_token();
            if (lookahead_token.type() != TokenType::Semicolon && lookahead_token.type() != TokenType::CurlyClose
                && !lookahead_token.trivia_contains_line_terminator()) {
                consume();
                is_async = true;
            }
        }

        if (match(TokenType::Asterisk)) {
            consume();
            is_generator = true;
        }

        StringView name;
        if (match_property_key() || match(TokenType::PrivateIdentifier)) {
            if (!is_generator && !is_async && m_state.current_token.original_value() == "static"sv) {
                if (match(TokenType::Identifier)) {
                    consume();
                    is_static = true;
                    if (match(TokenType::Async)) {
                        consume();
                        is_async = true;
                    }
                    if (match(TokenType::Asterisk)) {
                        consume();
                        is_generator = true;
                    }
                }
            }

            if (match(TokenType::Identifier)) {
                auto identifier_name = m_state.current_token.original_value();

                if (identifier_name == "get"sv) {
                    method_kind = ClassMethod::Kind::Getter;
                    consume();
                } else if (identifier_name == "set"sv) {
                    method_kind = ClassMethod::Kind::Setter;
                    consume();
                }
            }

            if (match_property_key() || match(TokenType::PrivateIdentifier)) {
                switch (m_state.current_token.type()) {
                case TokenType::Identifier:
                    name = consume().value();
                    property_key = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, name);
                    break;
                case TokenType::PrivateIdentifier:
                    name = consume().value();
                    if (name == "#constructor")
                        syntax_error("Private property with name '#constructor' is not allowed");

                    if (method_kind != ClassMethod::Kind::Method) {
                        // It is a Syntax Error if PrivateBoundIdentifiers of ClassElementList contains any duplicate entries,
                        //   unless the name is used once for a getter and once for a setter and in no other entries,
                        //   and the getter and setter are either both static or both non-static.

                        for (auto& element : elements) {
                            auto private_name = element.private_bound_identifier();
                            if (!private_name.has_value() || private_name.value() != name)
                                continue;

                            if (element.class_element_kind() != ClassElement::ElementKind::Method
                                || element.is_static() != is_static) {
                                syntax_error(String::formatted("Duplicate private field or method named '{}'", name));
                                break;
                            }

                            VERIFY(is<ClassMethod>(element));
                            auto& class_method_element = static_cast<ClassMethod const&>(element);

                            if (class_method_element.kind() == ClassMethod::Kind::Method || class_method_element.kind() == method_kind) {
                                syntax_error(String::formatted("Duplicate private field or method named '{}'", name));
                                break;
                            }
                        }

                        found_private_names.set(name);
                    } else if (found_private_names.set(name) != AK::HashSetResult::InsertedNewEntry) {
                        syntax_error(String::formatted("Duplicate private field or method named '{}'", name));
                    }

                    property_key = create_ast_node<PrivateIdentifier>({ m_state.current_token.filename(), rule_start.position(), position() }, name);
                    break;
                case TokenType::StringLiteral: {
                    auto string_literal = parse_string_literal(consume());
                    name = string_literal->value();
                    property_key = move(string_literal);
                    break;
                }
                default:
                    property_key = parse_property_key();
                    break;
                }

                // https://tc39.es/ecma262/#sec-class-definitions-static-semantics-early-errors
                // ClassElement : static MethodDefinition
                //   It is a Syntax Error if PropName of MethodDefinition is "prototype".
                if (is_static && name == "prototype"sv)
                    syntax_error("Classes may not have a static property named 'prototype'");
            } else if ((match(TokenType::ParenOpen) || match(TokenType::Equals) || match(TokenType::Semicolon) || match(TokenType::CurlyClose)) && (is_static || is_async || method_kind != ClassMethod::Kind::Method)) {
                switch (method_kind) {
                case ClassMethod::Kind::Method:
                    if (is_async) {
                        name = "async";
                        is_async = false;
                    } else {
                        VERIFY(is_static);
                        name = "static";
                        is_static = false;
                    }
                    break;
                case ClassMethod::Kind::Getter:
                    name = "get";
                    method_kind = ClassMethod::Kind::Method;
                    break;
                case ClassMethod::Kind::Setter:
                    name = "set";
                    method_kind = ClassMethod::Kind::Method;
                    break;
                }
                property_key = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, name);
            } else if (match(TokenType::CurlyOpen) && is_static) {
                auto static_start = push_start();
                consume(TokenType::CurlyOpen);

                auto static_init_block = create_ast_node<FunctionBody>({ m_state.current_token.filename(), rule_start.position(), position() });

                TemporaryChange break_context_rollback(m_state.in_break_context, false);
                TemporaryChange continue_context_rollback(m_state.in_continue_context, false);
                TemporaryChange function_context_rollback(m_state.in_function_context, false);
                TemporaryChange generator_function_context_rollback(m_state.in_generator_function_context, false);
                TemporaryChange async_function_context_rollback(m_state.await_expression_is_valid, false);
                TemporaryChange class_field_initializer_rollback(m_state.in_class_field_initializer, true);
                TemporaryChange class_static_init_block_rollback(m_state.in_class_static_init_block, true);

                ScopePusher static_init_scope = ScopePusher::static_init_block_scope(*this, *static_init_block);
                parse_statement_list(static_init_block);

                consume(TokenType::CurlyClose);
                elements.append(create_ast_node<StaticInitializer>({ m_state.current_token.filename(), static_start.position(), position() }, move(static_init_block), static_init_scope.contains_direct_call_to_eval()));
                continue;
            } else {
                expected("property key");
            }

            // Constructor may be a StringLiteral or an Identifier.
            if (!is_static && name == "constructor"sv) {
                if (method_kind != ClassMethod::Kind::Method)
                    syntax_error("Class constructor may not be an accessor");
                if (!constructor.is_null())
                    syntax_error("Classes may not have more than one constructor");
                if (is_generator)
                    syntax_error("Class constructor may not be a generator");
                if (is_async)
                    syntax_error("Class constructor may not be async");

                is_constructor = true;
            }
        }

        if (match(TokenType::ParenOpen)) {
            u8 parse_options = FunctionNodeParseOptions::AllowSuperPropertyLookup;
            if (!super_class.is_null() && !is_static && is_constructor)
                parse_options |= FunctionNodeParseOptions::AllowSuperConstructorCall;
            if (method_kind == ClassMethod::Kind::Getter)
                parse_options |= FunctionNodeParseOptions::IsGetterFunction;
            if (method_kind == ClassMethod::Kind::Setter)
                parse_options |= FunctionNodeParseOptions::IsSetterFunction;
            if (is_generator)
                parse_options |= FunctionNodeParseOptions::IsGeneratorFunction;
            if (is_async)
                parse_options |= FunctionNodeParseOptions::IsAsyncFunction;
            auto function = parse_function_node<FunctionExpression>(parse_options);
            if (is_constructor) {
                constructor = move(function);
            } else if (!property_key.is_null()) {
                elements.append(create_ast_node<ClassMethod>({ m_state.current_token.filename(), rule_start.position(), position() }, property_key.release_nonnull(), move(function), method_kind, is_static));
            } else {
                syntax_error("No key for class method");
            }
        } else if (is_generator || is_async) {
            expected("ParenOpen");
            consume();
        } else if (property_key.is_null()) {
            expected("property key");
            consume();
        } else {
            if (name == "constructor"sv)
                syntax_error("Class cannot have field named 'constructor'");

            RefPtr<Expression> initializer;
            bool contains_direct_call_to_eval = false;

            if (match(TokenType::Equals)) {
                consume();

                TemporaryChange super_property_access_rollback(m_state.allow_super_property_lookup, true);
                TemporaryChange field_initializer_rollback(m_state.in_class_field_initializer, true);

                auto class_field_scope = ScopePusher::class_field_scope(*this);
                initializer = parse_expression(2);
                contains_direct_call_to_eval = class_field_scope.contains_direct_call_to_eval();
            }

            elements.append(create_ast_node<ClassField>({ m_state.current_token.filename(), rule_start.position(), position() }, property_key.release_nonnull(), move(initializer), contains_direct_call_to_eval, is_static));
            consume_or_insert_semicolon();
        }
    }

    consume(TokenType::CurlyClose);

    if (constructor.is_null()) {
        auto constructor_body = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
        if (!super_class.is_null()) {
            // Set constructor to the result of parsing the source text
            // constructor(... args){ super (...args);}
            auto super_call = create_ast_node<SuperCall>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                Vector { CallExpression::Argument { create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, "args"), true } });
            constructor_body->append(create_ast_node<ExpressionStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(super_call)));

            constructor = create_ast_node<FunctionExpression>(
                { m_state.current_token.filename(), rule_start.position(), position() }, class_name, move(constructor_body),
                Vector { FunctionNode::Parameter { FlyString { "args" }, nullptr, true } }, 0, FunctionKind::Regular,
                /* is_strict_mode */ true, /* might_need_arguments_object */ false, /* contains_direct_call_to_eval */ false);
        } else {
            constructor = create_ast_node<FunctionExpression>(
                { m_state.current_token.filename(), rule_start.position(), position() }, class_name, move(constructor_body),
                Vector<FunctionNode::Parameter> {}, 0, FunctionKind::Regular,
                /* is_strict_mode */ true, /* might_need_arguments_object */ false, /* contains_direct_call_to_eval */ false);
        }
    }

    // We could be in a subclass defined within the main class so must move all non declared private names to outer.
    for (auto& private_name : referenced_private_names) {
        if (found_private_names.contains(private_name))
            continue;
        if (outer_referenced_private_names)
            outer_referenced_private_names->set(private_name);
        else // FIXME: Make these error appear in the appropriate places.
            syntax_error(String::formatted("Reference to undeclared private field or method '{}'", private_name));
    }

    return create_ast_node<ClassExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(class_name), move(constructor), move(super_class), move(elements));
}

Parser::PrimaryExpressionParseResult Parser::parse_primary_expression()
{
    auto rule_start = push_start();
    if (match_unary_prefixed_expression())
        return { parse_unary_prefixed_expression() };

    auto try_arrow_function_parse_or_fail = [this](Position const& position, bool expect_paren, bool is_async = false) -> RefPtr<FunctionExpression> {
        if (try_parse_arrow_function_expression_failed_at_position(position))
            return nullptr;
        auto arrow_function = try_parse_arrow_function_expression(expect_paren, is_async);
        if (arrow_function)
            return arrow_function;

        set_try_parse_arrow_function_expression_failed_at_position(position, true);
        return nullptr;
    };

    switch (m_state.current_token.type()) {
    case TokenType::ParenOpen: {
        auto paren_position = position();
        consume(TokenType::ParenOpen);
        if ((match(TokenType::ParenClose) || match_identifier() || match(TokenType::TripleDot) || match(TokenType::CurlyOpen) || match(TokenType::BracketOpen))) {
            if (auto arrow_function_result = try_arrow_function_parse_or_fail(paren_position, true))
                return { arrow_function_result.release_nonnull(), false };
        }
        auto expression = parse_expression(0);
        consume(TokenType::ParenClose);
        if (is<FunctionExpression>(*expression)) {
            auto& function = static_cast<FunctionExpression&>(*expression);
            if (function.kind() == FunctionKind::Generator && function.name() == "yield"sv)
                syntax_error("function is not allowed to be called 'yield' in this context", function.source_range().start);
            if (function.kind() == FunctionKind::Async && function.name() == "await"sv)
                syntax_error("function is not allowed to be called 'await' in this context", function.source_range().start);
        }
        return { move(expression) };
    }
    case TokenType::This:
        consume();
        return { create_ast_node<ThisExpression>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    case TokenType::Class:
        return { parse_class_expression(false) };
    case TokenType::Super:
        consume();
        if (!m_state.allow_super_property_lookup)
            syntax_error("'super' keyword unexpected here");
        return { create_ast_node<SuperExpression>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    case TokenType::EscapedKeyword:
        if (match_invalid_escaped_keyword())
            syntax_error("Keyword must not contain escaped characters");
        [[fallthrough]];
    case TokenType::Identifier: {
    read_as_identifier:;
        if (auto arrow_function_result = try_arrow_function_parse_or_fail(position(), false))
            return { arrow_function_result.release_nonnull(), false };

        auto string = m_state.current_token.value();
        // This could be 'eval' or 'arguments' and thus needs a custom check (`eval[1] = true`)
        if (m_state.strict_mode && (string == "let" || is_strict_reserved_word(string)))
            syntax_error(String::formatted("Identifier must not be a reserved word in strict mode ('{}')", string));
        return { parse_identifier() };
    }
    case TokenType::NumericLiteral:
        return { create_ast_node<NumericLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume_and_validate_numeric_literal().double_value()) };
    case TokenType::BigIntLiteral:
        return { create_ast_node<BigIntLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value()) };
    case TokenType::BoolLiteral:
        return { create_ast_node<BooleanLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().bool_value()) };
    case TokenType::StringLiteral:
        return { parse_string_literal(consume()) };
    case TokenType::NullLiteral:
        consume();
        return { create_ast_node<NullLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    case TokenType::CurlyOpen:
        return { parse_object_expression() };
    case TokenType::Async: {
        auto lookahead_token = next_token();
        // No valid async function (arrow or not) can have a line terminator after the async since asi would kick in.
        if (lookahead_token.trivia_contains_line_terminator())
            goto read_as_identifier;

        if (lookahead_token.type() == TokenType::Function)
            return { parse_function_node<FunctionExpression>() };

        if (lookahead_token.type() == TokenType::ParenOpen) {
            if (auto arrow_function_result = try_arrow_function_parse_or_fail(position(), true, true))
                return { arrow_function_result.release_nonnull(), false };
        } else if (lookahead_token.is_identifier_name()) {
            if (auto arrow_function_result = try_arrow_function_parse_or_fail(position(), false, true))
                return { arrow_function_result.release_nonnull(), false };
        }
        goto read_as_identifier;
    }
    case TokenType::Function:
        return { parse_function_node<FunctionExpression>() };
    case TokenType::BracketOpen:
        return { parse_array_expression() };
    case TokenType::RegexLiteral:
        return { parse_regexp_literal() };
    case TokenType::TemplateLiteralStart:
        return { parse_template_literal(false) };
    case TokenType::New: {
        auto new_start = position();
        auto new_target_result = try_parse_new_target_expression();
        if (!new_target_result.is_null()) {
            if (!m_state.in_function_context)
                syntax_error("'new.target' not allowed outside of a function", new_start);
            return { new_target_result.release_nonnull() };
        }
        return { parse_new_expression() };
    }
    case TokenType::Import: {
        auto lookahead_token = next_token();
        VERIFY(lookahead_token.type() == TokenType::Period || lookahead_token.type() == TokenType::ParenOpen);
        if (lookahead_token.type() == TokenType::ParenOpen)
            return { parse_import_call() };

        if (auto import_meta = try_parse_import_meta_expression()) {
            if (m_program_type != Program::Type::Module)
                syntax_error("import.meta is only allowed in modules");
            return { import_meta.release_nonnull() };
        }
        break;
    }
    case TokenType::Yield:
        if (!m_state.in_generator_function_context)
            goto read_as_identifier;
        return { parse_yield_expression(), false };
    case TokenType::Await:
        if (!m_state.await_expression_is_valid)
            goto read_as_identifier;
        return { parse_await_expression() };
    case TokenType::PrivateIdentifier:
        if (next_token().type() != TokenType::In)
            syntax_error("Cannot have a private identifier in expression if not followed by 'in'");
        if (!is_private_identifier_valid())
            syntax_error(String::formatted("Reference to undeclared private field or method '{}'", m_state.current_token.value()));
        return { create_ast_node<PrivateIdentifier>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value()) };
    default:
        if (match_identifier_name())
            goto read_as_identifier;
        break;
    }
    expected("primary expression");
    consume();
    return { create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() }) };
}

NonnullRefPtr<RegExpLiteral> Parser::parse_regexp_literal()
{
    auto rule_start = push_start();
    auto pattern = consume().value();
    // Remove leading and trailing slash.
    pattern = pattern.substring_view(1, pattern.length() - 2);

    auto flags = String::empty();
    auto parsed_flags = RegExpObject::default_flags;

    if (match(TokenType::RegexFlags)) {
        auto flags_start = position();
        flags = consume().value();

        auto parsed_flags_or_error = regex_flags_from_string(flags);
        if (parsed_flags_or_error.is_error())
            syntax_error(parsed_flags_or_error.release_error(), flags_start);
        else
            parsed_flags = parsed_flags_or_error.release_value();
    }

    auto parsed_pattern = parse_regex_pattern(pattern, parsed_flags.has_flag_set(ECMAScriptFlags::Unicode));
    auto parsed_regex = Regex<ECMA262>::parse_pattern(parsed_pattern, parsed_flags);

    if (parsed_regex.error != regex::Error::NoError)
        syntax_error(String::formatted("RegExp compile error: {}", Regex<ECMA262>(parsed_regex, parsed_pattern, parsed_flags).error_string()), rule_start.position());

    SourceRange range { m_state.current_token.filename(), rule_start.position(), position() };
    return create_ast_node<RegExpLiteral>(move(range), move(parsed_regex), move(parsed_pattern), move(parsed_flags), pattern.to_string(), move(flags));
}

NonnullRefPtr<Expression> Parser::parse_unary_prefixed_expression()
{
    auto rule_start = push_start();
    auto precedence = g_operator_precedence.get(m_state.current_token.type());
    auto associativity = operator_associativity(m_state.current_token.type());
    switch (m_state.current_token.type()) {
    case TokenType::PlusPlus: {
        consume();
        auto rhs_start = position();
        auto rhs = parse_expression(precedence, associativity);
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for ++foo()
        if (!is<Identifier>(*rhs) && !is<MemberExpression>(*rhs))
            syntax_error(String::formatted("Right-hand side of prefix increment operator must be identifier or member expression, got {}", rhs->class_name()), rhs_start);

        if (m_state.strict_mode && is<Identifier>(*rhs)) {
            auto& identifier = static_cast<Identifier&>(*rhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }

        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Increment, move(rhs), true);
    }
    case TokenType::MinusMinus: {
        consume();
        auto rhs_start = position();
        auto rhs = parse_expression(precedence, associativity);
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for --foo()
        if (!is<Identifier>(*rhs) && !is<MemberExpression>(*rhs))
            syntax_error(String::formatted("Right-hand side of prefix decrement operator must be identifier or member expression, got {}", rhs->class_name()), rhs_start);

        if (m_state.strict_mode && is<Identifier>(*rhs)) {
            auto& identifier = static_cast<Identifier&>(*rhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }

        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Decrement, move(rhs), true);
    }
    case TokenType::ExclamationMark:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Not, parse_expression(precedence, associativity));
    case TokenType::Tilde:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::BitwiseNot, parse_expression(precedence, associativity));
    case TokenType::Plus:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Plus, parse_expression(precedence, associativity));
    case TokenType::Minus:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Minus, parse_expression(precedence, associativity));
    case TokenType::Typeof:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Typeof, parse_expression(precedence, associativity));
    case TokenType::Void:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Void, parse_expression(precedence, associativity));
    case TokenType::Delete: {
        consume();
        auto rhs_start = position();
        auto rhs = parse_expression(precedence, associativity);
        if (is<Identifier>(*rhs) && m_state.strict_mode) {
            syntax_error("Delete of an unqualified identifier in strict mode.", rhs_start);
        }
        if (is<MemberExpression>(*rhs)) {
            auto& member_expression = static_cast<MemberExpression const&>(*rhs);
            if (member_expression.ends_in_private_name())
                syntax_error("Private fields cannot be deleted");
        }
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Delete, move(rhs));
    }
    default:
        expected("primary expression");
        consume();
        return create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

NonnullRefPtr<Expression> Parser::parse_property_key()
{
    auto rule_start = push_start();
    if (match(TokenType::StringLiteral)) {
        return parse_string_literal(consume());
    } else if (match(TokenType::NumericLiteral)) {
        return create_ast_node<NumericLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().double_value());
    } else if (match(TokenType::BigIntLiteral)) {
        return create_ast_node<BigIntLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value());
    } else if (match(TokenType::BracketOpen)) {
        consume(TokenType::BracketOpen);
        auto result = parse_expression(2);
        consume(TokenType::BracketClose);
        return result;
    } else {
        if (!match_identifier_name())
            expected("IdentifierName");
        return create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value());
    }
}

NonnullRefPtr<ObjectExpression> Parser::parse_object_expression()
{
    auto rule_start = push_start();
    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<ObjectProperty> properties;
    ObjectProperty::Type property_type;
    Optional<SourceRange> invalid_object_literal_property_range;

    auto skip_to_next_property = [&] {
        while (!done() && !match(TokenType::Comma) && !match(TokenType::CurlyOpen))
            consume();
    };

    // It is a Syntax Error if PropertyNameList of PropertyDefinitionList contains any duplicate
    // entries for "__proto__" and at least two of those entries were obtained from productions  of
    // the form PropertyDefinition : PropertyKey : AssignmentExpression .
    bool has_direct_proto_property = false;

    while (!done() && !match(TokenType::CurlyClose)) {
        property_type = ObjectProperty::Type::KeyValue;
        RefPtr<Expression> property_name;
        RefPtr<Expression> property_value;
        FunctionKind function_kind { FunctionKind::Regular };

        if (match(TokenType::TripleDot)) {
            consume();
            property_name = parse_expression(4);
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, nullptr, ObjectProperty::Type::Spread, false));
            if (!match(TokenType::Comma))
                break;
            consume(TokenType::Comma);
            continue;
        }

        auto type = m_state.current_token.type();

        if (match(TokenType::Async)) {
            auto lookahead_token = next_token();

            if (lookahead_token.type() != TokenType::ParenOpen && lookahead_token.type() != TokenType::Colon
                && lookahead_token.type() != TokenType::Comma && lookahead_token.type() != TokenType::CurlyClose
                && lookahead_token.type() != TokenType::Async
                && !lookahead_token.trivia_contains_line_terminator()) {
                consume(TokenType::Async);
                function_kind = FunctionKind::Async;
            }
        }
        if (match(TokenType::Asterisk)) {
            consume();
            property_type = ObjectProperty::Type::KeyValue;
            property_name = parse_property_key();
            VERIFY(function_kind == FunctionKind::Regular || function_kind == FunctionKind::Async);
            function_kind = function_kind == FunctionKind::Regular ? FunctionKind::Generator : FunctionKind::AsyncGenerator;
        } else if (match_identifier()) {
            auto identifier = consume();
            if (identifier.original_value() == "get"sv && match_property_key()) {
                property_type = ObjectProperty::Type::Getter;
                property_name = parse_property_key();
            } else if (identifier.original_value() == "set"sv && match_property_key()) {
                property_type = ObjectProperty::Type::Setter;
                property_name = parse_property_key();
            } else {
                property_name = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, identifier.value());
                property_value = create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, identifier.value());
            }
        } else {
            property_name = parse_property_key();
        }

        bool is_proto = (type == TokenType::StringLiteral || type == TokenType::Identifier) && is<StringLiteral>(*property_name) && static_cast<StringLiteral const&>(*property_name).value() == "__proto__";

        if (property_type == ObjectProperty::Type::Getter || property_type == ObjectProperty::Type::Setter) {
            if (!match(TokenType::ParenOpen)) {
                expected("'(' for object getter or setter property");
                skip_to_next_property();
                continue;
            }
        }
        if (match(TokenType::Equals)) {
            // Not a valid object literal, but a valid assignment target
            consume();
            // Parse the expression and throw it away
            auto expression = parse_expression(2);
            if (!invalid_object_literal_property_range.has_value())
                invalid_object_literal_property_range = expression->source_range();
        } else if (match(TokenType::ParenOpen)) {
            VERIFY(property_name);
            u8 parse_options = FunctionNodeParseOptions::AllowSuperPropertyLookup;
            if (property_type == ObjectProperty::Type::Getter)
                parse_options |= FunctionNodeParseOptions::IsGetterFunction;
            if (property_type == ObjectProperty::Type::Setter)
                parse_options |= FunctionNodeParseOptions::IsSetterFunction;
            if (function_kind == FunctionKind::Generator || function_kind == FunctionKind::AsyncGenerator)
                parse_options |= FunctionNodeParseOptions::IsGeneratorFunction;
            if (function_kind == FunctionKind::Async || function_kind == FunctionKind::AsyncGenerator)
                parse_options |= FunctionNodeParseOptions::IsAsyncFunction;
            auto function = parse_function_node<FunctionExpression>(parse_options);
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, function, property_type, true));
        } else if (match(TokenType::Colon)) {
            if (!property_name) {
                expected("a property name");
                skip_to_next_property();
                continue;
            }
            consume();
            if (is_proto) {
                if (has_direct_proto_property)
                    syntax_error("Property name '__proto__' must not appear more than once in object literal");
                has_direct_proto_property = true;
            }
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, parse_expression(2), property_type, false));
        } else if (property_name && property_value) {
            if (m_state.strict_mode && is<StringLiteral>(*property_name)) {
                auto& string_literal = static_cast<StringLiteral const&>(*property_name);
                if (is_strict_reserved_word(string_literal.value()))
                    syntax_error(String::formatted("'{}' is a reserved keyword", string_literal.value()));
            }

            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, *property_value, property_type, false));
        } else {
            expected("a property");
            skip_to_next_property();
            continue;
        }

        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::CurlyClose);
    return create_ast_node<ObjectExpression>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        move(properties),
        move(invalid_object_literal_property_range));
}

NonnullRefPtr<ArrayExpression> Parser::parse_array_expression()
{
    auto rule_start = push_start();
    consume(TokenType::BracketOpen);

    Vector<RefPtr<Expression>> elements;
    while (match_expression() || match(TokenType::TripleDot) || match(TokenType::Comma)) {
        RefPtr<Expression> expression;

        if (match(TokenType::TripleDot)) {
            consume(TokenType::TripleDot);
            expression = create_ast_node<SpreadExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, parse_expression(2));
        } else if (match_expression()) {
            expression = parse_expression(2);
        }

        elements.append(expression);
        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::BracketClose);
    return create_ast_node<ArrayExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(elements));
}

NonnullRefPtr<StringLiteral> Parser::parse_string_literal(const Token& token, bool in_template_literal)
{
    auto rule_start = push_start();
    auto status = Token::StringValueStatus::Ok;
    auto string = token.string_value(status);
    if (status != Token::StringValueStatus::Ok) {
        String message;
        if (status == Token::StringValueStatus::LegacyOctalEscapeSequence) {
            m_state.string_legacy_octal_escape_sequence_in_scope = true;
            if (in_template_literal)
                message = "Octal escape sequence not allowed in template literal";
            else if (m_state.strict_mode)
                message = "Octal escape sequence in string literal not allowed in strict mode";
        } else if (status == Token::StringValueStatus::MalformedHexEscape || status == Token::StringValueStatus::MalformedUnicodeEscape) {
            auto type = status == Token::StringValueStatus::MalformedUnicodeEscape ? "unicode" : "hexadecimal";
            message = String::formatted("Malformed {} escape sequence", type);
        } else if (status == Token::StringValueStatus::UnicodeEscapeOverflow) {
            message = "Unicode code_point must not be greater than 0x10ffff in escape sequence";
        } else {
            VERIFY_NOT_REACHED();
        }

        if (!message.is_empty())
            syntax_error(message, Position { token.line_number(), token.line_column() });
    }

    auto is_use_strict_directive = !in_template_literal && (token.value() == "'use strict'" || token.value() == "\"use strict\"");

    return create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, string, is_use_strict_directive);
}

NonnullRefPtr<TemplateLiteral> Parser::parse_template_literal(bool is_tagged)
{
    auto rule_start = push_start();
    consume(TokenType::TemplateLiteralStart);

    NonnullRefPtrVector<Expression> expressions;
    NonnullRefPtrVector<Expression> raw_strings;

    auto append_empty_string = [this, &rule_start, &expressions, &raw_strings, is_tagged]() {
        auto string_literal = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, "");
        expressions.append(string_literal);
        if (is_tagged)
            raw_strings.append(string_literal);
    };

    if (!match(TokenType::TemplateLiteralString))
        append_empty_string();

    while (!done() && !match(TokenType::TemplateLiteralEnd) && !match(TokenType::UnterminatedTemplateLiteral)) {
        if (match(TokenType::TemplateLiteralString)) {
            auto token = consume();
            expressions.append(parse_string_literal(token, true));
            if (is_tagged)
                raw_strings.append(create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, token.raw_template_value()));
        } else if (match(TokenType::TemplateLiteralExprStart)) {
            consume(TokenType::TemplateLiteralExprStart);
            if (match(TokenType::TemplateLiteralExprEnd)) {
                syntax_error("Empty template literal expression block");
                return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions);
            }

            expressions.append(parse_expression(0));
            if (match(TokenType::UnterminatedTemplateLiteral)) {
                syntax_error("Unterminated template literal");
                return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions);
            }
            consume(TokenType::TemplateLiteralExprEnd);

            if (!match(TokenType::TemplateLiteralString))
                append_empty_string();
        } else {
            expected("Template literal string or expression");
            break;
        }
    }

    if (match(TokenType::UnterminatedTemplateLiteral)) {
        syntax_error("Unterminated template literal");
    } else {
        consume(TokenType::TemplateLiteralEnd);
    }

    if (is_tagged)
        return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions, raw_strings);
    return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions);
}

NonnullRefPtr<Expression> Parser::parse_expression(int min_precedence, Associativity associativity, const Vector<TokenType>& forbidden)
{
    auto rule_start = push_start();
    auto [expression, should_continue_parsing] = parse_primary_expression();
    auto check_for_invalid_object_property = [&](auto& expression) {
        if (is<ObjectExpression>(*expression)) {
            if (auto range = static_cast<ObjectExpression&>(*expression).invalid_property_range(); range.has_value())
                syntax_error("Invalid property in object literal", range->start);
        }
    };
    if (is<Identifier>(*expression) && m_state.current_scope_pusher) {
        auto identifier_instance = static_ptr_cast<Identifier>(expression);
        auto function_scope = m_state.current_scope_pusher->last_function_scope();
        auto function_parent_scope = function_scope ? function_scope->parent_scope() : nullptr;
        bool has_not_been_declared_as_variable = true;
        for (auto scope = m_state.current_scope_pusher; scope != function_parent_scope; scope = scope->parent_scope()) {
            if (scope->has_declaration(identifier_instance->string())) {
                has_not_been_declared_as_variable = false;
                break;
            }
        }

        if (has_not_been_declared_as_variable) {
            if (identifier_instance->string() == "arguments"sv)
                m_state.current_scope_pusher->set_contains_access_to_arguments_object();
        }
    }

    while (match(TokenType::TemplateLiteralStart)) {
        auto template_literal = parse_template_literal(true);
        expression = create_ast_node<TaggedTemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(template_literal));
    }
    if (should_continue_parsing) {
        while (match_secondary_expression(forbidden)) {
            int new_precedence = g_operator_precedence.get(m_state.current_token.type());
            if (new_precedence < min_precedence)
                break;
            if (new_precedence == min_precedence && associativity == Associativity::Left)
                break;
            check_for_invalid_object_property(expression);

            Associativity new_associativity = operator_associativity(m_state.current_token.type());
            expression = parse_secondary_expression(move(expression), new_precedence, new_associativity);
            while (match(TokenType::TemplateLiteralStart) && !is<UpdateExpression>(*expression)) {
                auto template_literal = parse_template_literal(true);
                expression = create_ast_node<TaggedTemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(template_literal));
            }
        }
    }

    if (is<SuperExpression>(*expression))
        syntax_error("'super' keyword unexpected here");

    check_for_invalid_object_property(expression);

    if (is<CallExpression>(*expression) && m_state.current_scope_pusher) {
        auto& callee = static_ptr_cast<CallExpression>(expression)->callee();
        if (is<Identifier>(callee)) {
            auto& identifier_instance = static_cast<Identifier const&>(callee);
            if (identifier_instance.string() == "eval"sv) {
                bool has_not_been_declared_as_variable = true;
                for (auto scope = m_state.current_scope_pusher; scope; scope = scope->parent_scope()) {
                    if (scope->has_declaration(identifier_instance.string())) {
                        has_not_been_declared_as_variable = false;
                        break;
                    }
                }
                if (has_not_been_declared_as_variable)
                    m_state.current_scope_pusher->set_contains_direct_call_to_eval();
            }
        }
    }

    if (match(TokenType::Comma) && min_precedence <= 1) {
        NonnullRefPtrVector<Expression> expressions;
        expressions.append(expression);
        while (match(TokenType::Comma)) {
            consume();
            expressions.append(parse_expression(2));
        }
        expression = create_ast_node<SequenceExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expressions));
    }
    return expression;
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    auto rule_start = push_start();
    switch (m_state.current_token.type()) {
    case TokenType::Plus:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Addition, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PlusEquals:
        return parse_assignment_expression(AssignmentOp::AdditionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Minus:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Subtraction, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::MinusEquals:
        return parse_assignment_expression(AssignmentOp::SubtractionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Asterisk:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Multiplication, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AsteriskEquals:
        return parse_assignment_expression(AssignmentOp::MultiplicationAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Slash:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Division, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::SlashEquals:
        return parse_assignment_expression(AssignmentOp::DivisionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Percent:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Modulo, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PercentEquals:
        return parse_assignment_expression(AssignmentOp::ModuloAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoubleAsterisk:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Exponentiation, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleAsteriskEquals:
        return parse_assignment_expression(AssignmentOp::ExponentiationAssignment, move(lhs), min_precedence, associativity);
    case TokenType::GreaterThan:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::GreaterThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThanEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::GreaterThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThan:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LessThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThanEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LessThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::StrictlyEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::StrictlyInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LooselyEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LooselyInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::In:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::In, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Instanceof:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::InstanceOf, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Ampersand:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::BitwiseAnd, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AmpersandEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseAndAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Pipe:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::BitwiseOr, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PipeEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseOrAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Caret:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::BitwiseXor, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::CaretEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseXorAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ShiftLeft:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LeftShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ShiftLeftEquals:
        return parse_assignment_expression(AssignmentOp::LeftShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ShiftRight:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::RightShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ShiftRightEquals:
        return parse_assignment_expression(AssignmentOp::RightShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::UnsignedShiftRight:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::UnsignedRightShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::UnsignedShiftRightEquals:
        return parse_assignment_expression(AssignmentOp::UnsignedRightShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ParenOpen:
        return parse_call_expression(move(lhs));
    case TokenType::Equals:
        return parse_assignment_expression(AssignmentOp::Assignment, move(lhs), min_precedence, associativity);
    case TokenType::Period:
        consume();
        if (match(TokenType::PrivateIdentifier)) {
            if (!is_private_identifier_valid())
                syntax_error(String::formatted("Reference to undeclared private field or method '{}'", m_state.current_token.value()));
            else if (is<SuperExpression>(*lhs))
                syntax_error(String::formatted("Cannot access private field or method '{}' on super", m_state.current_token.value()));

            return create_ast_node<MemberExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), create_ast_node<PrivateIdentifier>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value()));
        } else if (!match_identifier_name()) {
            expected("IdentifierName");
        }

        return create_ast_node<MemberExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value()));
    case TokenType::BracketOpen: {
        consume(TokenType::BracketOpen);
        auto expression = create_ast_node<MemberExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), parse_expression(0), true);
        consume(TokenType::BracketClose);
        return expression;
    }
    case TokenType::PlusPlus:
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for foo()++
        if (!is<Identifier>(*lhs) && !is<MemberExpression>(*lhs))
            syntax_error(String::formatted("Left-hand side of postfix increment operator must be identifier or member expression, got {}", lhs->class_name()));

        if (m_state.strict_mode && is<Identifier>(*lhs)) {
            auto& identifier = static_cast<Identifier&>(*lhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }

        consume();
        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Increment, move(lhs));
    case TokenType::MinusMinus:
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for foo()--
        if (!is<Identifier>(*lhs) && !is<MemberExpression>(*lhs))
            syntax_error(String::formatted("Left-hand side of postfix increment operator must be identifier or member expression, got {}", lhs->class_name()));

        if (m_state.strict_mode && is<Identifier>(*lhs)) {
            auto& identifier = static_cast<Identifier&>(*lhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }
        consume();
        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Decrement, move(lhs));
    case TokenType::DoubleAmpersand:
        consume();
        return create_ast_node<LogicalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, LogicalOp::And, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleAmpersandEquals:
        return parse_assignment_expression(AssignmentOp::AndAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoublePipe:
        consume();
        return create_ast_node<LogicalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, LogicalOp::Or, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoublePipeEquals:
        return parse_assignment_expression(AssignmentOp::OrAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoubleQuestionMark:
        consume();
        return create_ast_node<LogicalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, LogicalOp::NullishCoalescing, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleQuestionMarkEquals:
        return parse_assignment_expression(AssignmentOp::NullishAssignment, move(lhs), min_precedence, associativity);
    case TokenType::QuestionMark:
        return parse_conditional_expression(move(lhs));
    case TokenType::QuestionMarkPeriod:
        // FIXME: This should allow `(new Foo)?.bar', but as our parser strips parenthesis,
        //        we can't really tell if `lhs' was parenthesized at this point.
        if (is<NewExpression>(lhs.ptr())) {
            syntax_error("'new' cannot be used with optional chaining", position());
            consume();
            return lhs;
        }
        return parse_optional_chain(move(lhs));
    default:
        expected("secondary expression");
        consume();
        return create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

bool Parser::is_private_identifier_valid() const
{
    VERIFY(match(TokenType::PrivateIdentifier));
    if (!m_state.referenced_private_names)
        return false;

    // We might not have hit the declaration yet so class will check this in the end
    m_state.referenced_private_names->set(m_state.current_token.value());
    return true;
}

RefPtr<BindingPattern> Parser::synthesize_binding_pattern(Expression const& expression)
{
    VERIFY(is<ArrayExpression>(expression) || is<ObjectExpression>(expression));
    // Clear any syntax error that has occurred in the range that 'expression' spans.
    m_state.errors.remove_all_matching([range = expression.source_range()](auto const& error) {
        return error.position.has_value() && range.contains(*error.position);
    });
    // Make a parser and parse the source for this expression as a binding pattern.
    auto source = m_state.lexer.source().substring_view(expression.source_range().start.offset - 2, expression.source_range().end.offset - expression.source_range().start.offset);
    Lexer lexer { source, m_state.lexer.filename(), expression.source_range().start.line, expression.source_range().start.column };
    Parser parser { lexer };

    parser.m_state.strict_mode = m_state.strict_mode;
    parser.m_state.allow_super_property_lookup = m_state.allow_super_property_lookup;
    parser.m_state.allow_super_constructor_call = m_state.allow_super_constructor_call;
    parser.m_state.in_function_context = m_state.in_function_context;
    parser.m_state.in_formal_parameter_context = m_state.in_formal_parameter_context;
    parser.m_state.in_generator_function_context = m_state.in_generator_function_context;
    parser.m_state.await_expression_is_valid = m_state.await_expression_is_valid;
    parser.m_state.in_arrow_function_context = m_state.in_arrow_function_context;
    parser.m_state.in_break_context = m_state.in_break_context;
    parser.m_state.in_continue_context = m_state.in_continue_context;
    parser.m_state.string_legacy_octal_escape_sequence_in_scope = m_state.string_legacy_octal_escape_sequence_in_scope;
    parser.m_state.in_class_field_initializer = m_state.in_class_field_initializer;
    parser.m_state.in_class_static_init_block = m_state.in_class_static_init_block;
    parser.m_state.referenced_private_names = m_state.referenced_private_names;

    auto result = parser.parse_binding_pattern(AllowDuplicates::Yes, AllowMemberExpressions::Yes);
    if (parser.has_errors())
        m_state.errors.extend(parser.errors());
    return result;
}

NonnullRefPtr<AssignmentExpression> Parser::parse_assignment_expression(AssignmentOp assignment_op, NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    auto rule_start = push_start();
    VERIFY(match(TokenType::Equals)
        || match(TokenType::PlusEquals)
        || match(TokenType::MinusEquals)
        || match(TokenType::AsteriskEquals)
        || match(TokenType::SlashEquals)
        || match(TokenType::PercentEquals)
        || match(TokenType::DoubleAsteriskEquals)
        || match(TokenType::AmpersandEquals)
        || match(TokenType::PipeEquals)
        || match(TokenType::CaretEquals)
        || match(TokenType::ShiftLeftEquals)
        || match(TokenType::ShiftRightEquals)
        || match(TokenType::UnsignedShiftRightEquals)
        || match(TokenType::DoubleAmpersandEquals)
        || match(TokenType::DoublePipeEquals)
        || match(TokenType::DoubleQuestionMarkEquals));
    consume();

    if (assignment_op == AssignmentOp::Assignment) {
        if (is<ArrayExpression>(*lhs) || is<ObjectExpression>(*lhs)) {
            auto binding_pattern = synthesize_binding_pattern(*lhs);
            if (binding_pattern) {
                auto rhs = parse_expression(min_precedence, associativity);
                return create_ast_node<AssignmentExpression>(
                    { m_state.current_token.filename(), rule_start.position(), position() },
                    assignment_op,
                    binding_pattern.release_nonnull(),
                    move(rhs));
            }
        }
    }
    if (!is<Identifier>(*lhs) && !is<MemberExpression>(*lhs) && !is<CallExpression>(*lhs)) {
        syntax_error("Invalid left-hand side in assignment");
    } else if (m_state.strict_mode && is<Identifier>(*lhs)) {
        auto name = static_cast<const Identifier&>(*lhs).string();
        check_identifier_name_for_assignment_validity(name);
    } else if (m_state.strict_mode && is<CallExpression>(*lhs)) {
        syntax_error("Cannot assign to function call");
    }
    auto rhs = parse_expression(min_precedence, associativity);
    return create_ast_node<AssignmentExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, assignment_op, move(lhs), move(rhs));
}

NonnullRefPtr<Identifier> Parser::parse_identifier()
{
    auto identifier_start = position();
    auto token = consume_identifier();
    if (m_state.in_class_field_initializer && token.value() == "arguments"sv)
        syntax_error("'arguments' is not allowed in class field initializer");
    return create_ast_node<Identifier>(
        { m_state.current_token.filename(), identifier_start, position() },
        token.value());
}

Vector<CallExpression::Argument> Parser::parse_arguments()
{
    Vector<CallExpression::Argument> arguments;

    consume(TokenType::ParenOpen);
    while (match_expression() || match(TokenType::TripleDot)) {
        if (match(TokenType::TripleDot)) {
            consume();
            arguments.append({ parse_expression(2), true });
        } else {
            arguments.append({ parse_expression(2), false });
        }
        if (!match(TokenType::Comma))
            break;
        consume();
    }

    consume(TokenType::ParenClose);
    return arguments;
}

NonnullRefPtr<Expression> Parser::parse_call_expression(NonnullRefPtr<Expression> lhs)
{
    auto rule_start = push_start();
    if (!m_state.allow_super_constructor_call && is<SuperExpression>(*lhs))
        syntax_error("'super' keyword unexpected here");

    auto arguments = parse_arguments();

    if (is<SuperExpression>(*lhs))
        return create_ast_node<SuperCall>({ m_state.current_token.filename(), rule_start.position(), position() }, move(arguments));

    return create_ast_node<CallExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), move(arguments));
}

NonnullRefPtr<NewExpression> Parser::parse_new_expression()
{
    auto rule_start = push_start();
    consume(TokenType::New);

    auto callee = parse_expression(g_operator_precedence.get(TokenType::New), Associativity::Right, { TokenType::ParenOpen, TokenType::QuestionMarkPeriod });
    if (is<ImportCall>(*callee))
        syntax_error("Cannot call new on dynamic import", callee->source_range().start);

    Vector<CallExpression::Argument> arguments;

    if (match(TokenType::ParenOpen)) {
        consume(TokenType::ParenOpen);
        while (match_expression() || match(TokenType::TripleDot)) {
            if (match(TokenType::TripleDot)) {
                consume();
                arguments.append({ parse_expression(2), true });
            } else {
                arguments.append({ parse_expression(2), false });
            }
            if (!match(TokenType::Comma))
                break;
            consume();
        }
        consume(TokenType::ParenClose);
    }

    return create_ast_node<NewExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(callee), move(arguments));
}

NonnullRefPtr<YieldExpression> Parser::parse_yield_expression()
{
    auto rule_start = push_start();

    if (m_state.in_formal_parameter_context)
        syntax_error("'Yield' expression is not allowed in formal parameters of generator function");

    consume(TokenType::Yield);
    RefPtr<Expression> argument;
    bool yield_from = false;

    if (!m_state.current_token.trivia_contains_line_terminator()) {
        if (match(TokenType::Asterisk)) {
            consume();
            yield_from = true;
        }

        if (yield_from || match_expression() || match(TokenType::Class))
            argument = parse_expression(2);
    }

    return create_ast_node<YieldExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(argument), yield_from);
}

NonnullRefPtr<AwaitExpression> Parser::parse_await_expression()
{
    auto rule_start = push_start();

    if (m_state.in_formal_parameter_context)
        syntax_error("'Await' expression is not allowed in formal parameters of an async function");

    consume(TokenType::Await);

    auto precedence = g_operator_precedence.get(TokenType::Await);
    auto associativity = operator_associativity(TokenType::Await);
    auto argument = parse_expression(precedence, associativity);

    return create_ast_node<AwaitExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(argument));
}

NonnullRefPtr<ReturnStatement> Parser::parse_return_statement()
{
    auto rule_start = push_start();
    if (!m_state.in_function_context && !m_state.in_arrow_function_context)
        syntax_error("'return' not allowed outside of a function");

    consume(TokenType::Return);

    // Automatic semicolon insertion: terminate statement when return is followed by newline
    if (m_state.current_token.trivia_contains_line_terminator())
        return create_ast_node<ReturnStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, nullptr);

    if (match_expression()) {
        auto expression = parse_expression(0);
        consume_or_insert_semicolon();
        return create_ast_node<ReturnStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression));
    }

    consume_or_insert_semicolon();
    return create_ast_node<ReturnStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, nullptr);
}

void Parser::parse_statement_list(ScopeNode& output_node, AllowLabelledFunction allow_labelled_functions)
{
    while (!done()) {
        if (match_declaration()) {
            auto declaration = parse_declaration();
            VERIFY(m_state.current_scope_pusher);
            m_state.current_scope_pusher->add_declaration(declaration);
            output_node.append(move(declaration));
        } else if (match_statement()) {
            output_node.append(parse_statement(allow_labelled_functions));
        } else {
            break;
        }
    }
}

// FunctionBody, https://tc39.es/ecma262/#prod-FunctionBody
NonnullRefPtr<FunctionBody> Parser::parse_function_body(Vector<FunctionDeclaration::Parameter> const& parameters, FunctionKind function_kind, bool& contains_direct_call_to_eval)
{
    auto rule_start = push_start();
    auto function_body = create_ast_node<FunctionBody>({ m_state.current_token.filename(), rule_start.position(), position() });
    ScopePusher function_scope = ScopePusher::function_scope(*this, function_body, parameters); // FIXME <-
    consume(TokenType::CurlyOpen);
    auto has_use_strict = parse_directive(function_body);
    bool previous_strict_mode = m_state.strict_mode;
    if (has_use_strict) {
        m_state.strict_mode = true;
        function_body->set_strict_mode();
        if (!is_simple_parameter_list(parameters))
            syntax_error("Illegal 'use strict' directive in function with non-simple parameter list");
    } else if (previous_strict_mode) {
        function_body->set_strict_mode();
    }

    parse_statement_list(function_body);

    // If the function contains 'use strict' we need to check the parameters (again).
    if (function_body->in_strict_mode() || function_kind != FunctionKind::Regular) {
        Vector<StringView> parameter_names;
        for (auto& parameter : parameters) {
            parameter.binding.visit(
                [&](FlyString const& parameter_name) {
                    check_identifier_name_for_assignment_validity(parameter_name, function_body->in_strict_mode());
                    if (function_kind == FunctionKind::Generator && parameter_name == "yield"sv)
                        syntax_error("Parameter name 'yield' not allowed in this context");

                    if (function_kind == FunctionKind::Async && parameter_name == "await"sv)
                        syntax_error("Parameter name 'await' not allowed in this context");

                    for (auto& previous_name : parameter_names) {
                        if (previous_name == parameter_name) {
                            syntax_error(String::formatted("Duplicate parameter '{}' not allowed in strict mode", parameter_name));
                        }
                    }

                    parameter_names.append(parameter_name);
                },
                [&](NonnullRefPtr<BindingPattern> const& binding) {
                    binding->for_each_bound_name([&](auto& bound_name) {
                        if (function_kind == FunctionKind::Generator && bound_name == "yield"sv)
                            syntax_error("Parameter name 'yield' not allowed in this context");

                        if (function_kind == FunctionKind::Async && bound_name == "await"sv)
                            syntax_error("Parameter name 'await' not allowed in this context");

                        for (auto& previous_name : parameter_names) {
                            if (previous_name == bound_name) {
                                syntax_error(String::formatted("Duplicate parameter '{}' not allowed in strict mode", bound_name));
                                break;
                            }
                        }
                        parameter_names.append(bound_name);
                    });
                });
        }
    }

    consume(TokenType::CurlyClose);
    m_state.strict_mode = previous_strict_mode;
    contains_direct_call_to_eval = function_scope.contains_direct_call_to_eval();
    return function_body;
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement()
{
    auto rule_start = push_start();
    auto block = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
    ScopePusher block_scope = ScopePusher::block_scope(*this, block);
    consume(TokenType::CurlyOpen);
    parse_statement_list(block);
    consume(TokenType::CurlyClose);

    return block;
}

template<typename FunctionNodeType>
NonnullRefPtr<FunctionNodeType> Parser::parse_function_node(u8 parse_options)
{
    auto rule_start = push_start();
    VERIFY(!(parse_options & FunctionNodeParseOptions::IsGetterFunction && parse_options & FunctionNodeParseOptions::IsSetterFunction));

    TemporaryChange super_property_access_rollback(m_state.allow_super_property_lookup, !!(parse_options & FunctionNodeParseOptions::AllowSuperPropertyLookup));
    TemporaryChange super_constructor_call_rollback(m_state.allow_super_constructor_call, !!(parse_options & FunctionNodeParseOptions::AllowSuperConstructorCall));
    TemporaryChange break_context_rollback(m_state.in_break_context, false);
    TemporaryChange continue_context_rollback(m_state.in_continue_context, false);
    TemporaryChange class_field_initializer_rollback(m_state.in_class_field_initializer, false);
    TemporaryChange might_need_arguments_object_rollback(m_state.function_might_need_arguments_object, false);

    constexpr auto is_function_expression = IsSame<FunctionNodeType, FunctionExpression>;
    FunctionKind function_kind;
    if ((parse_options & FunctionNodeParseOptions::IsGeneratorFunction) != 0 && (parse_options & FunctionNodeParseOptions::IsAsyncFunction) != 0)
        function_kind = FunctionKind::AsyncGenerator;
    else if ((parse_options & FunctionNodeParseOptions::IsGeneratorFunction) != 0)
        function_kind = FunctionKind::Generator;
    else if ((parse_options & FunctionNodeParseOptions::IsAsyncFunction) != 0)
        function_kind = FunctionKind::Async;
    else
        function_kind = FunctionKind::Regular;
    String name;
    if (parse_options & FunctionNodeParseOptions::CheckForFunctionAndName) {
        if (function_kind == FunctionKind::Regular && match(TokenType::Async) && !next_token().trivia_contains_line_terminator()) {
            function_kind = FunctionKind::Async;
            consume(TokenType::Async);
            parse_options = parse_options | FunctionNodeParseOptions::IsAsyncFunction;
        }
        consume(TokenType::Function);
        if (match(TokenType::Asterisk)) {
            function_kind = function_kind == FunctionKind::Regular ? FunctionKind::Generator : FunctionKind::AsyncGenerator;
            consume(TokenType::Asterisk);
            parse_options = parse_options | FunctionNodeParseOptions::IsGeneratorFunction;
        }

        if (FunctionNodeType::must_have_name() || match_identifier())
            name = consume_identifier().value();
        else if (is_function_expression && (match(TokenType::Yield) || match(TokenType::Await)))
            name = consume().value();

        check_identifier_name_for_assignment_validity(name);

        if (m_state.in_class_static_init_block && name == "await"sv)
            syntax_error("'await' is a reserved word");
    }
    TemporaryChange class_static_initializer_rollback(m_state.in_class_static_init_block, false);
    TemporaryChange generator_change(m_state.in_generator_function_context, function_kind == FunctionKind::Generator || function_kind == FunctionKind::AsyncGenerator);
    TemporaryChange async_change(m_state.await_expression_is_valid, function_kind == FunctionKind::Async || function_kind == FunctionKind::AsyncGenerator);

    consume(TokenType::ParenOpen);
    i32 function_length = -1;
    auto parameters = parse_formal_parameters(function_length, parse_options);
    consume(TokenType::ParenClose);

    if (function_length == -1)
        function_length = parameters.size();

    TemporaryChange function_context_rollback(m_state.in_function_context, true);

    auto old_labels_in_scope = move(m_state.labels_in_scope);
    ScopeGuard guard([&]() {
        m_state.labels_in_scope = move(old_labels_in_scope);
    });

    bool contains_direct_call_to_eval = false;
    auto body = parse_function_body(parameters, function_kind, contains_direct_call_to_eval);

    auto has_strict_directive = body->in_strict_mode();

    if (has_strict_directive)
        check_identifier_name_for_assignment_validity(name, true);

    return create_ast_node<FunctionNodeType>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        name, move(body), move(parameters), function_length,
        function_kind, has_strict_directive, m_state.function_might_need_arguments_object,
        contains_direct_call_to_eval);
}

Vector<FunctionNode::Parameter> Parser::parse_formal_parameters(int& function_length, u8 parse_options)
{
    auto rule_start = push_start();
    bool has_default_parameter = false;
    bool has_rest_parameter = false;
    TemporaryChange formal_parameter_context_change { m_state.in_formal_parameter_context, true };

    Vector<FunctionNode::Parameter> parameters;

    auto consume_identifier_or_binding_pattern = [&]() -> Variant<FlyString, NonnullRefPtr<BindingPattern>> {
        if (auto pattern = parse_binding_pattern(AllowDuplicates::No, AllowMemberExpressions::No))
            return pattern.release_nonnull();

        auto token = consume_identifier();
        auto parameter_name = token.value();

        check_identifier_name_for_assignment_validity(parameter_name);

        for (auto& parameter : parameters) {
            bool has_same_name = parameter.binding.visit(
                [&](FlyString const& name) {
                    return name == parameter_name;
                },
                [&](NonnullRefPtr<BindingPattern> const& bindings) {
                    bool found_duplicate = false;
                    bindings->for_each_bound_name([&](auto& bound_name) {
                        if (bound_name == parameter_name)
                            found_duplicate = true;
                    });
                    return found_duplicate;
                });

            if (!has_same_name)
                continue;

            String message;
            if (parse_options & FunctionNodeParseOptions::IsArrowFunction)
                message = String::formatted("Duplicate parameter '{}' not allowed in arrow function", parameter_name);
            else if (m_state.strict_mode)
                message = String::formatted("Duplicate parameter '{}' not allowed in strict mode", parameter_name);
            else if (has_default_parameter || match(TokenType::Equals))
                message = String::formatted("Duplicate parameter '{}' not allowed in function with default parameter", parameter_name);
            else if (has_rest_parameter)
                message = String::formatted("Duplicate parameter '{}' not allowed in function with rest parameter", parameter_name);
            if (!message.is_empty())
                syntax_error(message, Position { token.line_number(), token.line_column() });
            break;
        }
        return FlyString { token.value() };
    };

    while (match(TokenType::CurlyOpen) || match(TokenType::BracketOpen) || match_identifier() || match(TokenType::TripleDot)) {
        if (parse_options & FunctionNodeParseOptions::IsGetterFunction)
            syntax_error("Getter function must have no arguments");
        if (parse_options & FunctionNodeParseOptions::IsSetterFunction && (parameters.size() >= 1 || match(TokenType::TripleDot)))
            syntax_error("Setter function must have one argument");
        auto is_rest = false;
        if (match(TokenType::TripleDot)) {
            consume();
            has_rest_parameter = true;
            function_length = parameters.size();
            is_rest = true;
        }
        auto parameter = consume_identifier_or_binding_pattern();
        RefPtr<Expression> default_value;
        if (match(TokenType::Equals)) {
            consume();

            if (is_rest)
                syntax_error("Rest parameter may not have a default initializer");

            TemporaryChange change(m_state.in_function_context, true);
            has_default_parameter = true;
            function_length = parameters.size();
            default_value = parse_expression(2);

            bool is_generator = parse_options & FunctionNodeParseOptions::IsGeneratorFunction;
            if ((is_generator || m_state.strict_mode) && default_value && default_value->fast_is<Identifier>() && static_cast<Identifier&>(*default_value).string() == "yield"sv)
                syntax_error("Generator function parameter initializer cannot contain a reference to an identifier named \"yield\"");
        }
        parameters.append({ move(parameter), default_value, is_rest });
        if (match(TokenType::ParenClose) || is_rest)
            break;
        consume(TokenType::Comma);
    }
    if (parse_options & FunctionNodeParseOptions::IsSetterFunction && parameters.is_empty())
        syntax_error("Setter function must have one argument");
    return parameters;
}

static constexpr AK::Array<StringView, 36> s_reserved_words = { "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with" };

RefPtr<BindingPattern> Parser::parse_binding_pattern(Parser::AllowDuplicates allow_duplicates, Parser::AllowMemberExpressions allow_member_expressions)
{
    auto rule_start = push_start();

    TokenType closing_token;
    bool is_object = true;

    if (match(TokenType::BracketOpen)) {
        consume();
        closing_token = TokenType::BracketClose;
        is_object = false;
    } else if (match(TokenType::CurlyOpen)) {
        consume();
        closing_token = TokenType::CurlyClose;
    } else {
        return {};
    }

    Vector<BindingPattern::BindingEntry> entries;

    while (!match(closing_token)) {
        if (!is_object && match(TokenType::Comma)) {
            consume();
            entries.append(BindingPattern::BindingEntry {});
            continue;
        }

        auto is_rest = false;

        if (match(TokenType::TripleDot)) {
            consume();
            is_rest = true;
        }

        decltype(BindingPattern::BindingEntry::name) name = Empty {};
        decltype(BindingPattern::BindingEntry::alias) alias = Empty {};
        RefPtr<Expression> initializer = {};

        if (is_object) {
            bool needs_alias = false;
            if (allow_member_expressions == AllowMemberExpressions::Yes && is_rest) {
                auto expression_position = position();
                auto expression = parse_expression(2, Associativity::Right, { TokenType::Equals });
                if (is<MemberExpression>(*expression))
                    alias = static_ptr_cast<MemberExpression>(expression);
                else if (is<Identifier>(*expression))
                    name = static_ptr_cast<Identifier>(expression);
                else
                    syntax_error("Invalid destructuring assignment target", expression_position);
            } else if (match_identifier_name() || match(TokenType::StringLiteral) || match(TokenType::NumericLiteral)) {
                if (match(TokenType::StringLiteral) || match(TokenType::NumericLiteral))
                    needs_alias = true;

                if (match(TokenType::StringLiteral)) {
                    auto token = consume(TokenType::StringLiteral);
                    auto string_literal = parse_string_literal(token);

                    name = create_ast_node<Identifier>(
                        { m_state.current_token.filename(), rule_start.position(), position() },
                        string_literal->value());
                } else {
                    name = create_ast_node<Identifier>(
                        { m_state.current_token.filename(), rule_start.position(), position() },
                        consume().value());
                }
            } else if (match(TokenType::BracketOpen)) {
                consume();
                auto expression = parse_expression(0);

                name = move(expression);
                consume(TokenType::BracketClose);
            } else {
                expected("identifier or computed property name");
                return {};
            }

            if (!is_rest && match(TokenType::Colon)) {
                consume();
                if (allow_member_expressions == AllowMemberExpressions::Yes) {
                    auto expression_position = position();
                    auto expression = parse_expression(2, Associativity::Right, { TokenType::Equals });
                    if (is<ArrayExpression>(*expression) || is<ObjectExpression>(*expression)) {
                        if (auto synthesized_binding_pattern = synthesize_binding_pattern(*expression))
                            alias = synthesized_binding_pattern.release_nonnull();
                        else
                            syntax_error("Invalid destructuring assignment target", expression_position);
                    } else if (is<MemberExpression>(*expression)) {
                        alias = static_ptr_cast<MemberExpression>(expression);
                    } else if (is<Identifier>(*expression)) {
                        alias = static_ptr_cast<Identifier>(expression);
                    } else {
                        syntax_error("Invalid destructuring assignment target", expression_position);
                    }
                } else if (match(TokenType::CurlyOpen) || match(TokenType::BracketOpen)) {
                    auto binding_pattern = parse_binding_pattern(allow_duplicates, allow_member_expressions);
                    if (!binding_pattern)
                        return {};
                    alias = binding_pattern.release_nonnull();
                } else if (match_identifier_name()) {
                    alias = create_ast_node<Identifier>(
                        { m_state.current_token.filename(), rule_start.position(), position() },
                        consume().value());

                } else {
                    expected("identifier or binding pattern");
                    return {};
                }
            } else if (needs_alias) {
                expected("alias for string or numeric literal name");
                return {};
            }
        } else {
            if (allow_member_expressions == AllowMemberExpressions::Yes) {
                auto expression_position = position();
                auto expression = parse_expression(2, Associativity::Right, { TokenType::Equals });

                if (is<ArrayExpression>(*expression) || is<ObjectExpression>(*expression)) {
                    if (auto synthesized_binding_pattern = synthesize_binding_pattern(*expression))
                        alias = synthesized_binding_pattern.release_nonnull();
                    else
                        syntax_error("Invalid destructuring assignment target", expression_position);
                } else if (is<MemberExpression>(*expression)) {
                    alias = static_ptr_cast<MemberExpression>(expression);
                } else if (is<Identifier>(*expression)) {
                    alias = static_ptr_cast<Identifier>(expression);
                } else {
                    syntax_error("Invalid destructuring assignment target", expression_position);
                }
            } else if (match(TokenType::BracketOpen) || match(TokenType::CurlyOpen)) {
                auto pattern = parse_binding_pattern(allow_duplicates, allow_member_expressions);
                if (!pattern) {
                    expected("binding pattern");
                    return {};
                }
                alias = pattern.release_nonnull();
            } else if (match_identifier_name()) {
                // BindingElement must always have an Empty name field
                auto identifier_name = consume_identifier().value();
                alias = create_ast_node<Identifier>(
                    { m_state.current_token.filename(), rule_start.position(), position() },
                    identifier_name);
            } else {
                expected("identifier or binding pattern");
                return {};
            }
        }

        if (match(TokenType::Equals)) {
            if (is_rest) {
                syntax_error("Unexpected initializer after rest element");
                return {};
            }

            consume();

            initializer = parse_expression(2);
            if (!initializer) {
                expected("initialization expression");
                return {};
            }
        }

        entries.append(BindingPattern::BindingEntry { move(name), move(alias), move(initializer), is_rest });

        if (match(TokenType::Comma)) {
            if (is_rest) {
                syntax_error("Rest element may not be followed by a comma");
                return {};
            }
            consume();
        } else if (is_object && !match(TokenType::CurlyClose)) {
            consume(TokenType::Comma);
        }
    }

    while (!is_object && match(TokenType::Comma))
        consume();

    consume(closing_token);

    auto kind = is_object ? BindingPattern::Kind::Object : BindingPattern::Kind::Array;
    auto pattern = adopt_ref(*new BindingPattern);
    pattern->entries = move(entries);
    pattern->kind = kind;

    Vector<StringView> bound_names;
    pattern->for_each_bound_name([&](auto& name) {
        if (allow_duplicates == AllowDuplicates::No) {
            if (bound_names.contains_slow(name))
                syntax_error("Duplicate parameter names in bindings");
            bound_names.append(name);
        }
        check_identifier_name_for_assignment_validity(name);
    });

    return pattern;
}

NonnullRefPtr<VariableDeclaration> Parser::parse_variable_declaration(bool for_loop_variable_declaration)
{
    auto rule_start = push_start();
    DeclarationKind declaration_kind;

    switch (m_state.current_token.type()) {
    case TokenType::Var:
        declaration_kind = DeclarationKind::Var;
        break;
    case TokenType::Let:
        declaration_kind = DeclarationKind::Let;
        break;
    case TokenType::Const:
        declaration_kind = DeclarationKind::Const;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    consume();

    NonnullRefPtrVector<VariableDeclarator> declarations;
    for (;;) {
        Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>, Empty> target {};
        if (match_identifier()) {
            auto identifier_start = push_start();
            auto name = consume_identifier().value();
            target = create_ast_node<Identifier>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                name);
            check_identifier_name_for_assignment_validity(name);
            if ((declaration_kind == DeclarationKind::Let || declaration_kind == DeclarationKind::Const) && name == "let"sv)
                syntax_error("Lexical binding may not be called 'let'");
        } else if (auto pattern = parse_binding_pattern(declaration_kind != DeclarationKind::Var ? AllowDuplicates::No : AllowDuplicates::Yes, AllowMemberExpressions::No)) {
            target = pattern.release_nonnull();

            if ((declaration_kind == DeclarationKind::Let || declaration_kind == DeclarationKind::Const)) {
                target.get<NonnullRefPtr<BindingPattern>>()->for_each_bound_name([this](auto& name) {
                    if (name == "let"sv)
                        syntax_error("Lexical binding may not be called 'let'");
                });
            }
        } else if (!m_state.in_generator_function_context && match(TokenType::Yield)) {
            if (m_state.strict_mode)
                syntax_error("Identifier must not be a reserved word in strict mode ('yield')");

            target = create_ast_node<Identifier>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                consume().value());
        } else if (!m_state.await_expression_is_valid && match(TokenType::Async)) {
            if (m_program_type == Program::Type::Module)
                syntax_error("Identifier must not be a reserved word in modules ('async')");

            target = create_ast_node<Identifier>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                consume().value());
        }

        if (target.has<Empty>()) {
            expected("identifier or a binding pattern");
            if (match(TokenType::Comma)) {
                consume();
                continue;
            }
            break;
        }

        RefPtr<Expression> init;
        if (match(TokenType::Equals)) {
            consume();
            // In a for loop 'in' can be ambiguous so we do not allow it
            // 14.7.4 The for Statement, https://tc39.es/ecma262/#prod-ForStatement and 14.7.5 The for-in, for-of, and for-await-of Statements, https://tc39.es/ecma262/#prod-ForInOfStatement
            if (for_loop_variable_declaration)
                init = parse_expression(2, Associativity::Right, { TokenType::In });
            else
                init = parse_expression(2);
        } else if (!for_loop_variable_declaration && declaration_kind == DeclarationKind::Const) {
            syntax_error("Missing initializer in 'const' variable declaration");
        } else if (!for_loop_variable_declaration && target.has<NonnullRefPtr<BindingPattern>>()) {
            syntax_error("Missing initializer in destructuring assignment");
        }

        declarations.append(create_ast_node<VariableDeclarator>(
            { m_state.current_token.filename(), rule_start.position(), position() },
            move(target).downcast<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>(),
            move(init)));

        if (match(TokenType::Comma)) {
            consume();
            continue;
        }
        break;
    }
    if (!for_loop_variable_declaration)
        consume_or_insert_semicolon();

    auto declaration = create_ast_node<VariableDeclaration>({ m_state.current_token.filename(), rule_start.position(), position() }, declaration_kind, move(declarations));
    return declaration;
}

NonnullRefPtr<ThrowStatement> Parser::parse_throw_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Throw);

    // Automatic semicolon insertion: terminate statement when throw is followed by newline
    if (m_state.current_token.trivia_contains_line_terminator()) {
        syntax_error("No line break is allowed between 'throw' and its expression");
        return create_ast_node<ThrowStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() }));
    }

    auto expression = parse_expression(0);
    consume_or_insert_semicolon();
    return create_ast_node<ThrowStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression));
}

NonnullRefPtr<BreakStatement> Parser::parse_break_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Break);
    FlyString target_label;
    if (match(TokenType::Semicolon)) {
        consume();
    } else {
        if (!m_state.current_token.trivia_contains_line_terminator() && match_identifier()) {
            target_label = consume().value();

            auto label = m_state.labels_in_scope.find(target_label);
            if (label == m_state.labels_in_scope.end())
                syntax_error(String::formatted("Label '{}' not found", target_label));
        }
        consume_or_insert_semicolon();
    }

    if (target_label.is_null() && !m_state.in_break_context)
        syntax_error("Unlabeled 'break' not allowed outside of a loop or switch statement");

    return create_ast_node<BreakStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, target_label);
}

NonnullRefPtr<ContinueStatement> Parser::parse_continue_statement()
{
    auto rule_start = push_start();
    if (!m_state.in_continue_context)
        syntax_error("'continue' not allow outside of a loop");

    consume(TokenType::Continue);
    FlyString target_label;
    if (match(TokenType::Semicolon)) {
        consume();
        return create_ast_node<ContinueStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, target_label);
    }
    if (!m_state.current_token.trivia_contains_line_terminator() && match_identifier()) {
        auto label_position = position();
        target_label = consume().value();

        auto label = m_state.labels_in_scope.find(target_label);
        if (label == m_state.labels_in_scope.end())
            syntax_error(String::formatted("Label '{}' not found or invalid", target_label));
        else
            label->value = label_position;
    }
    consume_or_insert_semicolon();
    return create_ast_node<ContinueStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, target_label);
}

NonnullRefPtr<ConditionalExpression> Parser::parse_conditional_expression(NonnullRefPtr<Expression> test)
{
    auto rule_start = push_start();
    consume(TokenType::QuestionMark);
    auto consequent = parse_expression(2);
    consume(TokenType::Colon);
    auto alternate = parse_expression(2);
    return create_ast_node<ConditionalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(consequent), move(alternate));
}

NonnullRefPtr<OptionalChain> Parser::parse_optional_chain(NonnullRefPtr<Expression> base)
{
    auto rule_start = push_start();
    Vector<OptionalChain::Reference> chain;
    do {
        if (match(TokenType::QuestionMarkPeriod)) {
            consume(TokenType::QuestionMarkPeriod);
            switch (m_state.current_token.type()) {
            case TokenType::ParenOpen:
                chain.append(OptionalChain::Call { parse_arguments(), OptionalChain::Mode::Optional });
                break;
            case TokenType::BracketOpen:
                consume();
                chain.append(OptionalChain::ComputedReference { parse_expression(0), OptionalChain::Mode::Optional });
                consume(TokenType::BracketClose);
                break;
            case TokenType::PrivateIdentifier: {
                if (!is_private_identifier_valid())
                    syntax_error(String::formatted("Reference to undeclared private field or method '{}'", m_state.current_token.value()));

                auto start = position();
                auto private_identifier = consume();
                chain.append(OptionalChain::PrivateMemberReference {
                    create_ast_node<PrivateIdentifier>({ m_state.current_token.filename(), start, position() }, private_identifier.value()),
                    OptionalChain::Mode::Optional });
                break;
            }
            case TokenType::TemplateLiteralStart:
                // 13.3.1.1 - Static Semantics: Early Errors
                // OptionalChain :
                //        ?. TemplateLiteral
                //        OptionalChain TemplateLiteral
                // This is a hard error.
                syntax_error("Invalid tagged template literal after ?.", position());
                break;
            default:
                if (match_identifier_name()) {
                    auto start = position();
                    auto identifier = consume();
                    chain.append(OptionalChain::MemberReference {
                        create_ast_node<Identifier>({ m_state.current_token.filename(), start, position() }, identifier.value()),
                        OptionalChain::Mode::Optional,
                    });
                } else {
                    syntax_error("Invalid optional chain reference after ?.", position());
                }
                break;
            }
        } else if (match(TokenType::ParenOpen)) {
            chain.append(OptionalChain::Call { parse_arguments(), OptionalChain::Mode::NotOptional });
        } else if (match(TokenType::Period)) {
            consume();
            if (match(TokenType::PrivateIdentifier)) {
                auto start = position();
                auto private_identifier = consume();
                chain.append(OptionalChain::PrivateMemberReference {
                    create_ast_node<PrivateIdentifier>({ m_state.current_token.filename(), start, position() }, private_identifier.value()),
                    OptionalChain::Mode::NotOptional,
                });
            } else if (match_identifier_name()) {
                auto start = position();
                auto identifier = consume();
                chain.append(OptionalChain::MemberReference {
                    create_ast_node<Identifier>({ m_state.current_token.filename(), start, position() }, identifier.value()),
                    OptionalChain::Mode::NotOptional,
                });
            } else {
                expected("an identifier");
                break;
            }
        } else if (match(TokenType::TemplateLiteralStart)) {
            // 13.3.1.1 - Static Semantics: Early Errors
            // OptionalChain :
            //        ?. TemplateLiteral
            //        OptionalChain TemplateLiteral
            syntax_error("Invalid tagged template literal after optional chain", position());
            break;
        } else if (match(TokenType::BracketOpen)) {
            consume();
            chain.append(OptionalChain::ComputedReference { parse_expression(2), OptionalChain::Mode::NotOptional });
            consume(TokenType::BracketClose);
        } else {
            break;
        }
    } while (!done());

    return create_ast_node<OptionalChain>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        move(base),
        move(chain));
}

NonnullRefPtr<TryStatement> Parser::parse_try_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Try);

    auto block = parse_block_statement();

    RefPtr<CatchClause> handler;
    if (match(TokenType::Catch))
        handler = parse_catch_clause();

    RefPtr<BlockStatement> finalizer;
    if (match(TokenType::Finally)) {
        consume();
        finalizer = parse_block_statement();
    }

    if (!handler && !finalizer)
        syntax_error("try statement must have a 'catch' or 'finally' clause");

    return create_ast_node<TryStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(block), move(handler), move(finalizer));
}

NonnullRefPtr<DoWhileStatement> Parser::parse_do_while_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Do);

    auto body = [&]() -> NonnullRefPtr<Statement> {
        TemporaryChange break_change(m_state.in_break_context, true);
        TemporaryChange continue_change(m_state.in_continue_context, true);
        return parse_statement();
    }();

    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);

    // Since ES 2015 a missing semicolon is inserted here, despite the regular ASI rules not applying
    if (match(TokenType::Semicolon))
        consume();

    return create_ast_node<DoWhileStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(body));
}

NonnullRefPtr<WhileStatement> Parser::parse_while_statement()
{
    auto rule_start = push_start();
    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_state.in_break_context, true);
    TemporaryChange continue_change(m_state.in_continue_context, true);
    auto body = parse_statement();

    return create_ast_node<WhileStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(body));
}

NonnullRefPtr<SwitchStatement> Parser::parse_switch_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Switch);

    consume(TokenType::ParenOpen);
    auto determinant = parse_expression(0);
    consume(TokenType::ParenClose);

    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<SwitchCase> cases;

    auto switch_statement = create_ast_node<SwitchStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(determinant));

    ScopePusher switch_scope = ScopePusher::block_scope(*this, switch_statement);

    auto has_default = false;
    while (match(TokenType::Case) || match(TokenType::Default)) {
        if (match(TokenType::Default)) {
            if (has_default)
                syntax_error("Multiple 'default' clauses in switch statement");
            has_default = true;
        }
        switch_statement->add_case(parse_switch_case());
    }

    consume(TokenType::CurlyClose);

    return switch_statement;
}

NonnullRefPtr<WithStatement> Parser::parse_with_statement()
{
    auto rule_start = push_start();
    consume(TokenType::With);
    consume(TokenType::ParenOpen);

    auto object = parse_expression(0);

    consume(TokenType::ParenClose);

    auto body = parse_statement();
    return create_ast_node<WithStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(object), move(body));
}

NonnullRefPtr<SwitchCase> Parser::parse_switch_case()
{
    auto rule_start = push_start();
    RefPtr<Expression> test;

    if (consume().type() == TokenType::Case) {
        test = parse_expression(0);
    }

    consume(TokenType::Colon);

    NonnullRefPtrVector<Statement> consequent;
    TemporaryChange break_change(m_state.in_break_context, true);
    auto switch_case = create_ast_node<SwitchCase>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test));
    parse_statement_list(switch_case);

    return switch_case;
}

NonnullRefPtr<CatchClause> Parser::parse_catch_clause()
{
    auto rule_start = push_start();
    consume(TokenType::Catch);

    FlyString parameter;
    RefPtr<BindingPattern> pattern_parameter;
    auto should_expect_parameter = false;
    if (match(TokenType::ParenOpen)) {
        should_expect_parameter = true;
        consume();
        if (match_identifier_name()
            && (!match(TokenType::Yield) || !m_state.in_generator_function_context)
            && (!match(TokenType::Async) || !m_state.await_expression_is_valid)
            && (!match(TokenType::Await) || !m_state.in_class_static_init_block))
            parameter = consume().value();
        else
            pattern_parameter = parse_binding_pattern(AllowDuplicates::No, AllowMemberExpressions::No);
        consume(TokenType::ParenClose);
    }

    if (should_expect_parameter && parameter.is_empty() && !pattern_parameter)
        expected("an identifier or a binding pattern");

    HashTable<FlyString> bound_names;

    if (pattern_parameter) {
        pattern_parameter->for_each_bound_name(
            [&](auto& name) {
                check_identifier_name_for_assignment_validity(name);
                bound_names.set(name);
            });
    }

    if (!parameter.is_empty()) {
        check_identifier_name_for_assignment_validity(parameter);
        bound_names.set(parameter);
    }

    ScopePusher catch_scope = ScopePusher::catch_scope(*this, pattern_parameter, parameter);
    auto body = parse_block_statement();

    body->for_each_lexically_declared_name([&](auto const& name) {
        if (bound_names.contains(name))
            syntax_error(String::formatted("Identifier '{}' already declared as catch parameter", name));
    });

    if (pattern_parameter) {
        return create_ast_node<CatchClause>(
            { m_state.current_token.filename(), rule_start.position(), position() },
            pattern_parameter.release_nonnull(),
            move(body));
    }

    return create_ast_node<CatchClause>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        move(parameter),
        move(body));
}

NonnullRefPtr<IfStatement> Parser::parse_if_statement()
{
    auto rule_start = push_start();
    auto parse_function_declaration_as_block_statement = [&] {
        // https://tc39.es/ecma262/#sec-functiondeclarations-in-ifstatement-statement-clauses
        // Code matching this production is processed as if each matching occurrence of
        // FunctionDeclaration[?Yield, ?Await, ~Default] was the sole StatementListItem
        // of a BlockStatement occupying that position in the source code.
        VERIFY(match(TokenType::Function));
        auto block = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
        ScopePusher block_scope = ScopePusher::block_scope(*this, *block);
        auto declaration = parse_declaration();
        VERIFY(m_state.current_scope_pusher);
        block_scope.add_declaration(declaration);

        VERIFY(is<FunctionDeclaration>(*declaration));
        auto& function_declaration = static_cast<FunctionDeclaration const&>(*declaration);
        if (function_declaration.kind() == FunctionKind::Generator)
            syntax_error("Generator functions can only be declared in top-level or within a block");
        if (function_declaration.kind() == FunctionKind::Async)
            syntax_error("Async functions can only be declared in top-level or within a block");
        block->append(move(declaration));
        return block;
    };

    consume(TokenType::If);
    consume(TokenType::ParenOpen);
    auto predicate = parse_expression(0);
    consume(TokenType::ParenClose);

    RefPtr<Statement> consequent;
    if (!m_state.strict_mode && match(TokenType::Function))
        consequent = parse_function_declaration_as_block_statement();
    else
        consequent = parse_statement();

    RefPtr<Statement> alternate;
    if (match(TokenType::Else)) {
        consume();
        if (!m_state.strict_mode && match(TokenType::Function))
            alternate = parse_function_declaration_as_block_statement();
        else
            alternate = parse_statement();
    }
    return create_ast_node<IfStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(predicate), move(*consequent), move(alternate));
}

NonnullRefPtr<Statement> Parser::parse_for_statement()
{
    auto rule_start = push_start();
    auto is_await_loop = IsForAwaitLoop::No;

    auto match_of = [&](Token const& token) {
        return token.type() == TokenType::Identifier && token.original_value() == "of"sv;
    };

    auto match_for_in_of = [&]() {
        bool is_of = match_of(m_state.current_token);
        if (is_await_loop == IsForAwaitLoop::Yes) {
            if (!is_of)
                syntax_error("for await loop is only valid with 'of'");
            else if (!m_state.await_expression_is_valid)
                syntax_error("for await loop is only valid in async function or generator");
            return true;
        }

        return match(TokenType::In) || is_of;
    };

    consume(TokenType::For);

    if (match(TokenType::Await)) {
        consume();
        if (!m_state.await_expression_is_valid)
            syntax_error("for-await-of is only allowed in async function context");
        is_await_loop = IsForAwaitLoop::Yes;
    }

    consume(TokenType::ParenOpen);

    Optional<ScopePusher> scope_pusher;

    RefPtr<ASTNode> init;
    if (!match(TokenType::Semicolon)) {
        if (match_variable_declaration()) {
            auto declaration = parse_variable_declaration(true);
            if (declaration->declaration_kind() == DeclarationKind::Var) {
                m_state.current_scope_pusher->add_declaration(declaration);
            } else {
                // This does not follow the normal declaration structure so we need additional checks.
                HashTable<FlyString> bound_names;
                declaration->for_each_bound_name([&](auto const& name) {
                    if (bound_names.set(name) != AK::HashSetResult::InsertedNewEntry)
                        syntax_error(String::formatted("Identifier '{}' already declared in for loop initializer", name), declaration->source_range().start);
                });
            }

            init = move(declaration);
            if (match_for_in_of())
                return parse_for_in_of_statement(*init, is_await_loop);
            if (static_cast<VariableDeclaration&>(*init).declaration_kind() == DeclarationKind::Const) {
                for (auto& variable : static_cast<VariableDeclaration&>(*init).declarations()) {
                    if (!variable.init())
                        syntax_error("Missing initializer in 'const' variable declaration");
                }
            }
        } else if (match_expression()) {
            auto lookahead_token = next_token();
            bool starts_with_async_of = match(TokenType::Async) && match_of(lookahead_token);

            init = parse_expression(0, Associativity::Right, { TokenType::In });
            if (match_for_in_of()) {
                if (is_await_loop != IsForAwaitLoop::Yes
                    && starts_with_async_of && match_of(m_state.current_token))
                    syntax_error("for-of loop may not start with async of");
                return parse_for_in_of_statement(*init, is_await_loop);
            }
        } else {
            syntax_error("Unexpected token in for loop");
        }
    }
    consume(TokenType::Semicolon);

    RefPtr<Expression> test;
    if (!match(TokenType::Semicolon))
        test = parse_expression(0);

    consume(TokenType::Semicolon);

    RefPtr<Expression> update;
    if (!match(TokenType::ParenClose))
        update = parse_expression(0);

    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_state.in_break_context, true);
    TemporaryChange continue_change(m_state.in_continue_context, true);
    ScopePusher for_loop_scope = ScopePusher::for_loop_scope(*this, init);
    auto body = parse_statement();

    return create_ast_node<ForStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(init), move(test), move(update), move(body));
}

NonnullRefPtr<Statement> Parser::parse_for_in_of_statement(NonnullRefPtr<ASTNode> lhs, IsForAwaitLoop is_for_await_loop)
{
    Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> for_declaration = lhs;
    auto rule_start = push_start();

    auto has_annexB_for_in_init_extension = false;

    if (is<VariableDeclaration>(*lhs)) {
        auto& declaration = static_cast<VariableDeclaration&>(*lhs);
        if (declaration.declarations().size() > 1) {
            syntax_error("Multiple declarations not allowed in for..in/of");
        } else if (declaration.declarations().size() < 1) {
            syntax_error("Need exactly one variable declaration in for..in/of");
        } else {
            // AnnexB extension B.3.5 Initializers in ForIn Statement Heads, https://tc39.es/ecma262/#sec-initializers-in-forin-statement-heads
            auto& variable = declaration.declarations().first();
            if (variable.init()) {
                if (m_state.strict_mode || declaration.declaration_kind() != DeclarationKind::Var || !variable.target().has<NonnullRefPtr<Identifier>>())
                    syntax_error("Variable initializer not allowed in for..in/of");
                else
                    has_annexB_for_in_init_extension = true;
            }
        }
    } else if (!lhs->is_identifier() && !is<MemberExpression>(*lhs)) {
        bool valid = false;
        if (is<ObjectExpression>(*lhs) || is<ArrayExpression>(*lhs)) {
            auto synthesized_binding_pattern = synthesize_binding_pattern(static_cast<Expression const&>(*lhs));
            if (synthesized_binding_pattern) {
                for_declaration = synthesized_binding_pattern.release_nonnull();
                valid = true;
            }
        }
        if (!valid)
            syntax_error(String::formatted("Invalid left-hand side in for-loop ('{}')", lhs->class_name()));
    }
    auto in_or_of = consume();
    auto is_in = in_or_of.type() == TokenType::In;

    if (!is_in) {
        if (is<MemberExpression>(*lhs)) {
            auto& member = static_cast<MemberExpression const&>(*lhs);
            if (member.object().is_identifier() && static_cast<Identifier const&>(member.object()).string() == "let"sv)
                syntax_error("For of statement may not start with let.");
        }
        if (has_annexB_for_in_init_extension)
            syntax_error("Variable initializer not allowed in for..of", rule_start.position());
    }

    auto rhs = parse_expression(is_in ? 0 : 2);
    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_state.in_break_context, true);
    TemporaryChange continue_change(m_state.in_continue_context, true);
    ScopePusher for_loop_scope = ScopePusher::for_loop_scope(*this, lhs);
    auto body = parse_statement();
    if (is_in)
        return create_ast_node<ForInStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(for_declaration), move(rhs), move(body));
    if (is_for_await_loop == IsForAwaitLoop::Yes)
        return create_ast_node<ForAwaitOfStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(for_declaration), move(rhs), move(body));
    return create_ast_node<ForOfStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(for_declaration), move(rhs), move(body));
}

NonnullRefPtr<DebuggerStatement> Parser::parse_debugger_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Debugger);
    consume_or_insert_semicolon();
    return create_ast_node<DebuggerStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
}

bool Parser::match(TokenType type) const
{
    return m_state.current_token.type() == type;
}

bool Parser::match_expression() const
{
    auto type = m_state.current_token.type();
    if (type == TokenType::Import) {
        auto lookahead_token = next_token();
        return lookahead_token.type() == TokenType::Period || lookahead_token.type() == TokenType::ParenOpen;
    }

    return type == TokenType::BoolLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::BigIntLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::TemplateLiteralStart
        || type == TokenType::NullLiteral
        || match_identifier()
        || type == TokenType::PrivateIdentifier
        || type == TokenType::Await
        || type == TokenType::New
        || type == TokenType::Class
        || type == TokenType::CurlyOpen
        || type == TokenType::BracketOpen
        || type == TokenType::ParenOpen
        || type == TokenType::Function
        || type == TokenType::Async
        || type == TokenType::This
        || type == TokenType::Super
        || type == TokenType::RegexLiteral
        || type == TokenType::Slash       // Wrongly recognized regex by lexer
        || type == TokenType::SlashEquals // Wrongly recognized regex by lexer (/=a/ is a valid regex)
        || type == TokenType::Yield
        || match_unary_prefixed_expression();
}

bool Parser::match_unary_prefixed_expression() const
{
    auto type = m_state.current_token.type();
    return type == TokenType::PlusPlus
        || type == TokenType::MinusMinus
        || type == TokenType::ExclamationMark
        || type == TokenType::Tilde
        || type == TokenType::Plus
        || type == TokenType::Minus
        || type == TokenType::Typeof
        || type == TokenType::Void
        || type == TokenType::Delete;
}

bool Parser::match_secondary_expression(const Vector<TokenType>& forbidden) const
{
    auto type = m_state.current_token.type();
    if (forbidden.contains_slow(type))
        return false;
    return type == TokenType::Plus
        || type == TokenType::PlusEquals
        || type == TokenType::Minus
        || type == TokenType::MinusEquals
        || type == TokenType::Asterisk
        || type == TokenType::AsteriskEquals
        || type == TokenType::Slash
        || type == TokenType::SlashEquals
        || type == TokenType::Percent
        || type == TokenType::PercentEquals
        || type == TokenType::DoubleAsterisk
        || type == TokenType::DoubleAsteriskEquals
        || type == TokenType::Equals
        || type == TokenType::EqualsEqualsEquals
        || type == TokenType::ExclamationMarkEqualsEquals
        || type == TokenType::EqualsEquals
        || type == TokenType::ExclamationMarkEquals
        || type == TokenType::GreaterThan
        || type == TokenType::GreaterThanEquals
        || type == TokenType::LessThan
        || type == TokenType::LessThanEquals
        || type == TokenType::ParenOpen
        || type == TokenType::Period
        || type == TokenType::BracketOpen
        || (type == TokenType::PlusPlus && !m_state.current_token.trivia_contains_line_terminator())
        || (type == TokenType::MinusMinus && !m_state.current_token.trivia_contains_line_terminator())
        || type == TokenType::In
        || type == TokenType::Instanceof
        || type == TokenType::QuestionMark
        || type == TokenType::Ampersand
        || type == TokenType::AmpersandEquals
        || type == TokenType::Pipe
        || type == TokenType::PipeEquals
        || type == TokenType::Caret
        || type == TokenType::CaretEquals
        || type == TokenType::ShiftLeft
        || type == TokenType::ShiftLeftEquals
        || type == TokenType::ShiftRight
        || type == TokenType::ShiftRightEquals
        || type == TokenType::UnsignedShiftRight
        || type == TokenType::UnsignedShiftRightEquals
        || type == TokenType::DoubleAmpersand
        || type == TokenType::DoubleAmpersandEquals
        || type == TokenType::DoublePipe
        || type == TokenType::DoublePipeEquals
        || type == TokenType::DoubleQuestionMark
        || type == TokenType::DoubleQuestionMarkEquals
        || type == TokenType::QuestionMarkPeriod;
}

bool Parser::match_statement() const
{
    auto type = m_state.current_token.type();
    return match_expression()
        || type == TokenType::Return
        || type == TokenType::Yield
        || type == TokenType::Do
        || type == TokenType::If
        || type == TokenType::Throw
        || type == TokenType::Try
        || type == TokenType::While
        || type == TokenType::With
        || type == TokenType::For
        || type == TokenType::CurlyOpen
        || type == TokenType::Switch
        || type == TokenType::Break
        || type == TokenType::Continue
        || type == TokenType::Var
        || type == TokenType::Debugger
        || type == TokenType::Semicolon;
}

bool Parser::match_export_or_import() const
{
    auto type = m_state.current_token.type();
    return type == TokenType::Export
        || type == TokenType::Import;
}

bool Parser::match_declaration() const
{
    auto type = m_state.current_token.type();

    if (type == TokenType::Let && !m_state.strict_mode) {
        return try_match_let_declaration();
    }

    if (type == TokenType::Async) {
        auto lookahead_token = next_token();
        return lookahead_token.type() == TokenType::Function && !lookahead_token.trivia_contains_line_terminator();
    }

    return type == TokenType::Function
        || type == TokenType::Class
        || type == TokenType::Const
        || type == TokenType::Let;
}

Token Parser::next_token() const
{
    Lexer lookahead_lexer = m_state.lexer;

    auto token_after = lookahead_lexer.next();

    return token_after;
}

bool Parser::try_match_let_declaration() const
{
    VERIFY(m_state.current_token.type() == TokenType::Let);
    auto token_after = next_token();

    if (token_after.is_identifier_name() && token_after.value() != "in"sv)
        return true;

    if (token_after.type() == TokenType::CurlyOpen || token_after.type() == TokenType::BracketOpen)
        return true;

    return false;
}

bool Parser::match_variable_declaration() const
{
    auto type = m_state.current_token.type();

    if (type == TokenType::Let && !m_state.strict_mode) {
        return try_match_let_declaration();
    }

    return type == TokenType::Var
        || type == TokenType::Let
        || type == TokenType::Const;
}

bool Parser::match_identifier() const
{
    if (m_state.current_token.type() == TokenType::EscapedKeyword) {
        if (m_state.current_token.value() == "let"sv)
            return !m_state.strict_mode;
        if (m_state.current_token.value() == "yield"sv)
            return !m_state.strict_mode && !m_state.in_generator_function_context;
        if (m_state.current_token.value() == "await"sv)
            return m_program_type != Program::Type::Module && !m_state.await_expression_is_valid && !m_state.in_class_static_init_block;
        return true;
    }

    return m_state.current_token.type() == TokenType::Identifier
        || m_state.current_token.type() == TokenType::Async
        || (m_state.current_token.type() == TokenType::Let && !m_state.strict_mode)
        || (m_state.current_token.type() == TokenType::Await && m_program_type != Program::Type::Module && !m_state.await_expression_is_valid && !m_state.in_class_static_init_block)
        || (m_state.current_token.type() == TokenType::Yield && !m_state.in_generator_function_context && !m_state.strict_mode); // See note in Parser::parse_identifier().
}

bool Parser::match_identifier_name() const
{
    return m_state.current_token.is_identifier_name();
}

bool Parser::match_property_key() const
{
    auto type = m_state.current_token.type();
    return match_identifier_name()
        || type == TokenType::BracketOpen
        || type == TokenType::StringLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::BigIntLiteral;
}

bool Parser::done() const
{
    return match(TokenType::Eof);
}

Token Parser::consume()
{
    auto old_token = m_state.current_token;
    m_state.current_token = m_state.lexer.next();
    // NOTE: This is the bare minimum needed to decide whether we might need an arguments object
    // in a function expression or declaration. ("might" because the AST implements some further
    // conditions from the spec that rule out the need for allocating one)
    if (old_token.type() == TokenType::Identifier && old_token.value().is_one_of("arguments"sv, "eval"sv))
        m_state.function_might_need_arguments_object = true;
    return old_token;
}

void Parser::consume_or_insert_semicolon()
{
    // Semicolon was found and will be consumed
    if (match(TokenType::Semicolon)) {
        consume();
        return;
    }
    // Insert semicolon if...
    // ...token is preceded by one or more newlines
    if (m_state.current_token.trivia_contains_line_terminator())
        return;
    // ...token is a closing curly brace
    if (match(TokenType::CurlyClose))
        return;
    // ...token is eof
    if (match(TokenType::Eof))
        return;

    // No rule for semicolon insertion applies -> syntax error
    expected("Semicolon");
}

Token Parser::consume_identifier()
{
    if (match(TokenType::Identifier))
        return consume(TokenType::Identifier);

    if (match(TokenType::EscapedKeyword))
        return consume(TokenType::EscapedKeyword);

    // Note that 'let' is not a reserved keyword, but our lexer considers it such
    // As it's pretty nice to have that (for syntax highlighting and such), we'll
    // special-case it here instead.
    if (match(TokenType::Let)) {
        if (m_state.strict_mode)
            syntax_error("'let' is not allowed as an identifier in strict mode");
        return consume();
    }

    if (match(TokenType::Yield)) {
        if (m_state.strict_mode || m_state.in_generator_function_context)
            syntax_error("Identifier must not be a reserved word in strict mode ('yield')");
        return consume();
    }

    if (match(TokenType::Await)) {
        if (m_program_type == Program::Type::Module || m_state.await_expression_is_valid || m_state.in_class_static_init_block)
            syntax_error("Identifier must not be a reserved word in modules ('await')");
        return consume();
    }

    if (match(TokenType::Async))
        return consume();

    expected("Identifier");
    return consume();
}

// https://tc39.es/ecma262/#prod-IdentifierReference
Token Parser::consume_identifier_reference()
{
    if (match(TokenType::Identifier))
        return consume(TokenType::Identifier);

    if (match(TokenType::EscapedKeyword)) {
        auto name = m_state.current_token.value();
        if (m_state.strict_mode && (name == "let"sv || name == "yield"sv))
            syntax_error(String::formatted("'{}' is not allowed as an identifier in strict mode", name));
        if (m_program_type == Program::Type::Module && name == "await"sv)
            syntax_error("'await' is not allowed as an identifier in module");

        return consume();
    }

    // See note in Parser::parse_identifier().
    if (match(TokenType::Let)) {
        if (m_state.strict_mode)
            syntax_error("'let' is not allowed as an identifier in strict mode");
        return consume();
    }

    if (match(TokenType::Yield)) {
        if (m_state.strict_mode)
            syntax_error("Identifier reference may not be 'yield' in strict mode");
        return consume();
    }

    if (match(TokenType::Await)) {
        if (m_program_type == Program::Type::Module)
            syntax_error("'await' is not allowed as an identifier in module");
        return consume();
    }

    if (match(TokenType::Async))
        return consume();

    expected(Token::name(TokenType::Identifier));
    return consume();
}

Token Parser::consume(TokenType expected_type)
{
    if (!match(expected_type)) {
        expected(Token::name(expected_type));
    }
    auto token = consume();
    if (expected_type == TokenType::Identifier) {
        if (m_state.strict_mode && is_strict_reserved_word(token.value()))
            syntax_error(String::formatted("Identifier must not be a reserved word in strict mode ('{}')", token.value()));
    }
    return token;
}

Token Parser::consume_and_validate_numeric_literal()
{
    auto is_unprefixed_octal_number = [](StringView value) {
        return value.length() > 1 && value[0] == '0' && is_ascii_digit(value[1]);
    };
    auto literal_start = position();
    auto token = consume(TokenType::NumericLiteral);
    if (m_state.strict_mode && is_unprefixed_octal_number(token.value()))
        syntax_error("Unprefixed octal number not allowed in strict mode", literal_start);
    if (match_identifier_name() && m_state.current_token.trivia().is_empty())
        syntax_error("Numeric literal must not be immediately followed by identifier");
    return token;
}

void Parser::expected(const char* what)
{
    auto message = m_state.current_token.message();
    if (message.is_empty())
        message = String::formatted("Unexpected token {}. Expected {}", m_state.current_token.name(), what);
    syntax_error(message);
}

Position Parser::position() const
{
    return {
        m_state.current_token.line_number(),
        m_state.current_token.line_column(),
        m_state.current_token.offset(),
    };
}

bool Parser::try_parse_arrow_function_expression_failed_at_position(const Position& position) const
{
    auto it = m_token_memoizations.find(position);
    if (it == m_token_memoizations.end())
        return false;

    return (*it).value.try_parse_arrow_function_expression_failed;
}

void Parser::set_try_parse_arrow_function_expression_failed_at_position(const Position& position, bool failed)
{
    m_token_memoizations.set(position, { failed });
}

void Parser::syntax_error(const String& message, Optional<Position> position)
{
    if (!position.has_value())
        position = this->position();
    m_state.errors.append({ message, position });
}

void Parser::save_state()
{
    m_saved_state.append(m_state);
}

void Parser::load_state()
{
    VERIFY(!m_saved_state.is_empty());
    m_state = m_saved_state.take_last();
}

void Parser::discard_saved_state()
{
    m_saved_state.take_last();
}

void Parser::check_identifier_name_for_assignment_validity(StringView name, bool force_strict)
{
    // FIXME: this is now called from multiple places maybe the error message should be dynamic?
    if (any_of(s_reserved_words, [&](auto& value) { return name == value; })) {
        syntax_error("Binding pattern target may not be a reserved word");
    } else if (m_state.strict_mode || force_strict) {
        if (name.is_one_of("arguments"sv, "eval"sv))
            syntax_error("Binding pattern target may not be called 'arguments' or 'eval' in strict mode");
        else if (is_strict_reserved_word(name))
            syntax_error(String::formatted("Binding pattern target may not be called '{}' in strict mode", name));
    }
}

NonnullRefPtr<ImportStatement> Parser::parse_import_statement(Program& program)
{
    auto rule_start = push_start();
    if (program.type() != Program::Type::Module)
        syntax_error("Cannot use import statement outside a module");

    consume(TokenType::Import);

    if (match(TokenType::StringLiteral)) {
        auto module_name = consume(TokenType::StringLiteral).value();
        return create_ast_node<ImportStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, module_name);
    }

    auto match_imported_binding = [&] {
        return match_identifier() || match(TokenType::Yield) || match(TokenType::Await);
    };

    auto match_as = [&] {
        return match(TokenType::Identifier) && m_state.current_token.original_value() == "as"sv;
    };

    bool continue_parsing = true;

    struct ImportWithLocation {
        ImportStatement::ImportEntry entry;
        Position position;
    };

    Vector<ImportWithLocation> entries_with_location;

    if (match_imported_binding()) {
        auto id_position = position();
        auto bound_name = consume().value();
        entries_with_location.append({ { "default", bound_name }, id_position });

        if (match(TokenType::Comma)) {
            consume(TokenType::Comma);
        } else {
            continue_parsing = false;
        }
    }

    if (!continue_parsing) {
        // skip the rest
    } else if (match(TokenType::Asterisk)) {
        consume(TokenType::Asterisk);

        if (!match_as())
            syntax_error(String::formatted("Unexpected token: {}", m_state.current_token.name()));

        consume(TokenType::Identifier);

        if (match_imported_binding()) {
            auto namespace_position = position();
            auto namespace_name = consume().value();
            entries_with_location.append({ { "*", namespace_name }, namespace_position });
        } else {
            syntax_error(String::formatted("Unexpected token: {}", m_state.current_token.name()));
        }

    } else if (match(TokenType::CurlyOpen)) {
        consume(TokenType::CurlyOpen);

        while (!done() && !match(TokenType::CurlyClose)) {
            if (match_identifier_name()) {
                auto require_as = !match_imported_binding();
                auto name_position = position();
                auto name = consume().value();

                if (match_as()) {
                    consume(TokenType::Identifier);

                    auto alias_position = position();
                    auto alias = consume_identifier().value();
                    check_identifier_name_for_assignment_validity(alias);

                    entries_with_location.append({ { name, alias }, alias_position });
                } else if (require_as) {
                    syntax_error(String::formatted("Unexpected reserved word '{}'", name));
                } else {
                    check_identifier_name_for_assignment_validity(name);

                    entries_with_location.append({ { name, name }, name_position });
                }
            } else {
                expected("identifier");
                break;
            }

            if (!match(TokenType::Comma))
                break;

            consume(TokenType::Comma);
        }

        consume(TokenType::CurlyClose);
    } else {
        expected("import clauses");
    }

    auto from_statement = consume(TokenType::Identifier).original_value();
    if (from_statement != "from"sv)
        syntax_error(String::formatted("Expected 'from' got {}", from_statement));

    auto module_name = consume(TokenType::StringLiteral).value();

    Vector<ImportStatement::ImportEntry> entries;
    entries.ensure_capacity(entries_with_location.size());

    for (auto& entry : entries_with_location) {
        for (auto& import_statement : program.imports()) {
            if (import_statement.has_bound_name(entry.entry.local_name))
                syntax_error(String::formatted("Identifier '{}' already declared", entry.entry.local_name), entry.position);
        }

        for (auto& new_entry : entries) {
            if (new_entry.local_name == entry.entry.local_name)
                syntax_error(String::formatted("Identifier '{}' already declared", entry.entry.local_name), entry.position);
        }

        entries.append(move(entry.entry));
    }

    return create_ast_node<ImportStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, module_name, move(entries));
}

NonnullRefPtr<ExportStatement> Parser::parse_export_statement(Program& program)
{
    auto rule_start = push_start();
    if (program.type() != Program::Type::Module)
        syntax_error("Cannot use export statement outside a module");

    auto match_as = [&] {
        return match(TokenType::Identifier) && m_state.current_token.original_value() == "as"sv;
    };

    auto match_from = [&] {
        return match(TokenType::Identifier) && m_state.current_token.original_value() == "from"sv;
    };

    auto match_default = [&] {
        return match(TokenType::Default) && m_state.current_token.original_value() == "default"sv;
    };

    consume(TokenType::Export);

    struct EntryAndLocation {
        ExportStatement::ExportEntry entry;
        Position position;

        void to_module_request(String from_module)
        {
            entry.kind = ExportStatement::ExportEntry::Kind::ModuleRequest;
            entry.module_request = from_module;
        }
    };

    Vector<EntryAndLocation> entries_with_location;

    RefPtr<ASTNode> expression = {};

    if (match_default()) {
        auto default_position = position();
        consume(TokenType::Default);

        String local_name;

        if (match(TokenType::Class)) {
            auto class_expression = parse_class_expression(false);
            local_name = class_expression->name();
            expression = move(class_expression);
        } else if (match(TokenType::Function) || (match(TokenType::Async) && next_token().type() == TokenType::Function)) {
            auto func_expr = parse_function_node<FunctionExpression>();
            local_name = func_expr->name();
            expression = move(func_expr);
        } else if (match_expression()) {
            expression = parse_expression(2);
            consume_or_insert_semicolon();
            local_name = "*default*";
        } else {
            expected("Declaration or assignment expression");
        }

        entries_with_location.append({ { "default", local_name }, default_position });
    } else {
        enum FromSpecifier {
            NotAllowed,
            Optional,
            Required
        } check_for_from { NotAllowed };

        if (match(TokenType::Asterisk)) {
            auto asterisk_position = position();
            consume(TokenType::Asterisk);

            if (match_as()) {
                consume(TokenType::Identifier);
                if (match_identifier_name()) {
                    auto namespace_position = position();
                    auto exported_name = consume().value();
                    entries_with_location.append({ { exported_name, "*" }, namespace_position });
                } else {
                    expected("identifier");
                }
            } else {
                entries_with_location.append({ { {}, "*" }, asterisk_position });
            }
            check_for_from = Required;
        } else if (match_declaration()) {
            auto decl_position = position();
            auto declaration = parse_declaration();
            m_state.current_scope_pusher->add_declaration(declaration);
            if (is<FunctionDeclaration>(*declaration)) {
                auto& func = static_cast<FunctionDeclaration&>(*declaration);
                entries_with_location.append({ { func.name(), func.name() }, func.source_range().start });
            } else if (is<ClassDeclaration>(*declaration)) {
                auto& class_declaration = static_cast<ClassDeclaration&>(*declaration);
                entries_with_location.append({ { class_declaration.name(), class_declaration.name() }, class_declaration.source_range().start });
            } else {
                VERIFY(is<VariableDeclaration>(*declaration));
                auto& variables = static_cast<VariableDeclaration&>(*declaration);
                for (auto& decl : variables.declarations()) {
                    decl.target().visit(
                        [&](NonnullRefPtr<Identifier> const& identifier) {
                            entries_with_location.append({ { identifier->string(), identifier->string() }, identifier->source_range().start });
                        },
                        [&](NonnullRefPtr<BindingPattern> const& binding) {
                            binding->for_each_bound_name([&](auto& name) {
                                entries_with_location.append({ { name, name }, decl_position });
                            });
                        });
                }
            }
            expression = declaration;
        } else if (match(TokenType::Var)) {
            auto variable_position = position();
            auto variable_declaration = parse_variable_declaration();
            for (auto& decl : variable_declaration->declarations()) {
                decl.target().visit(
                    [&](NonnullRefPtr<Identifier> const& identifier) {
                        entries_with_location.append({ { identifier->string(), identifier->string() }, identifier->source_range().start });
                    },
                    [&](NonnullRefPtr<BindingPattern> const& binding) {
                        binding->for_each_bound_name([&](auto& name) {
                            entries_with_location.append({ { name, name }, variable_position });
                        });
                    });
            }
            expression = variable_declaration;
        } else if (match(TokenType::CurlyOpen)) {
            consume(TokenType::CurlyOpen);

            while (!done() && !match(TokenType::CurlyClose)) {
                if (match_identifier_name()) {
                    auto identifier_position = position();
                    auto identifier = consume().value();

                    if (match_as()) {
                        consume(TokenType::Identifier);
                        if (match_identifier_name()) {
                            auto export_name = consume().value();
                            entries_with_location.append({ { export_name, identifier }, identifier_position });
                        } else {
                            expected("identifier name");
                        }
                    } else {
                        entries_with_location.append({ { identifier, identifier }, identifier_position });
                    }
                } else {
                    expected("identifier");
                    break;
                }

                if (!match(TokenType::Comma))
                    break;

                consume(TokenType::Comma);
            }

            consume(TokenType::CurlyClose);
            check_for_from = Optional;
        } else {
            syntax_error("Unexpected token 'export'", rule_start.position());
        }

        if (check_for_from != NotAllowed && match_from()) {
            consume(TokenType::Identifier);
            if (match(TokenType::StringLiteral)) {
                auto from_specifier = consume().value();
                for (auto& entry : entries_with_location)
                    entry.to_module_request(from_specifier);
            } else {
                expected("ModuleSpecifier");
            }
        } else if (check_for_from == Required) {
            expected("from");
        }

        if (check_for_from != NotAllowed)
            consume_or_insert_semicolon();
    }

    Vector<ExportStatement::ExportEntry> entries;
    entries.ensure_capacity(entries_with_location.size());

    for (auto& entry : entries_with_location) {
        for (auto& export_statement : program.exports()) {
            if (export_statement.has_export(entry.entry.export_name))
                syntax_error(String::formatted("Duplicate export with name: '{}'", entry.entry.export_name), entry.position);
        }

        for (auto& new_entry : entries) {
            if (new_entry.export_name == entry.entry.export_name)
                syntax_error(String::formatted("Duplicate export with name: '{}'", entry.entry.export_name), entry.position);
        }

        entries.append(move(entry.entry));
    }

    return create_ast_node<ExportStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(entries));
}
}
