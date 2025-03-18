/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include "Shell.h"
#include <AK/AllOf.h>
#include <AK/GenericLexer.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopedValueRollback.h>
#include <AK/TemporaryChange.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

#define TRY_OR_THROW_PARSE_ERROR(expr) ({                                              \
    /* Ignore -Wshadow to allow nesting the macro. */                                  \
    AK_IGNORE_DIAGNOSTIC("-Wshadow",                                                   \
                         auto&& _value_or_error = expr;)                               \
    if (_value_or_error.is_error()) {                                                  \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                               \
                             auto _error = _value_or_error.release_error();)           \
        if (_error.is_errno() && _error.code() == ENOMEM)                              \
            return create<AST::SyntaxError>("OOM"_string);                             \
        return create<AST::SyntaxError>(MUST(String::formatted("Error: {}", _error))); \
    }                                                                                  \
    _value_or_error.release_value();                                                   \
})

#define TRY_OR_RESOLVE_TO_ERROR_STRING(expr) ({                                   \
    /* Ignore -Wshadow to allow nesting the macro. */                             \
    AK_IGNORE_DIAGNOSTIC("-Wshadow",                                              \
                         auto&& _value_or_error = expr;                           \
                         String _string_value;)                                   \
    if (_value_or_error.is_error()) {                                             \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                          \
                             auto _error = _value_or_error.release_error();)      \
        if (_error.is_errno() && _error.code() == ENOMEM)                         \
            _string_value = "OOM"_string;                                         \
        else                                                                      \
            _string_value = MUST(String::formatted("Error: {}", _error));         \
    }                                                                             \
    _value_or_error.is_error() ? _string_value : _value_or_error.release_value(); \
})

#define TRY_OR(expr, catch_expr) ({                                          \
    /* Ignore -Wshadow to allow nesting the macro. */                        \
    AK_IGNORE_DIAGNOSTIC("-Wshadow",                                         \
                         auto&& _value_or_error = expr;)                     \
    if (_value_or_error.is_error()) {                                        \
        AK_IGNORE_DIAGNOSTIC("-Wshadow",                                     \
                             auto _error = _value_or_error.release_error();) \
        catch_expr;                                                          \
    }                                                                        \
    _value_or_error.release_value();                                         \
})

namespace Shell {

Parser::SavedOffset Parser::save_offset() const
{
    return { m_offset, m_line };
}

char Parser::peek()
{
    if (at_end())
        return 0;

    VERIFY(m_offset < m_input.length());

    auto ch = m_input[m_offset];
    if (ch == '\\' && m_input.length() > m_offset + 1 && m_input[m_offset + 1] == '\n') {
        m_offset += 2;
        ++m_line.line_number;
        m_line.line_column = 0;
        return peek();
    }

    return ch;
}

char Parser::consume()
{
    if (at_end())
        return 0;

    auto ch = peek();
    ++m_offset;

    if (ch == '\n') {
        ++m_line.line_number;
        m_line.line_column = 0;
    } else {
        ++m_line.line_column;
    }

    return ch;
}

bool Parser::expect(char ch)
{
    return expect(StringView { &ch, 1 });
}

bool Parser::expect(StringView expected)
{
    auto offset_at_start = m_offset;
    auto line_at_start = line();

    if (expected.length() + m_offset > m_input.length())
        return false;

    for (auto& c : expected) {
        if (peek() != c) {
            restore_to(offset_at_start, line_at_start);
            return false;
        }

        consume();
    }

    return true;
}

template<typename A, typename... Args>
NonnullRefPtr<A> Parser::create(Args&&... args)
{
    return adopt_ref(*new A(AST::Position { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() }, forward<Args>(args)...));
}

[[nodiscard]] OwnPtr<Parser::ScopedOffset> Parser::push_start()
{
    return make<ScopedOffset>(m_rule_start_offsets, m_rule_start_lines, m_offset, m_line.line_number, m_line.line_column);
}

Parser::Offset Parser::current_position()
{
    return Offset { m_offset, { m_line.line_number, m_line.line_column } };
}

static constexpr bool is_whitespace(char c)
{
    return c == ' ' || c == '\t';
}

static constexpr bool is_digit(char c)
{
    return c <= '9' && c >= '0';
}

static constexpr auto is_not(char c)
{
    return [c](char ch) { return ch != c; };
}

static inline char to_byte(char a, char b)
{
    char buf[3] { a, b, 0 };
    return strtol(buf, nullptr, 16);
}

RefPtr<AST::Node> Parser::parse()
{
    m_offset = 0;
    m_line = { 0, 0 };

    auto toplevel = parse_toplevel();

    if (m_offset < m_input.length()) {
        // Parsing stopped midway, this is a syntax error.
        auto error_start = push_start();
        while (!at_end())
            consume();
        auto syntax_error_node = create<AST::SyntaxError>("Unexpected tokens past the end"_string);
        if (!toplevel)
            toplevel = move(syntax_error_node);
        else if (!toplevel->is_syntax_error())
            toplevel->set_is_syntax_error(*syntax_error_node);
    }

    return toplevel;
}

RefPtr<AST::Node> Parser::parse_as_single_expression()
{
    auto input = Shell::escape_token_for_double_quotes(m_input);
    Parser parser { input };
    return parser.parse_expression();
}

Vector<NonnullRefPtr<AST::Node>> Parser::parse_as_multiple_expressions()
{
    Vector<NonnullRefPtr<AST::Node>> nodes;
    for (;;) {
        consume_while(is_whitespace);
        auto node = parse_expression();
        if (!node)
            node = parse_redirection();
        if (!node)
            return nodes;
        nodes.append(node.release_nonnull());
    }
}

RefPtr<AST::Node> Parser::parse_toplevel()
{
    auto rule_start = push_start();

    SequenceParseResult result;
    Vector<NonnullRefPtr<AST::Node>> sequence;
    Vector<AST::Position> positions;
    do {
        result = parse_sequence();
        if (result.entries.is_empty())
            break;

        sequence.extend(move(result.entries));
        positions.extend(result.separator_positions.span());
    } while (result.decision == ShouldReadMoreSequences::Yes);

    if (sequence.is_empty())
        return nullptr;

    return create<AST::Execute>(
        create<AST::Sequence>(move(sequence), move(positions)));
}

Parser::SequenceParseResult Parser::parse_sequence()
{
    Vector<NonnullRefPtr<AST::Node>> left;
    auto read_terminators = [&](bool consider_tabs_and_spaces) {
        if (m_heredoc_initiations.is_empty()) {
        discard_terminators:;
            consume_while(is_any_of(consider_tabs_and_spaces ? " \t\n;"sv : "\n;"sv));
        } else {
            for (;;) {
                if (consider_tabs_and_spaces && (peek() == '\t' || peek() == ' ')) {
                    consume();
                    continue;
                }
                if (peek() == ';') {
                    consume();
                    continue;
                }
                if (peek() == '\n') {
                    auto rule_start = push_start();
                    consume();
                    if (!parse_heredoc_entries()) {
                        StringBuilder error_builder;
                        error_builder.append("Expected to find heredoc entries for "sv);
                        bool first = true;
                        for (auto& entry : m_heredoc_initiations) {
                            if (first)
                                error_builder.appendff("{} (at {}:{})", entry.end, entry.node->position().start_line.line_column, entry.node->position().start_line.line_number);
                            else
                                error_builder.appendff(", {} (at {}:{})", entry.end, entry.node->position().start_line.line_column, entry.node->position().start_line.line_number);
                            first = false;
                        }
                        left.append(create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(error_builder.to_string()), true));
                        // Just read the rest of the newlines
                        goto discard_terminators;
                    }
                    continue;
                }
                break;
            }
        }
    };

    read_terminators(true);

    auto rule_start = push_start();
    {
        auto var_decls = parse_variable_decls();
        if (var_decls)
            left.append(var_decls.release_nonnull());
    }

    auto pos_before_seps = save_offset();

    switch (peek()) {
    case '}':
        return { move(left), {}, ShouldReadMoreSequences::No };
    case '\n':
        read_terminators(false);
        [[fallthrough]];
    case ';': {
        if (left.is_empty())
            break;

        consume_while(is_any_of("\n;"sv));
        auto pos_after_seps = save_offset();
        AST::Position separator_position { pos_before_seps.offset, pos_after_seps.offset, pos_before_seps.line, pos_after_seps.line };

        return { move(left), { move(separator_position) }, ShouldReadMoreSequences::Yes };
    }
    default:
        break;
    }

    auto first_entry = parse_function_decl();

    Vector<AST::Position> separator_positions;

    if (!first_entry)
        first_entry = parse_or_logical_sequence();

    if (!first_entry)
        return { move(left), {}, ShouldReadMoreSequences::No };

    left.append(first_entry.release_nonnull());
    separator_positions.empend(pos_before_seps.offset, pos_before_seps.offset, pos_before_seps.line, pos_before_seps.line);

    consume_while(is_whitespace);

    pos_before_seps = save_offset();
    switch (peek()) {
    case '\n':
        read_terminators(false);
        [[fallthrough]];
    case ';': {
        consume_while(is_any_of("\n;"sv));
        auto pos_after_seps = save_offset();
        separator_positions.empend(pos_before_seps.offset, pos_after_seps.offset, pos_before_seps.line, pos_after_seps.line);
        return { move(left), move(separator_positions), ShouldReadMoreSequences::Yes };
    }
    case '&': {
        consume();
        auto pos_after_seps = save_offset();
        auto bg = create<AST::Background>(left.take_last()); // Execute Background
        left.append(move(bg));
        separator_positions.empend(pos_before_seps.offset, pos_after_seps.offset, pos_before_seps.line, pos_after_seps.line);
        return { move(left), move(separator_positions), ShouldReadMoreSequences::Yes };
    }
    default:
        return { move(left), move(separator_positions), ShouldReadMoreSequences::No };
    }
}

RefPtr<AST::Node> Parser::parse_variable_decls()
{
    auto rule_start = push_start();

    consume_while(is_whitespace);

    auto pos_before_name = save_offset();
    auto var_name = consume_while(is_word_character);
    if (var_name.is_empty())
        return nullptr;

    if (!expect('=')) {
        restore_to(pos_before_name.offset, pos_before_name.line);
        return nullptr;
    }

    auto name_expr = create<AST::BarewordLiteral>(TRY_OR_THROW_PARSE_ERROR(String::from_utf8(var_name)));

    auto start = push_start();
    auto expression = parse_expression();
    if (!expression || expression->is_syntax_error()) {
        restore_to(*start);
        if (peek() == '(') {
            consume();
            auto command = parse_pipe_sequence();
            if (!command)
                restore_to(*start);
            else if (!expect(')'))
                command->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating close paren"_string, true));
            expression = command;
        }
    }
    if (!expression) {
        if (is_whitespace(peek())) {
            auto string_start = push_start();
            expression = create<AST::StringLiteral>(String {}, AST::StringLiteral::EnclosureType::None);
        } else {
            restore_to(pos_before_name.offset, pos_before_name.line);
            return nullptr;
        }
    }

    Vector<AST::VariableDeclarations::Variable> variables;
    variables.append({ move(name_expr), expression.release_nonnull() });

    if (consume_while(is_whitespace).is_empty())
        return create<AST::VariableDeclarations>(move(variables));

    auto rest = parse_variable_decls();
    if (!rest)
        return create<AST::VariableDeclarations>(move(variables));

    VERIFY(rest->is_variable_decls());
    auto* rest_decl = static_cast<AST::VariableDeclarations*>(rest.ptr());

    variables.extend(rest_decl->variables());

    return create<AST::VariableDeclarations>(move(variables));
}

RefPtr<AST::Node> Parser::parse_function_decl()
{
    auto rule_start = push_start();

    auto restore = [&] {
        restore_to(*rule_start);
        return nullptr;
    };

    consume_while(is_whitespace);
    auto pos_before_name = save_offset();
    auto function_name = consume_while(is_word_character);
    auto pos_after_name = save_offset();
    if (function_name.is_empty())
        return restore();

    if (!expect('('))
        return restore();

    Vector<AST::NameWithPosition> arguments;
    for (;;) {
        consume_while(is_whitespace);

        if (expect(')'))
            break;

        auto name_offset = m_offset;
        auto start_line = line();
        auto arg_name = consume_while(is_word_character);
        if (arg_name.is_empty()) {
            // FIXME: Should this be a syntax error, or just return?
            return restore();
        }
        arguments.append({ TRY_OR_THROW_PARSE_ERROR(String::from_utf8(arg_name)), { name_offset, m_offset, start_line, line() } });
    }

    consume_while(is_any_of("\n\t "sv));

    {
        RefPtr<AST::Node> syntax_error;
        {
            auto obrace_error_start = push_start();
            syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a function body"_string, true);
        }
        if (!expect('{')) {
            return create<AST::FunctionDeclaration>(
                AST::NameWithPosition {
                    TRY_OR_THROW_PARSE_ERROR(String::from_utf8(function_name)),
                    { pos_before_name.offset, pos_after_name.offset, pos_before_name.line, pos_after_name.line } },
                move(arguments),
                move(syntax_error));
        }
    }

    TemporaryChange controls { m_continuation_controls_allowed, false };
    auto body = parse_toplevel();

    {
        RefPtr<AST::SyntaxError> syntax_error;
        {
            auto cbrace_error_start = push_start();
            syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a function body"_string, true);
        }
        if (!expect('}')) {
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = move(syntax_error);

            return create<AST::FunctionDeclaration>(
                AST::NameWithPosition {
                    TRY_OR_THROW_PARSE_ERROR(String::from_utf8(function_name)),
                    { pos_before_name.offset, pos_after_name.offset, pos_before_name.line, pos_after_name.line } },
                move(arguments),
                move(body));
        }
    }

    return create<AST::FunctionDeclaration>(
        AST::NameWithPosition {
            TRY_OR_THROW_PARSE_ERROR(String::from_utf8(function_name)),
            { pos_before_name.offset, pos_after_name.offset, pos_before_name.line, pos_after_name.line } },
        move(arguments),
        move(body));
}

RefPtr<AST::Node> Parser::parse_or_logical_sequence()
{
    consume_while(is_whitespace);
    auto rule_start = push_start();
    auto and_sequence = parse_and_logical_sequence();
    if (!and_sequence)
        return nullptr;

    consume_while(is_whitespace);
    auto pos_before_or = save_offset();
    if (!expect("||"sv))
        return and_sequence;
    auto pos_after_or = save_offset();

    auto right_and_sequence = parse_and_logical_sequence();
    if (!right_and_sequence)
        right_and_sequence = create<AST::SyntaxError>("Expected an expression after '||'"_string, true);

    return create<AST::Or>(
        and_sequence.release_nonnull(),
        right_and_sequence.release_nonnull(),
        AST::Position { pos_before_or.offset, pos_after_or.offset, pos_before_or.line, pos_after_or.line });
}

RefPtr<AST::Node> Parser::parse_and_logical_sequence()
{
    consume_while(is_whitespace);
    auto rule_start = push_start();
    auto pipe_sequence = parse_pipe_sequence();
    if (!pipe_sequence)
        return nullptr;

    consume_while(is_whitespace);
    auto pos_before_and = save_offset();
    if (!expect("&&"sv))
        return pipe_sequence;
    auto pos_after_end = save_offset();

    auto right_and_sequence = parse_and_logical_sequence();
    if (!right_and_sequence)
        right_and_sequence = create<AST::SyntaxError>("Expected an expression after '&&'"_string, true);

    return create<AST::And>(
        pipe_sequence.release_nonnull(),
        right_and_sequence.release_nonnull(),
        AST::Position { pos_before_and.offset, pos_after_end.offset, pos_before_and.line, pos_after_end.line });
}

RefPtr<AST::Node> Parser::parse_pipe_sequence()
{
    auto rule_start = push_start();
    auto left = parse_control_structure();
    if (!left) {
        if (auto cmd = parse_command())
            left = cmd;
        else
            return nullptr;
    }

    consume_while(is_whitespace);

    if (peek() != '|')
        return left;

    auto before_pipe = save_offset();
    consume();
    auto also_pipe_stderr = peek() == '&';
    if (also_pipe_stderr) {
        consume();

        RefPtr<AST::Node> redirection;
        {
            auto redirection_start = push_start();
            redirection = create<AST::Fd2FdRedirection>(STDERR_FILENO, STDOUT_FILENO);
        }

        left = create<AST::Join>(left.release_nonnull(), redirection.release_nonnull());
    }

    if (auto pipe_seq = parse_pipe_sequence()) {
        return create<AST::Pipe>(left.release_nonnull(), pipe_seq.release_nonnull()); // Pipe
    }

    restore_to(before_pipe.offset, before_pipe.line);
    return left;
}

RefPtr<AST::Node> Parser::parse_command()
{
    auto rule_start = push_start();
    consume_while(is_whitespace);

    auto redir = parse_redirection();
    if (!redir) {
        auto list_expr = parse_list_expression();
        if (!list_expr)
            return nullptr;

        auto cast = create<AST::CastToCommand>(list_expr.release_nonnull()); // Cast List Command

        auto next_command = parse_command();
        if (!next_command)
            return cast;

        return create<AST::Join>(move(cast), next_command.release_nonnull()); // Join List Command
    }

    auto command = parse_command();
    if (!command)
        return redir;

    return create<AST::Join>(redir.release_nonnull(), command.release_nonnull()); // Join Command Command
}

RefPtr<AST::Node> Parser::parse_control_structure()
{
    auto rule_start = push_start();
    consume_while(is_whitespace);
    if (auto control = parse_continuation_control())
        return control;

    if (auto for_loop = parse_for_loop())
        return for_loop;

    if (auto loop = parse_loop_loop())
        return loop;

    if (auto if_expr = parse_if_expr())
        return if_expr;

    if (auto subshell = parse_subshell())
        return subshell;

    if (auto match = parse_match_expr())
        return match;

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_continuation_control()
{
    if (!m_continuation_controls_allowed)
        return nullptr;

    auto rule_start = push_start();

    if (expect("break"sv)) {
        {
            auto break_end = push_start();
            if (consume_while(is_any_of(" \t\n;"sv)).is_empty()) {
                restore_to(*rule_start);
                return nullptr;
            }
            restore_to(*break_end);
        }
        return create<AST::ContinuationControl>(AST::ContinuationControl::Break);
    }

    if (expect("continue"sv)) {
        {
            auto continue_end = push_start();
            if (consume_while(is_any_of(" \t\n;"sv)).is_empty()) {
                restore_to(*rule_start);
                return nullptr;
            }
            restore_to(*continue_end);
        }
        return create<AST::ContinuationControl>(AST::ContinuationControl::Continue);
    }

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_for_loop()
{
    auto rule_start = push_start();
    if (!expect("for"sv))
        return nullptr;

    if (consume_while(is_any_of(" \t\n"sv)).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    Optional<AST::NameWithPosition> index_variable_name, variable_name;
    Optional<AST::Position> in_start_position, index_start_position;

    auto offset_before_index = current_position();
    if (expect("index"sv)) {
        auto offset = current_position();
        if (!consume_while(is_whitespace).is_empty()) {
            auto offset_before_variable = current_position();
            auto variable = consume_while(is_word_character);
            if (!variable.is_empty()) {
                index_start_position = AST::Position { offset_before_index.offset, offset.offset, offset_before_index.line, offset.line };

                auto offset_after_variable = current_position();
                index_variable_name = AST::NameWithPosition {
                    TRY_OR_THROW_PARSE_ERROR(String::from_utf8(variable)),
                    { offset_before_variable.offset, offset_after_variable.offset, offset_before_variable.line, offset_after_variable.line },
                };

                consume_while(is_whitespace);
            } else {
                restore_to(offset_before_index.offset, offset_before_index.line);
            }
        } else {
            restore_to(offset_before_index.offset, offset_before_index.line);
        }
    }

    auto variable_name_start_offset = current_position();
    auto name = consume_while(is_word_character);
    auto variable_name_end_offset = current_position();
    if (!name.is_empty()) {
        variable_name = AST::NameWithPosition {
            TRY_OR_THROW_PARSE_ERROR(String::from_utf8(name)),
            { variable_name_start_offset.offset, variable_name_end_offset.offset, variable_name_start_offset.line, variable_name_end_offset.line }
        };
        consume_while(is_whitespace);
        auto in_error_start = push_start();
        if (!expect("in"sv)) {
            auto syntax_error = create<AST::SyntaxError>("Expected 'in' after a variable name in a 'for' loop"_string, true);
            return create<AST::ForLoop>(move(variable_name), move(index_variable_name), move(syntax_error), nullptr); // ForLoop Var Iterated Block
        }
        in_start_position = AST::Position { in_error_start->offset, m_offset, in_error_start->line, line() };
    }

    consume_while(is_whitespace);
    RefPtr<AST::Node> iterated_expression;
    {
        auto iter_error_start = push_start();
        iterated_expression = parse_expression();
        if (!iterated_expression)
            iterated_expression = create<AST::SyntaxError>("Expected an expression in 'for' loop"_string, true);
    }

    consume_while(is_any_of(" \t\n"sv));
    {
        auto obrace_error_start = push_start();
        if (!expect('{')) {
            auto syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a 'for' loop body"_string, true);
            return create<AST::ForLoop>(move(variable_name), move(index_variable_name), move(iterated_expression), move(syntax_error), move(in_start_position), move(index_start_position)); // ForLoop Var Iterated Block
        }
    }

    TemporaryChange controls { m_continuation_controls_allowed, true };
    auto body = parse_toplevel();

    {
        auto cbrace_error_start = push_start();
        if (!expect('}')) {
            auto error_start = push_start();
            auto syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a 'for' loop body"_string, true);
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = syntax_error;
        }
    }

    return create<AST::ForLoop>(move(variable_name), move(index_variable_name), move(iterated_expression), move(body), move(in_start_position), move(index_start_position)); // ForLoop Var Iterated Block
}

RefPtr<AST::Node> Parser::parse_loop_loop()
{
    auto rule_start = push_start();
    if (!expect("loop"sv))
        return nullptr;

    if (consume_while(is_any_of(" \t\n"sv)).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    {
        auto obrace_error_start = push_start();
        if (!expect('{')) {
            auto syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a 'loop' loop body"_string, true);
            return create<AST::ForLoop>(AST::NameWithPosition {}, AST::NameWithPosition {}, nullptr, move(syntax_error)); // ForLoop null null Block
        }
    }

    TemporaryChange controls { m_continuation_controls_allowed, true };
    auto body = parse_toplevel();

    {
        auto cbrace_error_start = push_start();
        if (!expect('}')) {
            auto error_start = push_start();
            auto syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a 'loop' loop body"_string, true);
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = syntax_error;
        }
    }

    return create<AST::ForLoop>(AST::NameWithPosition {}, AST::NameWithPosition {}, nullptr, move(body)); // ForLoop null null Block
}

RefPtr<AST::Node> Parser::parse_if_expr()
{
    auto rule_start = push_start();
    if (!expect("if"sv))
        return nullptr;

    if (consume_while(is_any_of(" \t\n"sv)).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    RefPtr<AST::Node> condition;
    {
        auto cond_error_start = push_start();
        condition = parse_or_logical_sequence();
        if (!condition)
            condition = create<AST::SyntaxError>("Expected a logical sequence after 'if'"_string, true);
    }

    auto parse_braced_toplevel = [&]() -> RefPtr<AST::Node> {
        RefPtr<AST::Node> body;
        {
            auto obrace_error_start = push_start();
            if (!expect('{')) {
                body = create<AST::SyntaxError>("Expected an open brace '{' to start an 'if' true branch"_string, true);
            }
        }

        if (!body)
            body = parse_toplevel();

        {
            auto cbrace_error_start = push_start();
            if (!expect('}')) {
                auto error_start = push_start();
                RefPtr<AST::SyntaxError> syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end an 'if' true branch"_string, true);
                if (body)
                    body->set_is_syntax_error(*syntax_error);
                else
                    body = syntax_error;
            }
        }

        return body;
    };

    consume_while(is_any_of(" \t\n"sv));
    auto true_branch = parse_braced_toplevel();

    auto end_before_else = m_offset;
    auto line_before_else = line();
    consume_while(is_any_of(" \t\n"sv));
    Optional<AST::Position> else_position;
    {
        auto else_start = push_start();
        if (expect("else"sv))
            else_position = AST::Position { else_start->offset, m_offset, else_start->line, line() };
        else
            restore_to(end_before_else, line_before_else);
    }

    if (else_position.has_value()) {
        consume_while(is_any_of(" \t\n"sv));
        if (peek() == '{') {
            auto false_branch = parse_braced_toplevel();
            return create<AST::IfCond>(else_position, condition.release_nonnull(), move(true_branch), move(false_branch)); // If expr true_branch Else false_branch
        }

        auto else_if_branch = parse_if_expr();
        return create<AST::IfCond>(else_position, condition.release_nonnull(), move(true_branch), move(else_if_branch)); // If expr true_branch Else If ...
    }

    return create<AST::IfCond>(else_position, condition.release_nonnull(), move(true_branch), nullptr); // If expr true_branch
}

RefPtr<AST::Node> Parser::parse_subshell()
{
    auto rule_start = push_start();
    if (!expect('{'))
        return nullptr;

    auto body = parse_toplevel();

    {
        auto cbrace_error_start = push_start();
        if (!expect('}')) {
            auto error_start = push_start();
            RefPtr<AST::SyntaxError> syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a subshell"_string, true);
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = syntax_error;
        }
    }

    return create<AST::Subshell>(move(body));
}

RefPtr<AST::Node> Parser::parse_match_expr()
{
    auto rule_start = push_start();
    if (!expect("match"sv))
        return nullptr;

    if (consume_while(is_whitespace).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    auto match_expression = parse_expression();
    if (!match_expression) {
        return create<AST::MatchExpr>(
            create<AST::SyntaxError>("Expected an expression after 'match'"_string, true),
            String {}, Optional<AST::Position> {}, Vector<AST::MatchEntry> {});
    }

    consume_while(is_any_of(" \t\n"sv));

    String match_name;
    Optional<AST::Position> as_position;
    auto as_start = m_offset;
    auto as_line = line();
    if (expect("as"sv)) {
        as_position = AST::Position { as_start, m_offset, as_line, line() };

        if (consume_while(is_any_of(" \t\n"sv)).is_empty()) {
            auto node = create<AST::MatchExpr>(
                match_expression.release_nonnull(),
                String {}, move(as_position), Vector<AST::MatchEntry> {});
            node->set_is_syntax_error(create<AST::SyntaxError>("Expected whitespace after 'as' in 'match'"_string, true));
            return node;
        }

        match_name = TRY_OR_THROW_PARSE_ERROR(String::from_utf8(consume_while(is_word_character)));
        if (match_name.is_empty()) {
            auto node = create<AST::MatchExpr>(
                match_expression.release_nonnull(),
                String {}, move(as_position), Vector<AST::MatchEntry> {});
            node->set_is_syntax_error(create<AST::SyntaxError>("Expected an identifier after 'as' in 'match'"_string, true));
            return node;
        }
    }

    consume_while(is_any_of(" \t\n"sv));

    if (!expect('{')) {
        auto node = create<AST::MatchExpr>(
            match_expression.release_nonnull(),
            move(match_name), move(as_position), Vector<AST::MatchEntry> {});
        node->set_is_syntax_error(create<AST::SyntaxError>("Expected an open brace '{' to start a 'match' entry list"_string, true));
        return node;
    }

    consume_while(is_any_of(" \t\n"sv));

    Vector<AST::MatchEntry> entries;
    for (;;) {
        auto entry = parse_match_entry();
        consume_while(is_any_of(" \t\n"sv));
        if (entry.options.visit([](auto& x) { return x.is_empty(); }))
            break;

        entries.append(move(entry));
    }

    consume_while(is_any_of(" \t\n"sv));

    if (!expect('}')) {
        auto node = create<AST::MatchExpr>(
            match_expression.release_nonnull(),
            move(match_name), move(as_position), move(entries));
        node->set_is_syntax_error(create<AST::SyntaxError>("Expected a close brace '}' to end a 'match' entry list"_string, true));
        return node;
    }

    return create<AST::MatchExpr>(match_expression.release_nonnull(), move(match_name), move(as_position), move(entries));
}

AST::MatchEntry Parser::parse_match_entry()
{
    auto rule_start = push_start();

    Vector<NonnullRefPtr<AST::Node>> patterns;
    Vector<Regex<ECMA262>> regexps;
    Vector<AST::Position> pipe_positions;
    Optional<Vector<String>> match_names;
    Optional<AST::Position> match_as_position;
    enum {
        Regex,
        Glob,
    } pattern_kind;

    consume_while(is_any_of(" \t\n"sv));

    auto regex_pattern = parse_regex_pattern();
    if (regex_pattern.has_value()) {
        if (auto error = regex_pattern.value().parser_result.error; error != regex::Error::NoError)
            return { Vector<NonnullRefPtr<AST::Node>> {}, {}, {}, {}, create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::from_utf8(regex::get_error_string(error))), false) };

        pattern_kind = Regex;
        regexps.append(regex_pattern.release_value());
    } else {
        auto glob_pattern = parse_match_pattern();
        if (!glob_pattern)
            return { Vector<NonnullRefPtr<AST::Node>> {}, {}, {}, {}, create<AST::SyntaxError>("Expected a pattern in 'match' body"_string, true) };

        pattern_kind = Glob;
        patterns.append(glob_pattern.release_nonnull());
    }

    consume_while(is_any_of(" \t\n"sv));

    auto previous_pipe_start_position = m_offset;
    auto previous_pipe_start_line = line();
    RefPtr<AST::SyntaxError> error;
    while (expect('|')) {
        pipe_positions.append({ previous_pipe_start_position, m_offset, previous_pipe_start_line, line() });
        consume_while(is_any_of(" \t\n"sv));
        switch (pattern_kind) {
        case Regex: {
            auto pattern = parse_regex_pattern();
            if (!pattern.has_value()) {
                error = create<AST::SyntaxError>("Expected a regex pattern to follow '|' in 'match' body"_string, true);
                break;
            }
            regexps.append(pattern.release_value());
            break;
        }
        case Glob: {
            auto pattern = parse_match_pattern();
            if (!pattern) {
                error = create<AST::SyntaxError>("Expected a pattern to follow '|' in 'match' body"_string, true);
                break;
            }
            patterns.append(pattern.release_nonnull());
            break;
        }
        }

        consume_while(is_any_of(" \t\n"sv));

        previous_pipe_start_line = line();
        previous_pipe_start_position = m_offset;
    }

    consume_while(is_any_of(" \t\n"sv));

    auto as_start_position = m_offset;
    auto as_start_line = line();
    if (pattern_kind == Glob && expect("as"sv)) {
        match_as_position = AST::Position { as_start_position, m_offset, as_start_line, line() };
        consume_while(is_any_of(" \t\n"sv));
        if (!expect('(')) {
            if (!error)
                error = create<AST::SyntaxError>("Expected an explicit list of identifiers after a pattern 'as'"_string);
        } else {
            match_names = Vector<String>();
            for (;;) {
                consume_while(is_whitespace);
                auto name = consume_while(is_word_character);
                if (name.is_empty())
                    break;
                match_names->append(TRY_OR(
                    String::from_utf8(name),
                    error = create<AST::SyntaxError>(MUST(String::from_utf8(_error.string_literal())));
                    break;));
            }

            if (!expect(')')) {
                if (!error)
                    error = create<AST::SyntaxError>("Expected a close paren ')' to end the identifier list of pattern 'as'"_string, true);
            }
        }
        consume_while(is_any_of(" \t\n"sv));
    }

    if (pattern_kind == Regex) {
        Vector<String> names;
        for (auto& regex : regexps) {
            if (names.is_empty()) {
                for (auto& name : regex.parser_result.capture_groups)
                    names.append(TRY_OR(
                        String::from_byte_string(name),
                        error = create<AST::SyntaxError>(MUST(String::from_utf8(_error.string_literal())));
                        break;));
            } else {
                size_t index = 0;
                for (auto& name : regex.parser_result.capture_groups) {
                    if (names.size() <= index) {
                        names.append(TRY_OR(
                            String::from_byte_string(name),
                            error = create<AST::SyntaxError>(MUST(String::from_utf8(_error.string_literal())));
                            break;));
                        continue;
                    }

                    if (names[index] != name.view()) {
                        if (!error)
                            error = create<AST::SyntaxError>("Alternative regex patterns must have the same capture groups"_string, false);
                        break;
                    }
                }
            }
        }
        match_names = move(names);
    }

    if (!expect('{')) {
        if (!error)
            error = create<AST::SyntaxError>("Expected an open brace '{' to start a match entry body"_string, true);
    }

    auto body = parse_toplevel();

    if (!expect('}')) {
        if (!error)
            error = create<AST::SyntaxError>("Expected a close brace '}' to end a match entry body"_string, true);
    }

    if (body && error)
        body->set_is_syntax_error(*error);
    else if (error)
        body = error;

    if (pattern_kind == Glob)
        return { move(patterns), move(match_names), move(match_as_position), move(pipe_positions), move(body) };

    return { move(regexps), move(match_names), move(match_as_position), move(pipe_positions), move(body) };
}

RefPtr<AST::Node> Parser::parse_match_pattern()
{
    return parse_expression();
}

Optional<Regex<ECMA262>> Parser::parse_regex_pattern()
{
    auto rule_start = push_start();

    auto start = m_offset;
    if (!expect("(?:"sv) && !expect("(?<"sv))
        return {};

    size_t open_parens = 1;
    while (open_parens > 0) {
        if (at_end())
            break;

        if (next_is("("sv))
            ++open_parens;
        else if (next_is(")"sv))
            --open_parens;
        consume();
    }

    if (open_parens != 0) {
        restore_to(*rule_start);
        return {};
    }

    auto end = m_offset;
    auto pattern = m_input.substring_view(start, end - start);
    return Regex<ECMA262>(pattern);
}

RefPtr<AST::Node> Parser::parse_redirection()
{
    auto rule_start = push_start();

    // heredoc entry
    if (next_is("<<-"sv) || next_is("<<~"sv))
        return nullptr;

    auto pipe_fd = 0;
    auto number = consume_while(is_digit);
    if (number.is_empty()) {
        pipe_fd = -1;
    } else {
        auto fd = number.to_number<int>();
        pipe_fd = fd.value_or(-1);
    }

    switch (peek()) {
    case '>': {
        consume();
        if (peek() == '>') {
            consume();
            consume_while(is_whitespace);
            pipe_fd = pipe_fd >= 0 ? pipe_fd : STDOUT_FILENO;
            auto path = parse_expression();
            if (!path) {
                if (!at_end()) {
                    // Eat a character and hope the problem goes away
                    consume();
                }
                path = create<AST::SyntaxError>("Expected a path after redirection"_string, true);
            }
            return create<AST::WriteAppendRedirection>(pipe_fd, path.release_nonnull()); // Redirection WriteAppend
        }
        if (peek() == '&') {
            consume();
            // FIXME: 'fd>&-' Syntax not the best. needs discussion.
            if (peek() == '-') {
                consume();
                pipe_fd = pipe_fd >= 0 ? pipe_fd : STDOUT_FILENO;
                return create<AST::CloseFdRedirection>(pipe_fd); // Redirection CloseFd
            }
            int dest_pipe_fd = 0;
            auto number = consume_while(is_digit);
            pipe_fd = pipe_fd >= 0 ? pipe_fd : STDOUT_FILENO;
            if (number.is_empty()) {
                dest_pipe_fd = -1;
            } else {
                auto fd = number.to_number<int>();
                dest_pipe_fd = fd.value_or(-1);
            }
            auto redir = create<AST::Fd2FdRedirection>(pipe_fd, dest_pipe_fd); // Redirection Fd2Fd
            if (dest_pipe_fd == -1)
                redir->set_is_syntax_error(*create<AST::SyntaxError>("Expected a file descriptor"_string));
            return redir;
        }
        consume_while(is_whitespace);
        pipe_fd = pipe_fd >= 0 ? pipe_fd : STDOUT_FILENO;
        auto path = parse_expression();
        if (!path) {
            if (!at_end()) {
                // Eat a character and hope the problem goes away
                consume();
            }
            path = create<AST::SyntaxError>("Expected a path after redirection"_string, true);
        }
        return create<AST::WriteRedirection>(pipe_fd, path.release_nonnull()); // Redirection Write
    }
    case '<': {
        consume();
        enum {
            Read,
            ReadWrite,
        } mode { Read };

        if (peek() == '>') {
            mode = ReadWrite;
            consume();
        }

        consume_while(is_whitespace);
        pipe_fd = pipe_fd >= 0 ? pipe_fd : STDIN_FILENO;
        auto path = parse_expression();
        if (!path) {
            if (!at_end()) {
                // Eat a character and hope the problem goes away
                consume();
            }
            path = create<AST::SyntaxError>("Expected a path after redirection"_string, true);
        }
        if (mode == Read)
            return create<AST::ReadRedirection>(pipe_fd, path.release_nonnull()); // Redirection Read

        return create<AST::ReadWriteRedirection>(pipe_fd, path.release_nonnull()); // Redirection ReadWrite
    }
    default:
        restore_to(*rule_start);
        return nullptr;
    }
}

RefPtr<AST::Node> Parser::parse_list_expression()
{
    consume_while(is_whitespace);

    auto rule_start = push_start();
    Vector<NonnullRefPtr<AST::Node>> nodes;

    do {
        auto expr = parse_expression();
        if (!expr)
            break;
        nodes.append(expr.release_nonnull());
    } while (!consume_while(is_whitespace).is_empty());

    if (nodes.is_empty())
        return nullptr;

    return create<AST::ListConcatenate>(move(nodes)); // Concatenate List
}

RefPtr<AST::Node> Parser::parse_expression()
{
    auto rule_start = push_start();
    if (m_rule_start_offsets.size() > max_allowed_nested_rule_depth)
        return create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expression nested too deep (max allowed is {})", max_allowed_nested_rule_depth)));

    auto starting_char = peek();

    auto read_concat = [&](auto&& expr) -> NonnullRefPtr<AST::Node> {
        if (is_whitespace(peek()))
            return move(expr);

        if (auto next_expr = parse_expression())
            return create<AST::Juxtaposition>(move(expr), next_expr.release_nonnull());

        return move(expr);
    };

    // Heredocs are expressions, so allow them
    if (!(next_is("<<-"sv) || next_is("<<~"sv))) {
        if (strchr("&|)} ;<>\n", starting_char) != nullptr)
            return nullptr;
    }

    if (m_extra_chars_not_allowed_in_barewords.contains_slow(starting_char))
        return nullptr;

    if (m_is_in_brace_expansion_spec && next_is(".."sv))
        return nullptr;

    if (isdigit(starting_char)) {
        ScopedValueRollback offset_rollback { m_offset };

        auto redir = parse_redirection();
        if (redir)
            return nullptr;
    }

    if (starting_char == '$') {
        if (auto variable = parse_variable())
            return read_concat(variable.release_nonnull());

        if (auto immediate = parse_immediate_expression())
            return read_concat(immediate.release_nonnull());

        auto inline_exec = parse_evaluate();
        if (inline_exec && !inline_exec->is_syntax_error())
            return read_concat(inline_exec.release_nonnull());
        return inline_exec;
    }

    if (starting_char == '#')
        return parse_comment();

    if (starting_char == '(') {
        consume();
        auto list = parse_list_expression();
        if (!expect(')')) {
            restore_to(*rule_start);
            return nullptr;
        }
        return read_concat(create<AST::CastToList>(move(list))); // Cast To List
    }

    if (starting_char == '!' && m_in_interactive_mode) {
        if (auto designator = parse_history_designator())
            return designator;
    }

    if (auto composite = parse_string_composite())
        return read_concat(composite.release_nonnull());

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_string_composite()
{
    auto rule_start = push_start();
    if (auto string = parse_string()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(string.release_nonnull(), next_part.release_nonnull()); // Concatenate String StringComposite

        return string;
    }

    if (auto variable = parse_variable()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(variable.release_nonnull(), next_part.release_nonnull()); // Concatenate Variable StringComposite

        return variable;
    }

    if (auto glob = parse_glob()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(glob.release_nonnull(), next_part.release_nonnull()); // Concatenate Glob StringComposite

        return glob;
    }

    if (auto expansion = parse_brace_expansion()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(expansion.release_nonnull(), next_part.release_nonnull()); // Concatenate BraceExpansion StringComposite

        return expansion;
    }

    if (auto bareword = parse_bareword()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(bareword.release_nonnull(), next_part.release_nonnull()); // Concatenate Bareword StringComposite

        return bareword;
    }

    if (auto inline_command = parse_evaluate()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(inline_command.release_nonnull(), next_part.release_nonnull()); // Concatenate Execute StringComposite

        return inline_command;
    }

    if (auto heredoc = parse_heredoc_initiation_record()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(heredoc.release_nonnull(), next_part.release_nonnull()); // Concatenate Heredoc StringComposite

        return heredoc;
    }

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_string()
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    if (peek() == '"') {
        consume();
        auto inner = parse_string_inner(StringEndCondition::DoubleQuote);
        if (!inner)
            inner = create<AST::SyntaxError>("Unexpected EOF in string"_string, true);
        if (!expect('"')) {
            inner = create<AST::DoubleQuotedString>(move(inner));
            inner->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating double quote"_string, true));
            return inner;
        }
        return create<AST::DoubleQuotedString>(move(inner)); // Double Quoted String
    }

    if (peek() == '\'') {
        consume();
        auto text = consume_while(is_not('\''));
        bool is_error = false;
        if (!expect('\''))
            is_error = true;
        auto result = create<AST::StringLiteral>(TRY_OR_THROW_PARSE_ERROR(String::from_utf8(text)), AST::StringLiteral::EnclosureType::SingleQuotes); // String Literal
        if (is_error)
            result->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating single quote"_string, true));
        return result;
    }

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_string_inner(StringEndCondition condition)
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    StringBuilder builder;
    while (!at_end()) {
        if (condition == StringEndCondition::DoubleQuote && peek() == '"') {
            break;
        }

        if (peek() == '\\') {
            consume();
            if (at_end()) {
                break;
            }
            auto ch = consume();
            switch (ch) {
            case '\\':
            default:
                builder.append(ch);
                break;
            case 'x': {
                if (m_input.length() <= m_offset + 2)
                    break;
                auto first_nibble = tolower(consume());
                auto second_nibble = tolower(consume());
                if (!isxdigit(first_nibble) || !isxdigit(second_nibble)) {
                    builder.append(first_nibble);
                    builder.append(second_nibble);
                    break;
                }
                builder.append(to_byte(first_nibble, second_nibble));
                break;
            }
            case 'u': {
                if (m_input.length() <= m_offset + 8)
                    break;
                size_t counter = 8;
                auto chars = consume_while([&](auto) { return counter-- > 0; });
                if (auto number = AK::StringUtils::convert_to_uint_from_hex(chars); number.has_value())
                    builder.append(Utf32View { &number.value(), 1 });
                else
                    builder.append(chars);

                break;
            }
            case '0':
            case 'o':
            case 'c': {
                auto read_anything = false;
                u8 byte = 0;
                auto start = m_offset;
                while (!at_end() && is_ascii_octal_digit(peek())) {
                    if (byte > 32)
                        break;
                    read_anything = true;
                    byte *= 8;
                    byte += consume() - '0';
                }
                if (read_anything)
                    builder.append(byte);
                else
                    builder.append(m_input.substring_view(start, m_offset - start));
                break;
            }
            case 'a':
                builder.append('\a');
                break;
            case 'b':
                builder.append('\b');
                break;
            case 'e':
                builder.append('\x1b');
                break;
            case 'f':
                builder.append('\f');
                break;
            case 'r':
                builder.append('\r');
                break;
            case 'n':
                builder.append('\n');
                break;
            case 't':
                builder.append('\t');
                break;
            }
            continue;
        }
        if (peek() == '$') {
            auto string_literal = create<AST::StringLiteral>(TRY_OR_THROW_PARSE_ERROR(builder.to_string()), AST::StringLiteral::EnclosureType::DoubleQuotes); // String Literal
            auto read_concat = [&](auto&& node) {
                auto inner = create<AST::StringPartCompose>(
                    move(string_literal),
                    move(node)); // Compose String Node

                if (auto string = parse_string_inner(condition)) {
                    return create<AST::StringPartCompose>(move(inner), string.release_nonnull()); // Compose Composition Composition
                }

                return inner;
            };

            if (auto variable = parse_variable())
                return read_concat(variable.release_nonnull());

            if (auto immediate = parse_immediate_expression())
                return read_concat(immediate.release_nonnull());

            if (auto evaluate = parse_evaluate())
                return read_concat(evaluate.release_nonnull());
        }

        builder.append(consume());
    }

    return create<AST::StringLiteral>(TRY_OR_THROW_PARSE_ERROR(builder.to_string()), AST::StringLiteral::EnclosureType::DoubleQuotes); // String Literal
}

RefPtr<AST::Node> Parser::parse_variable()
{
    auto rule_start = push_start();
    auto ref = parse_variable_ref();

    if (!ref)
        return nullptr;

    auto variable = static_ptr_cast<AST::VariableNode>(ref);
    if (auto slice = parse_slice())
        variable->set_slice(slice.release_nonnull());

    return variable;
}

RefPtr<AST::Node> Parser::parse_variable_ref()
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    if (peek() != '$')
        return nullptr;

    consume();
    switch (peek()) {
    case '$':
    case '?':
    case '*':
    case '#':
        return create<AST::SpecialVariable>(consume()); // Variable Special
    default:
        break;
    }

    auto name = consume_while(is_word_character);

    if (name.length() == 0) {
        restore_to(rule_start->offset, rule_start->line);
        return nullptr;
    }

    return create<AST::SimpleVariable>(TRY_OR_THROW_PARSE_ERROR(String::from_utf8(name))); // Variable Simple
}

RefPtr<AST::Slice> Parser::parse_slice()
{
    auto rule_start = push_start();
    if (!next_is("["sv))
        return nullptr;

    consume(); // [

    ScopedValueRollback chars_change { m_extra_chars_not_allowed_in_barewords };
    m_extra_chars_not_allowed_in_barewords.append(']');
    auto spec = parse_brace_expansion_spec();

    RefPtr<AST::SyntaxError> error;

    if (peek() != ']')
        error = create<AST::SyntaxError>("Expected a close bracket ']' to end a variable slice"_string);
    else
        consume();

    if (!spec) {
        if (error)
            spec = move(error);
        else
            spec = create<AST::SyntaxError>("Expected either a range, or a comma-seprated list of selectors"_string);
    }

    auto node = create<AST::Slice>(spec.release_nonnull());
    if (error)
        node->set_is_syntax_error(*error);
    return node;
}

RefPtr<AST::Node> Parser::parse_evaluate()
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    if (peek() != '$')
        return nullptr;

    consume();
    if (peek() == '(') {
        consume();
        auto inner = parse_pipe_sequence();
        if (!inner)
            inner = create<AST::SyntaxError>("Unexpected EOF in list"_string, true);
        if (!expect(')'))
            inner->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating close paren"_string, true));

        return create<AST::Execute>(inner.release_nonnull(), true);
    }
    auto inner = parse_expression();

    if (!inner) {
        inner = create<AST::SyntaxError>("Expected a command"_string, true);
    } else {
        if (inner->is_list()) {
            auto execute_inner = create<AST::Execute>(inner.release_nonnull(), true);
            inner = move(execute_inner);
        } else {
            auto dyn_inner = create<AST::DynamicEvaluate>(inner.release_nonnull());
            inner = move(dyn_inner);
        }
    }

    return inner;
}

RefPtr<AST::Node> Parser::parse_immediate_expression()
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    if (peek() != '$')
        return nullptr;

    consume();

    if (peek() != '{') {
        restore_to(*rule_start);
        return nullptr;
    }

    consume();
    consume_while(is_whitespace);

    auto function_name_start_offset = current_position();
    auto function_name = consume_while(is_word_character);
    auto function_name_end_offset = current_position();
    AST::Position function_position {
        function_name_start_offset.offset,
        function_name_end_offset.offset,
        function_name_start_offset.line,
        function_name_end_offset.line,
    };

    consume_while(is_whitespace);

    Vector<NonnullRefPtr<AST::Node>> arguments;
    do {
        auto expr = parse_expression();
        if (!expr)
            break;
        arguments.append(expr.release_nonnull());
    } while (!consume_while(is_whitespace).is_empty());

    auto ending_brace_start_offset = current_position();
    if (peek() == '}')
        consume();

    auto ending_brace_end_offset = current_position();

    auto ending_brace_position = ending_brace_start_offset.offset == ending_brace_end_offset.offset
        ? Optional<AST::Position> {}
        : Optional<AST::Position> {
              AST::Position {
                  ending_brace_start_offset.offset,
                  ending_brace_end_offset.offset,
                  ending_brace_start_offset.line,
                  ending_brace_end_offset.line,
              }
          };

    auto node = create<AST::ImmediateExpression>(
        AST::NameWithPosition { TRY_OR_THROW_PARSE_ERROR(String::from_utf8(function_name)), move(function_position) },
        move(arguments),
        ending_brace_position);

    if (!ending_brace_position.has_value())
        node->set_is_syntax_error(create<AST::SyntaxError>("Expected a closing brace '}' to end an immediate expression"_string, true));
    else if (node->function_name().is_empty())
        node->set_is_syntax_error(create<AST::SyntaxError>("Expected an immediate function name"_string));

    return node;
}

RefPtr<AST::Node> Parser::parse_history_designator()
{
    auto rule_start = push_start();

    VERIFY(peek() == '!');
    consume();

    // Event selector
    AST::HistorySelector selector;
    RefPtr<AST::SyntaxError> syntax_error;
    selector.event.kind = AST::HistorySelector::EventKind::StartingStringLookup;
    selector.event.text_position = { m_offset, m_offset, m_line, m_line };
    selector.word_selector_range = {
        AST::HistorySelector::WordSelector {
            AST::HistorySelector::WordSelectorKind::Index,
            0,
            { m_offset, m_offset, m_line, m_line },
            nullptr },
        AST::HistorySelector::WordSelector {
            AST::HistorySelector::WordSelectorKind::Last,
            0,
            { m_offset, m_offset, m_line, m_line },
            nullptr }
    };

    bool is_word_selector = false;

    switch (peek()) {
    case ':':
        consume();
        [[fallthrough]];
    case '^':
    case '$':
    case '*':
        is_word_selector = true;
        break;
    case '!':
        consume();
        selector.event.kind = AST::HistorySelector::EventKind::IndexFromEnd;
        selector.event.index = 0;
        selector.event.text = "!"_string;
        break;
    case '?':
        consume();
        selector.event.kind = AST::HistorySelector::EventKind::ContainingStringLookup;
        [[fallthrough]];
    default: {
        TemporaryChange chars_change { m_extra_chars_not_allowed_in_barewords, { ':', '^', '$', '*' } };

        auto bareword = parse_bareword();
        if (!bareword || !bareword->is_bareword()) {
            restore_to(*rule_start);
            return nullptr;
        }

        selector.event.text = static_ptr_cast<AST::BarewordLiteral>(bareword)->text();
        selector.event.text_position = bareword->position();
        auto selector_bytes = selector.event.text.bytes();
        auto it = selector_bytes.begin();
        bool is_negative = false;
        if (*it == '-') {
            ++it;
            is_negative = true;
        }
        if (it != selector_bytes.end() && all_of(it, selector_bytes.end(), is_digit)) {
            if (is_negative)
                selector.event.kind = AST::HistorySelector::EventKind::IndexFromEnd;
            else
                selector.event.kind = AST::HistorySelector::EventKind::IndexFromStart;
            auto number = abs(selector.event.text.to_number<int>().value_or(0));
            if (number != 0)
                selector.event.index = number - 1;
            else
                syntax_error = create<AST::SyntaxError>("History entry index value invalid or out of range"_string);
        }
        if (":^$*"sv.contains(peek())) {
            is_word_selector = true;
            if (peek() == ':')
                consume();
        }
    }
    }

    if (!is_word_selector) {
        auto node = create<AST::HistoryEvent>(move(selector));
        if (syntax_error)
            node->set_is_syntax_error(*syntax_error);
        return node;
    }

    // Word selectors
    auto parse_word_selector = [&]() -> Optional<AST::HistorySelector::WordSelector> {
        auto c = peek();
        AST::HistorySelector::WordSelectorKind word_selector_kind;
        ssize_t offset = -1;
        if (isdigit(c)) {
            auto num = consume_while(is_digit);
            auto value = num.to_number<unsigned>();
            if (!value.has_value())
                return {};
            word_selector_kind = AST::HistorySelector::WordSelectorKind::Index;
            offset = value.value();
        } else if (c == '^') {
            consume();
            word_selector_kind = AST::HistorySelector::WordSelectorKind::Index;
            offset = 1;
        } else if (c == '$') {
            consume();
            word_selector_kind = AST::HistorySelector::WordSelectorKind::Last;
            offset = 0;
        }
        if (offset == -1)
            return {};
        return AST::HistorySelector::WordSelector {
            word_selector_kind,
            static_cast<size_t>(offset),
            { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() },
            syntax_error
        };
    };

    auto make_word_selector = [&](AST::HistorySelector::WordSelectorKind word_selector_kind, size_t offset) {
        return AST::HistorySelector::WordSelector {
            word_selector_kind,
            offset,
            { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() },
            syntax_error
        };
    };

    auto first_char = peek();
    if (!(is_digit(first_char) || "^$-*"sv.contains(first_char))) {
        if (!syntax_error)
            syntax_error = create<AST::SyntaxError>("Expected a word selector after ':' in a history event designator"_string, true);
    } else if (first_char == '*') {
        consume();
        selector.word_selector_range.start = make_word_selector(AST::HistorySelector::WordSelectorKind::Index, 1);
        selector.word_selector_range.end = make_word_selector(AST::HistorySelector::WordSelectorKind::Last, 0);
    } else if (first_char == '-') {
        consume();
        selector.word_selector_range.start = make_word_selector(AST::HistorySelector::WordSelectorKind::Index, 0);
        auto last_selector = parse_word_selector();
        if (!last_selector.has_value())
            selector.word_selector_range.end = make_word_selector(AST::HistorySelector::WordSelectorKind::Last, 1);
        else
            selector.word_selector_range.end = last_selector.release_value();
    } else {
        auto first_selector = parse_word_selector();
        // peek() should be a digit, ^, or $ here, so this should always have value.
        VERIFY(first_selector.has_value());
        selector.word_selector_range.start = first_selector.release_value();
        if (peek() == '-') {
            consume();
            auto last_selector = parse_word_selector();
            if (last_selector.has_value()) {
                selector.word_selector_range.end = last_selector.release_value();
            } else {
                selector.word_selector_range.end = make_word_selector(AST::HistorySelector::WordSelectorKind::Last, 1);
            }
        } else if (peek() == '*') {
            consume();
            selector.word_selector_range.end = make_word_selector(AST::HistorySelector::WordSelectorKind::Last, 0);
        } else {
            selector.word_selector_range.end.clear();
        }
    }

    auto node = create<AST::HistoryEvent>(move(selector));
    if (syntax_error)
        node->set_is_syntax_error(*syntax_error);
    return node;
}

RefPtr<AST::Node> Parser::parse_comment()
{
    if (at_end())
        return nullptr;

    if (peek() != '#')
        return nullptr;

    consume();
    auto text = consume_while(is_not('\n'));
    return create<AST::Comment>(TRY_OR_THROW_PARSE_ERROR(String::from_utf8(text))); // Comment
}

RefPtr<AST::Node> Parser::parse_bareword()
{
    auto rule_start = push_start();
    StringBuilder builder;
    auto is_acceptable_bareword_character = [&](char c) {
        return strchr("\\\"'*$&|(){} ?;<>\n", c) == nullptr
            && !m_extra_chars_not_allowed_in_barewords.contains_slow(c);
    };
    while (!at_end()) {
        char ch = peek();
        if (ch == '\\') {
            consume();
            if (!at_end()) {
                ch = consume();
                if (is_acceptable_bareword_character(ch))
                    builder.append('\\');
            }
            builder.append(ch);
            continue;
        }

        if (m_is_in_brace_expansion_spec && next_is(".."sv)) {
            // Don't eat '..' in a brace expansion spec.
            break;
        }

        if (is_acceptable_bareword_character(ch)) {
            builder.append(consume());
            continue;
        }

        break;
    }

    if (builder.is_empty())
        return nullptr;

    auto current_end = m_offset;
    auto current_line = line();
    auto string = TRY_OR_THROW_PARSE_ERROR(builder.to_string());
    if (string.starts_with('~')) {
        String username;
        RefPtr<AST::Node> tilde, text;

        auto first_slash_index = string.find_byte_offset('/');
        if (first_slash_index.has_value()) {
            username = TRY_OR_THROW_PARSE_ERROR(string.substring_from_byte_offset(1, *first_slash_index - 1));
            string = TRY_OR_THROW_PARSE_ERROR(string.substring_from_byte_offset(*first_slash_index));
        } else {
            username = TRY_OR_THROW_PARSE_ERROR(string.substring_from_byte_offset(1));
            string = {};
        }

        // Synthesize a Tilde Node with the correct positioning information.
        {
            restore_to(rule_start->offset, rule_start->line);
            auto ch = consume();
            VERIFY(ch == '~');
            auto username_length = username.bytes_as_string_view().length();
            tilde = create<AST::Tilde>(move(username));
            // Consume the username (if any)
            for (size_t i = 0; i < username_length; ++i)
                consume();
        }

        if (string.is_empty())
            return tilde;

        // Synthesize a BarewordLiteral Node with the correct positioning information.
        {
            auto text_start = push_start();
            restore_to(current_end, current_line);
            text = create<AST::BarewordLiteral>(move(string));
        }

        return create<AST::Juxtaposition>(tilde.release_nonnull(), text.release_nonnull()); // Juxtaposition Variable Bareword
    }

    if (string.starts_with_bytes("\\~"sv)) {
        // Un-escape the tilde, but only at the start (where it would be an expansion)
        string = TRY_OR_THROW_PARSE_ERROR(string.substring_from_byte_offset(1));
    }

    return create<AST::BarewordLiteral>(move(string)); // Bareword Literal
}

RefPtr<AST::Node> Parser::parse_glob()
{
    auto rule_start = push_start();
    auto bareword_part = parse_bareword();

    if (at_end())
        return bareword_part;

    char ch = peek();
    if (ch == '*' || ch == '?') {
        auto saved_offset = save_offset();
        consume();
        StringBuilder textbuilder;
        if (bareword_part) {
            StringView text;
            if (bareword_part->is_bareword()) {
                auto bareword = static_cast<AST::BarewordLiteral*>(bareword_part.ptr());
                text = bareword->text();
            } else {
                // FIXME: Allow composition of tilde+bareword with globs: '~/foo/bar/baz*'
                restore_to(saved_offset.offset, saved_offset.line);
                bareword_part->set_is_syntax_error(*create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Unexpected {} inside a glob", bareword_part->class_name()))));
                return bareword_part;
            }
            textbuilder.append(text);
        }

        textbuilder.append(ch);

        auto glob_after = parse_glob();
        if (glob_after) {
            if (glob_after->is_glob()) {
                auto glob = static_cast<AST::Glob*>(glob_after.ptr());
                textbuilder.append(glob->text());
            } else if (glob_after->is_bareword()) {
                auto bareword = static_cast<AST::BarewordLiteral*>(glob_after.ptr());
                textbuilder.append(bareword->text());
            } else if (glob_after->is_tilde()) {
                auto bareword = static_cast<AST::Tilde*>(glob_after.ptr());
                textbuilder.append('~');
                textbuilder.append(bareword->text());
            } else {
                return create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Invalid node '{}' in glob position, escape shell special characters", glob_after->class_name())));
            }
        }

        return create<AST::Glob>(TRY_OR_THROW_PARSE_ERROR(textbuilder.to_string())); // Glob
    }

    return bareword_part;
}

RefPtr<AST::Node> Parser::parse_brace_expansion()
{
    auto rule_start = push_start();

    if (!expect('{'))
        return nullptr;

    if (auto spec = parse_brace_expansion_spec()) {
        if (!expect('}'))
            spec->set_is_syntax_error(create<AST::SyntaxError>("Expected a close brace '}' to end a brace expansion"_string, true));

        return spec;
    }

    restore_to(*rule_start);
    return nullptr;
}

RefPtr<AST::Node> Parser::parse_brace_expansion_spec()
{
    TemporaryChange is_in_brace_expansion { m_is_in_brace_expansion_spec, true };
    ScopedValueRollback chars_change { m_extra_chars_not_allowed_in_barewords };

    m_extra_chars_not_allowed_in_barewords.append(',');

    auto rule_start = push_start();
    Vector<NonnullRefPtr<AST::Node>> subexpressions;

    if (next_is(","sv)) {
        // Note that we don't consume the ',' here.
        subexpressions.append(create<AST::StringLiteral>(String {}, AST::StringLiteral::EnclosureType::None));
    } else {
        auto start_expr = parse_expression();
        if (start_expr) {
            if (expect(".."sv)) {
                if (auto end_expr = parse_expression()) {
                    if (end_expr->position().start_offset != start_expr->position().end_offset + 2)
                        end_expr->set_is_syntax_error(create<AST::SyntaxError>("Expected no whitespace between '..' and the following expression in brace expansion"_string));

                    return create<AST::Range>(start_expr.release_nonnull(), end_expr.release_nonnull());
                }

                return create<AST::Range>(start_expr.release_nonnull(), create<AST::SyntaxError>("Expected an expression to end range brace expansion with"_string, true));
            }
        }

        if (start_expr)
            subexpressions.append(start_expr.release_nonnull());
    }

    while (expect(',')) {
        auto expr = parse_expression();
        if (expr) {
            subexpressions.append(expr.release_nonnull());
        } else {
            subexpressions.append(create<AST::StringLiteral>(String {}, AST::StringLiteral::EnclosureType::None));
        }
    }

    if (subexpressions.is_empty())
        return nullptr;

    return create<AST::BraceExpansion>(move(subexpressions));
}

RefPtr<AST::Node> Parser::parse_heredoc_initiation_record()
{
    if (!next_is("<<"sv))
        return nullptr;

    auto rule_start = push_start();

    // '<' '<'
    consume();
    consume();

    HeredocInitiationRecord record;
    record.end = "<error>"_string;

    RefPtr<AST::SyntaxError> syntax_error_node;

    // '-' | '~'
    switch (peek()) {
    case '-':
        record.deindent = false;
        consume();
        break;
    case '~':
        record.deindent = true;
        consume();
        break;
    default:
        restore_to(*rule_start);
        return nullptr;
    }

    // StringLiteral | bareword
    if (auto bareword = parse_bareword()) {
        if (!bareword->is_bareword()) {
            syntax_error_node = create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expected a bareword or a quoted string, not {}", bareword->class_name())));
        } else {
            if (bareword->is_syntax_error())
                syntax_error_node = bareword->syntax_error_node();
            else
                record.end = static_cast<AST::BarewordLiteral*>(bareword.ptr())->text();
        }

        record.interpolate = true;
    } else if (peek() == '\'') {
        consume();
        auto text = consume_while(is_not('\''));
        bool is_error = false;
        if (!expect('\''))
            is_error = true;
        if (is_error)
            syntax_error_node = create<AST::SyntaxError>("Expected a terminating single quote"_string, true);

        record.end = TRY_OR_THROW_PARSE_ERROR(String::from_utf8(text));
        record.interpolate = false;
    } else {
        syntax_error_node = create<AST::SyntaxError>("Expected a bareword or a single-quoted string literal for heredoc end key"_string, true);
    }

    auto node = create<AST::Heredoc>(record.end, record.interpolate, record.deindent);
    if (syntax_error_node)
        node->set_is_syntax_error(*syntax_error_node);
    else
        node->set_is_syntax_error(*create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expected heredoc contents for heredoc with end key '{}'", node->end())), true));

    record.node = node;
    m_heredoc_initiations.append(move(record));

    return node;
}

bool Parser::parse_heredoc_entries()
{
    auto heredocs = move(m_heredoc_initiations);
    m_heredoc_initiations.clear();
    // Try to parse heredoc entries, as reverse recorded in the initiation records
    for (auto& record : heredocs) {
        auto rule_start = push_start();
        if (m_rule_start_offsets.size() > max_allowed_nested_rule_depth) {
            record.node->set_is_syntax_error(*create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expression nested too deep (max allowed is {})", max_allowed_nested_rule_depth))));
            continue;
        }
        bool found_key = false;
        if (!record.interpolate) {
            // Since no interpolation is allowed, just read lines until we hit the key
            Optional<Offset> last_line_offset;
            for (;;) {
                if (at_end())
                    break;
                if (peek() == '\n')
                    consume();
                last_line_offset = current_position();
                auto line = consume_while(is_not('\n'));
                if (peek() == '\n')
                    consume();
                if (line.trim_whitespace() == record.end) {
                    found_key = true;
                    break;
                }
            }

            if (!last_line_offset.has_value())
                last_line_offset = current_position();
            // Now just wrap it in a StringLiteral and set it as the node's contents
            auto node = create<AST::StringLiteral>(
                MUST(String::from_utf8(m_input.substring_view(rule_start->offset, last_line_offset->offset - rule_start->offset))),
                AST::StringLiteral::EnclosureType::None);
            if (!found_key)
                node->set_is_syntax_error(*create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expected to find the heredoc key '{}', but found Eof", record.end)), true));
            record.node->set_contents(move(node));
        } else {
            // Interpolation is allowed, so we're going to read doublequoted string innards
            // until we find a line that contains the key
            auto end_condition = move(m_end_condition);
            found_key = false;
            set_end_condition(make<Function<bool()>>([this, end = record.end, &found_key] {
                if (found_key)
                    return true;
                auto offset = current_position();
                auto cond = move(m_end_condition);
                ScopeGuard guard {
                    [&] {
                        m_end_condition = move(cond);
                    }
                };
                if (peek() == '\n') {
                    consume();
                    auto line = consume_while(is_not('\n'));
                    if (peek() == '\n')
                        consume();
                    if (line.trim_whitespace() == end) {
                        restore_to(offset.offset, offset.line);
                        found_key = true;
                        return true;
                    }
                }
                restore_to(offset.offset, offset.line);
                return false;
            }));

            auto expr = parse_string_inner(StringEndCondition::Heredoc);
            set_end_condition(move(end_condition));

            if (found_key) {
                auto offset = current_position();
                if (peek() == '\n')
                    consume();
                auto line = consume_while(is_not('\n'));
                if (peek() == '\n')
                    consume();
                if (line.trim_whitespace() != record.end)
                    restore_to(offset.offset, offset.line);
            }

            if (!expr && found_key) {
                expr = create<AST::StringLiteral>(String {}, AST::StringLiteral::EnclosureType::None);
            } else if (!expr) {
                expr = create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expected to find a valid string inside a heredoc (with end key '{}')", record.end)), true);
            } else if (!found_key) {
                expr->set_is_syntax_error(*create<AST::SyntaxError>(TRY_OR_RESOLVE_TO_ERROR_STRING(String::formatted("Expected to find the heredoc key '{}'", record.end)), true));
            }

            record.node->set_contents(create<AST::DoubleQuotedString>(move(expr)));
        }
    }
    return true;
}

StringView Parser::consume_while(Function<bool(char)> condition)
{
    if (at_end())
        return {};

    auto start_offset = m_offset;

    while (!at_end() && condition(peek()))
        consume();

    return m_input.substring_view(start_offset, m_offset - start_offset);
}

bool Parser::next_is(StringView next)
{
    auto start = current_position();
    auto res = expect(next);
    restore_to(start.offset, start.line);
    return res;
}

}
