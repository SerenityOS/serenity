/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "Parser.h"
#include "Shell.h"
#include <AK/AllOf.h>
#include <AK/TemporaryChange.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

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

bool Parser::expect(const StringView& expected)
{
    auto offset_at_start = m_offset;
    auto line_at_start = line();

    if (expected.length() + m_offset > m_input.length())
        return false;

    for (size_t i = 0; i < expected.length(); ++i) {
        if (peek() != expected[i]) {
            restore_to(offset_at_start, line_at_start);
            return false;
        }

        consume();
    }

    return true;
}

template<typename A, typename... Args>
NonnullRefPtr<A> Parser::create(Args... args)
{
    return adopt(*new A(AST::Position { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() }, args...));
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
        auto syntax_error_node = create<AST::SyntaxError>("Unexpected tokens past the end");
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

NonnullRefPtrVector<AST::Node> Parser::parse_as_multiple_expressions()
{
    NonnullRefPtrVector<AST::Node> nodes;
    for (;;) {
        consume_while(is_whitespace);
        auto node = parse_expression();
        if (!node)
            node = parse_redirection();
        if (!node)
            return nodes;
        nodes.append(node.release_nonnull());
    }
    return nodes;
}

RefPtr<AST::Node> Parser::parse_toplevel()
{
    auto rule_start = push_start();

    SequenceParseResult result;
    NonnullRefPtrVector<AST::Node> sequence;
    Vector<AST::Position> positions;
    do {
        result = parse_sequence();
        if (result.entries.is_empty())
            break;

        sequence.append(move(result.entries));
        positions.append(move(result.separator_positions));
    } while (result.decision == ShouldReadMoreSequences::Yes);

    if (sequence.is_empty())
        return nullptr;

    return create<AST::Execute>(
        create<AST::Sequence>(move(sequence), move(positions)));
}

Parser::SequenceParseResult Parser::parse_sequence()
{
    consume_while(is_any_of(" \t\n;")); // ignore whitespaces or terminators without effect.

    NonnullRefPtrVector<AST::Node> left;

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
    case ';':
    case '\n': {
        if (left.is_empty())
            break;

        consume_while(is_any_of("\n;"));
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
    case ';':
    case '\n': {
        consume_while(is_any_of("\n;"));
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

    auto name_expr = create<AST::BarewordLiteral>(move(var_name));

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
                command->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating close paren", true));
            expression = command;
        }
    }
    if (!expression) {
        if (is_whitespace(peek())) {
            auto string_start = push_start();
            expression = create<AST::StringLiteral>("");
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

    variables.append(rest_decl->variables());

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

    Vector<AST::FunctionDeclaration::NameWithPosition> arguments;
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
        arguments.append({ arg_name, { name_offset, m_offset, start_line, line() } });
    }

    consume_while(is_whitespace);

    {
        RefPtr<AST::Node> syntax_error;
        {
            auto obrace_error_start = push_start();
            syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a function body", true);
        }
        if (!expect('{')) {
            return create<AST::FunctionDeclaration>(
                AST::FunctionDeclaration::NameWithPosition {
                    move(function_name),
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
            syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a function body", true);
        }
        if (!expect('}')) {
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = move(syntax_error);

            return create<AST::FunctionDeclaration>(
                AST::FunctionDeclaration::NameWithPosition {
                    move(function_name),
                    { pos_before_name.offset, pos_after_name.offset, pos_before_name.line, pos_after_name.line } },
                move(arguments),
                move(body));
        }
    }

    return create<AST::FunctionDeclaration>(
        AST::FunctionDeclaration::NameWithPosition {
            move(function_name),
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
    if (!expect("||"))
        return and_sequence;
    auto pos_after_or = save_offset();

    auto right_and_sequence = parse_and_logical_sequence();
    if (!right_and_sequence)
        right_and_sequence = create<AST::SyntaxError>("Expected an expression after '||'", true);

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
    if (!expect("&&"))
        return pipe_sequence;
    auto pos_after_end = save_offset();

    auto right_and_sequence = parse_and_logical_sequence();
    if (!right_and_sequence)
        right_and_sequence = create<AST::SyntaxError>("Expected an expression after '&&'", true);

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

    if (expect("break")) {
        {
            auto break_end = push_start();
            if (consume_while(is_any_of(" \t\n;")).is_empty()) {
                restore_to(*rule_start);
                return nullptr;
            }
            restore_to(*break_end);
        }
        return create<AST::ContinuationControl>(AST::ContinuationControl::Break);
    }

    if (expect("continue")) {
        {
            auto continue_end = push_start();
            if (consume_while(is_any_of(" \t\n;")).is_empty()) {
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
    if (!expect("for"))
        return nullptr;

    if (consume_while(is_any_of(" \t\n")).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    auto variable_name = consume_while(is_word_character);
    Optional<AST::Position> in_start_position;
    if (variable_name.is_empty()) {
        variable_name = "it";
    } else {
        consume_while(is_whitespace);
        auto in_error_start = push_start();
        if (!expect("in")) {
            auto syntax_error = create<AST::SyntaxError>("Expected 'in' after a variable name in a 'for' loop", true);
            return create<AST::ForLoop>(move(variable_name), move(syntax_error), nullptr); // ForLoop Var Iterated Block
        }
        in_start_position = AST::Position { in_error_start->offset, m_offset, in_error_start->line, line() };
    }

    consume_while(is_whitespace);
    RefPtr<AST::Node> iterated_expression;
    {
        auto iter_error_start = push_start();
        iterated_expression = parse_expression();
        if (!iterated_expression)
            iterated_expression = create<AST::SyntaxError>("Expected an expression in 'for' loop", true);
    }

    consume_while(is_any_of(" \t\n"));
    {
        auto obrace_error_start = push_start();
        if (!expect('{')) {
            auto syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a 'for' loop body", true);
            return create<AST::ForLoop>(move(variable_name), move(iterated_expression), move(syntax_error), move(in_start_position)); // ForLoop Var Iterated Block
        }
    }

    TemporaryChange controls { m_continuation_controls_allowed, true };
    auto body = parse_toplevel();

    {
        auto cbrace_error_start = push_start();
        if (!expect('}')) {
            auto error_start = push_start();
            auto syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a 'for' loop body", true);
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = syntax_error;
        }
    }

    return create<AST::ForLoop>(move(variable_name), move(iterated_expression), move(body), move(in_start_position)); // ForLoop Var Iterated Block
}

RefPtr<AST::Node> Parser::parse_loop_loop()
{
    auto rule_start = push_start();
    if (!expect("loop"))
        return nullptr;

    if (consume_while(is_any_of(" \t\n")).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    {
        auto obrace_error_start = push_start();
        if (!expect('{')) {
            auto syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a 'loop' loop body", true);
            return create<AST::ForLoop>(String::empty(), nullptr, move(syntax_error), Optional<AST::Position> {}); // ForLoop null null Block
        }
    }

    TemporaryChange controls { m_continuation_controls_allowed, true };
    auto body = parse_toplevel();

    {
        auto cbrace_error_start = push_start();
        if (!expect('}')) {
            auto error_start = push_start();
            auto syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a 'loop' loop body", true);
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = syntax_error;
        }
    }

    return create<AST::ForLoop>(String::empty(), nullptr, move(body), Optional<AST::Position> {}); // ForLoop null null Block
}

RefPtr<AST::Node> Parser::parse_if_expr()
{
    auto rule_start = push_start();
    if (!expect("if"))
        return nullptr;

    if (consume_while(is_any_of(" \t\n")).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    RefPtr<AST::Node> condition;
    {
        auto cond_error_start = push_start();
        condition = parse_or_logical_sequence();
        if (!condition)
            condition = create<AST::SyntaxError>("Expected a logical sequence after 'if'", true);
    }

    auto parse_braced_toplevel = [&]() -> RefPtr<AST::Node> {
        RefPtr<AST::Node> body;
        {
            auto obrace_error_start = push_start();
            if (!expect('{')) {
                body = create<AST::SyntaxError>("Expected an open brace '{' to start an 'if' true branch", true);
            }
        }

        if (!body)
            body = parse_toplevel();

        {
            auto cbrace_error_start = push_start();
            if (!expect('}')) {
                auto error_start = push_start();
                RefPtr<AST::SyntaxError> syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end an 'if' true branch", true);
                if (body)
                    body->set_is_syntax_error(*syntax_error);
                else
                    body = syntax_error;
            }
        }

        return body;
    };

    consume_while(is_any_of(" \t\n"));
    auto true_branch = parse_braced_toplevel();

    auto end_before_else = m_offset;
    auto line_before_else = line();
    consume_while(is_any_of(" \t\n"));
    Optional<AST::Position> else_position;
    {
        auto else_start = push_start();
        if (expect("else"))
            else_position = AST::Position { else_start->offset, m_offset, else_start->line, line() };
        else
            restore_to(end_before_else, line_before_else);
    }

    if (else_position.has_value()) {
        consume_while(is_any_of(" \t\n"));
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
            RefPtr<AST::SyntaxError> syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a subshell", true);
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
    if (!expect("match"))
        return nullptr;

    if (consume_while(is_whitespace).is_empty()) {
        restore_to(*rule_start);
        return nullptr;
    }

    auto match_expression = parse_expression();
    if (!match_expression) {
        return create<AST::MatchExpr>(
            create<AST::SyntaxError>("Expected an expression after 'match'", true),
            String {}, Optional<AST::Position> {}, Vector<AST::MatchEntry> {});
    }

    consume_while(is_any_of(" \t\n"));

    String match_name;
    Optional<AST::Position> as_position;
    auto as_start = m_offset;
    auto as_line = line();
    if (expect("as")) {
        as_position = AST::Position { as_start, m_offset, as_line, line() };

        if (consume_while(is_any_of(" \t\n")).is_empty()) {
            auto node = create<AST::MatchExpr>(
                match_expression.release_nonnull(),
                String {}, move(as_position), Vector<AST::MatchEntry> {});
            node->set_is_syntax_error(create<AST::SyntaxError>("Expected whitespace after 'as' in 'match'", true));
            return node;
        }

        match_name = consume_while(is_word_character);
        if (match_name.is_empty()) {
            auto node = create<AST::MatchExpr>(
                match_expression.release_nonnull(),
                String {}, move(as_position), Vector<AST::MatchEntry> {});
            node->set_is_syntax_error(create<AST::SyntaxError>("Expected an identifier after 'as' in 'match'", true));
            return node;
        }
    }

    consume_while(is_any_of(" \t\n"));

    if (!expect('{')) {
        auto node = create<AST::MatchExpr>(
            match_expression.release_nonnull(),
            move(match_name), move(as_position), Vector<AST::MatchEntry> {});
        node->set_is_syntax_error(create<AST::SyntaxError>("Expected an open brace '{' to start a 'match' entry list", true));
        return node;
    }

    consume_while(is_any_of(" \t\n"));

    Vector<AST::MatchEntry> entries;
    for (;;) {
        auto entry = parse_match_entry();
        consume_while(is_any_of(" \t\n"));
        if (entry.options.is_empty())
            break;

        entries.append(entry);
    }

    consume_while(is_any_of(" \t\n"));

    if (!expect('}')) {
        auto node = create<AST::MatchExpr>(
            match_expression.release_nonnull(),
            move(match_name), move(as_position), move(entries));
        node->set_is_syntax_error(create<AST::SyntaxError>("Expected a close brace '}' to end a 'match' entry list", true));
        return node;
    }

    return create<AST::MatchExpr>(match_expression.release_nonnull(), move(match_name), move(as_position), move(entries));
}

AST::MatchEntry Parser::parse_match_entry()
{
    auto rule_start = push_start();

    NonnullRefPtrVector<AST::Node> patterns;
    Vector<AST::Position> pipe_positions;
    Optional<Vector<String>> match_names;
    Optional<AST::Position> match_as_position;

    auto pattern = parse_match_pattern();
    if (!pattern)
        return { {}, {}, {}, {}, create<AST::SyntaxError>("Expected a pattern in 'match' body", true) };

    patterns.append(pattern.release_nonnull());

    consume_while(is_any_of(" \t\n"));

    auto previous_pipe_start_position = m_offset;
    auto previous_pipe_start_line = line();
    RefPtr<AST::SyntaxError> error;
    while (expect('|')) {
        pipe_positions.append({ previous_pipe_start_position, m_offset, previous_pipe_start_line, line() });
        consume_while(is_any_of(" \t\n"));
        auto pattern = parse_match_pattern();
        if (!pattern) {
            error = create<AST::SyntaxError>("Expected a pattern to follow '|' in 'match' body", true);
            break;
        }
        consume_while(is_any_of(" \t\n"));

        patterns.append(pattern.release_nonnull());

        previous_pipe_start_line = line();
        previous_pipe_start_position = m_offset;
    }

    consume_while(is_any_of(" \t\n"));

    auto as_start_position = m_offset;
    auto as_start_line = line();
    if (expect("as")) {
        match_as_position = AST::Position { as_start_position, m_offset, as_start_line, line() };
        consume_while(is_any_of(" \t\n"));
        if (!expect('(')) {
            if (!error)
                error = create<AST::SyntaxError>("Expected an explicit list of identifiers after a pattern 'as'");
        } else {
            match_names = Vector<String>();
            for (;;) {
                consume_while(is_whitespace);
                auto name = consume_while(is_word_character);
                if (name.is_empty())
                    break;
                match_names.value().append(move(name));
            }

            if (!expect(')')) {
                if (!error)
                    error = create<AST::SyntaxError>("Expected a close paren ')' to end the identifier list of pattern 'as'", true);
            }
        }
        consume_while(is_any_of(" \t\n"));
    }

    if (!expect('{')) {
        if (!error)
            error = create<AST::SyntaxError>("Expected an open brace '{' to start a match entry body", true);
    }

    auto body = parse_toplevel();

    if (!expect('}')) {
        if (!error)
            error = create<AST::SyntaxError>("Expected a close brace '}' to end a match entry body", true);
    }

    if (body && error)
        body->set_is_syntax_error(*error);
    else if (error)
        body = error;

    return { move(patterns), move(match_names), move(match_as_position), move(pipe_positions), move(body) };
}

RefPtr<AST::Node> Parser::parse_match_pattern()
{
    return parse_expression();
}

RefPtr<AST::Node> Parser::parse_redirection()
{
    auto rule_start = push_start();
    auto pipe_fd = 0;
    auto number = consume_while(is_digit);
    if (number.is_empty()) {
        pipe_fd = -1;
    } else {
        auto fd = number.to_int();
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
                path = create<AST::SyntaxError>("Expected a path after redirection", true);
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
                auto fd = number.to_int();
                dest_pipe_fd = fd.value_or(-1);
            }
            auto redir = create<AST::Fd2FdRedirection>(pipe_fd, dest_pipe_fd); // Redirection Fd2Fd
            if (dest_pipe_fd == -1)
                redir->set_is_syntax_error(*create<AST::SyntaxError>("Expected a file descriptor"));
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
            path = create<AST::SyntaxError>("Expected a path after redirection", true);
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
            path = create<AST::SyntaxError>("Expected a path after redirection", true);
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
        return create<AST::SyntaxError>(String::formatted("Expression nested too deep (max allowed is {})", max_allowed_nested_rule_depth));

    auto starting_char = peek();

    auto read_concat = [&](auto&& expr) -> NonnullRefPtr<AST::Node> {
        if (is_whitespace(peek()))
            return move(expr);

        if (auto next_expr = parse_expression())
            return create<AST::Juxtaposition>(move(expr), next_expr.release_nonnull());

        return move(expr);
    };

    if (strchr("&|)} ;<>\n", starting_char) != nullptr)
        return nullptr;

    if (m_extra_chars_not_allowed_in_barewords.contains_slow(starting_char))
        return nullptr;

    if (m_is_in_brace_expansion_spec && next_is(".."))
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

        if (auto inline_exec = parse_evaluate())
            return read_concat(inline_exec.release_nonnull());
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

    if (starting_char == '!') {
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

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_string()
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    if (peek() == '"') {
        consume();
        auto inner = parse_doublequoted_string_inner();
        if (!inner)
            inner = create<AST::SyntaxError>("Unexpected EOF in string", true);
        if (!expect('"')) {
            inner = create<AST::DoubleQuotedString>(move(inner));
            inner->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating double quote", true));
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
        auto result = create<AST::StringLiteral>(move(text)); // String Literal
        if (is_error)
            result->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating single quote", true));
        return move(result);
    }

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_doublequoted_string_inner()
{
    auto rule_start = push_start();
    if (at_end())
        return nullptr;

    StringBuilder builder;
    while (!at_end() && peek() != '"') {
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
            }
            continue;
        }
        if (peek() == '$') {
            auto string_literal = create<AST::StringLiteral>(builder.to_string()); // String Literal
            if (auto variable = parse_variable()) {
                auto inner = create<AST::StringPartCompose>(
                    move(string_literal),
                    variable.release_nonnull()); // Compose String Variable

                if (auto string = parse_doublequoted_string_inner()) {
                    return create<AST::StringPartCompose>(move(inner), string.release_nonnull()); // Compose Composition Composition
                }

                return inner;
            }

            if (auto evaluate = parse_evaluate()) {
                auto composition = create<AST::StringPartCompose>(
                    move(string_literal),
                    evaluate.release_nonnull()); // Compose String Sequence

                if (auto string = parse_doublequoted_string_inner()) {
                    return create<AST::StringPartCompose>(move(composition), string.release_nonnull()); // Compose Composition Composition
                }

                return composition;
            }
        }

        builder.append(consume());
    }

    return create<AST::StringLiteral>(builder.to_string()); // String Literal
}

RefPtr<AST::Node> Parser::parse_variable()
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

    return create<AST::SimpleVariable>(move(name)); // Variable Simple
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
            inner = create<AST::SyntaxError>("Unexpected EOF in list", true);
        if (!expect(')'))
            inner->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating close paren", true));

        return create<AST::Execute>(inner.release_nonnull(), true);
    }
    auto inner = parse_expression();

    if (!inner) {
        inner = create<AST::SyntaxError>("Expected a command", true);
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

    switch (peek()) {
    case '!':
        consume();
        selector.event.kind = AST::HistorySelector::EventKind::IndexFromEnd;
        selector.event.index = 0;
        selector.event.text = "!";
        break;
    case '?':
        consume();
        selector.event.kind = AST::HistorySelector::EventKind::ContainingStringLookup;
        [[fallthrough]];
    default: {
        TemporaryChange chars_change { m_extra_chars_not_allowed_in_barewords, { ':' } };

        auto bareword = parse_bareword();
        if (!bareword || !bareword->is_bareword()) {
            restore_to(*rule_start);
            return nullptr;
        }

        selector.event.text = static_ptr_cast<AST::BarewordLiteral>(bareword)->text();
        selector.event.text_position = bareword->position();
        auto it = selector.event.text.begin();
        bool is_negative = false;
        if (*it == '-') {
            ++it;
            is_negative = true;
        }
        if (it != selector.event.text.end() && AK::all_of(it, selector.event.text.end(), is_digit)) {
            if (is_negative)
                selector.event.kind = AST::HistorySelector::EventKind::IndexFromEnd;
            else
                selector.event.kind = AST::HistorySelector::EventKind::IndexFromStart;
            auto number = selector.event.text.to_int();
            if (number.has_value())
                selector.event.index = abs(number.value());
            else
                syntax_error = create<AST::SyntaxError>("History entry index value invalid or out of range");
        }
        break;
    }
    }

    if (peek() != ':') {
        auto node = create<AST::HistoryEvent>(move(selector));
        if (syntax_error)
            node->set_is_syntax_error(*syntax_error);
        return node;
    }

    consume();

    // Word selectors
    auto parse_word_selector = [&]() -> Optional<AST::HistorySelector::WordSelector> {
        auto rule_start = push_start();
        auto c = peek();
        if (isdigit(c)) {
            auto num = consume_while(is_digit);
            auto value = num.to_uint();
            if (!value.has_value()) {
                return AST::HistorySelector::WordSelector {
                    AST::HistorySelector::WordSelectorKind::Index,
                    0,
                    { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() },
                    syntax_error ? NonnullRefPtr(*syntax_error) : create<AST::SyntaxError>("Word selector value invalid or out of range")
                };
            }
            return AST::HistorySelector::WordSelector {
                AST::HistorySelector::WordSelectorKind::Index,
                value.value(),
                { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() },
                syntax_error
            };
        }
        if (c == '^') {
            consume();
            return AST::HistorySelector::WordSelector {
                AST::HistorySelector::WordSelectorKind::Index,
                0,
                { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() },
                syntax_error
            };
        }
        if (c == '$') {
            consume();
            return AST::HistorySelector::WordSelector {
                AST::HistorySelector::WordSelectorKind::Last,
                0,
                { m_rule_start_offsets.last(), m_offset, m_rule_start_lines.last(), line() },
                syntax_error
            };
        }
        return {};
    };

    auto start = parse_word_selector();
    if (!start.has_value()) {
        if (!syntax_error)
            syntax_error = create<AST::SyntaxError>("Expected a word selector after ':' in a history event designator", true);
        auto node = create<AST::HistoryEvent>(move(selector));
        node->set_is_syntax_error(*syntax_error);
        return node;
    }
    selector.word_selector_range.start = start.release_value();

    if (peek() == '-') {
        consume();
        auto end = parse_word_selector();
        if (!end.has_value()) {
            if (!syntax_error)
                syntax_error = create<AST::SyntaxError>("Expected a word selector after '-' in a history event designator word selector", true);
            auto node = create<AST::HistoryEvent>(move(selector));
            node->set_is_syntax_error(*syntax_error);
            return node;
        }
        selector.word_selector_range.end = move(end);
    } else {
        selector.word_selector_range.end.clear();
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
    return create<AST::Comment>(move(text)); // Comment
}

RefPtr<AST::Node> Parser::parse_bareword()
{
    auto rule_start = push_start();
    StringBuilder builder;
    auto is_acceptable_bareword_character = [&](char c) {
        return strchr("\\\"'*$&#|(){} ?;<>\n", c) == nullptr
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

        if (m_is_in_brace_expansion_spec && next_is("..")) {
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
    auto string = builder.to_string();
    if (string.starts_with('~')) {
        String username;
        RefPtr<AST::Node> tilde, text;

        auto first_slash_index = string.index_of("/");
        if (first_slash_index.has_value()) {
            username = string.substring_view(1, first_slash_index.value() - 1);
            string = string.substring_view(first_slash_index.value(), string.length() - first_slash_index.value());
        } else {
            username = string.substring_view(1, string.length() - 1);
            string = "";
        }

        // Synthesize a Tilde Node with the correct positioning information.
        {
            restore_to(rule_start->offset, rule_start->line);
            auto ch = consume();
            VERIFY(ch == '~');
            tilde = create<AST::Tilde>(move(username));
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

    if (string.starts_with("\\~")) {
        // Un-escape the tilde, but only at the start (where it would be an expansion)
        string = string.substring(1, string.length() - 1);
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
                bareword_part->set_is_syntax_error(*create<AST::SyntaxError>(String::format("Unexpected %s inside a glob", bareword_part->class_name().characters())));
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
                textbuilder.append("~");
                textbuilder.append(bareword->text());
            } else {
                return create<AST::SyntaxError>(String::formatted("Invalid node '{}' in glob position, escape shell special characters", glob_after->class_name()));
            }
        }

        return create<AST::Glob>(textbuilder.to_string()); // Glob
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
            spec->set_is_syntax_error(create<AST::SyntaxError>("Expected a close brace '}' to end a brace expansion", true));

        return spec;
    }

    restore_to(*rule_start);
    return nullptr;
}

RefPtr<AST::Node> Parser::parse_brace_expansion_spec()
{
    TemporaryChange is_in_brace_expansion { m_is_in_brace_expansion_spec, true };
    TemporaryChange chars_change { m_extra_chars_not_allowed_in_barewords, { ',' } };

    auto rule_start = push_start();
    auto start_expr = parse_expression();
    if (start_expr) {
        if (expect("..")) {
            if (auto end_expr = parse_expression()) {
                if (end_expr->position().start_offset != start_expr->position().end_offset + 2)
                    end_expr->set_is_syntax_error(create<AST::SyntaxError>("Expected no whitespace between '..' and the following expression in brace expansion"));

                return create<AST::Range>(start_expr.release_nonnull(), end_expr.release_nonnull());
            }

            return create<AST::Range>(start_expr.release_nonnull(), create<AST::SyntaxError>("Expected an expression to end range brace expansion with", true));
        }
    }

    NonnullRefPtrVector<AST::Node> subexpressions;
    if (start_expr)
        subexpressions.append(start_expr.release_nonnull());

    while (expect(',')) {
        auto expr = parse_expression();
        if (expr) {
            subexpressions.append(expr.release_nonnull());
        } else {
            subexpressions.append(create<AST::StringLiteral>(""));
        }
    }

    if (subexpressions.is_empty())
        return nullptr;

    return create<AST::BraceExpansion>(move(subexpressions));
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

bool Parser::next_is(const StringView& next)
{
    auto start = push_start();
    auto res = expect(next);
    restore_to(*start);
    return res;
}

}
