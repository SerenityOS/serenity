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
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

char Parser::peek()
{
    if (m_offset == m_input.length())
        return 0;

    ASSERT(m_offset < m_input.length());
    return m_input[m_offset];
}

char Parser::consume()
{
    auto ch = peek();
    ++m_offset;
    return ch;
}

void Parser::putback()
{
    ASSERT(m_offset > 0);
    --m_offset;
}

bool Parser::expect(char ch)
{
    return expect(StringView { &ch, 1 });
}

bool Parser::expect(const StringView& expected)
{
    if (expected.length() + m_offset > m_input.length())
        return false;

    for (size_t i = 0; i < expected.length(); ++i) {
        if (peek() != expected[i])
            return false;

        consume();
    }

    return true;
}

template<typename A, typename... Args>
NonnullRefPtr<A> Parser::create(Args... args)
{
    return adopt(*new A(AST::Position { m_rule_start_offsets.last(), m_offset }, args...));
}

[[nodiscard]] OwnPtr<Parser::ScopedOffset> Parser::push_start()
{
    return make<ScopedOffset>(m_rule_start_offsets, m_offset);
}

static constexpr bool is_whitespace(char c)
{
    return c == ' ' || c == '\t';
}

static constexpr bool is_word_character(char c)
{
    return (c <= '9' && c >= '0') || (c <= 'Z' && c >= 'A') || (c <= 'z' && c >= 'a') || c == '_';
}

static constexpr bool is_digit(char c)
{
    return c <= '9' && c >= '0';
}

static constexpr auto is_not(char c)
{
    return [c](char ch) { return ch != c; };
}

static constexpr auto is_any_of(StringView s)
{
    return [s](char ch) { return s.contains(ch); };
}

static inline char to_byte(char a, char b)
{
    char buf[3] { a, b, 0 };
    return strtol(buf, nullptr, 16);
}

RefPtr<AST::Node> Parser::parse()
{
    m_offset = 0;

    auto toplevel = parse_toplevel();

    if (m_offset < m_input.length()) {
        // Parsing stopped midway, this is a syntax error.
        auto error_start = push_start();
        m_offset = m_input.length();
        auto syntax_error_node = create<AST::SyntaxError>("Unexpected tokens past the end");
        if (toplevel)
            return create<AST::Join>(move(toplevel), move(syntax_error_node));
        return syntax_error_node;
    }

    return toplevel;
}

RefPtr<AST::Node> Parser::parse_toplevel()
{
    auto rule_start = push_start();

    if (auto sequence = parse_sequence())
        return create<AST::Execute>(sequence);

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_sequence()
{
    consume_while(is_any_of(" \t\n;")); // ignore whitespaces or terminators without effect.

    auto rule_start = push_start();
    auto var_decls = parse_variable_decls();

    switch (peek()) {
    case '}':
        return var_decls;
    case ';':
    case '\n': {
        if (!var_decls)
            break;

        consume_while(is_any_of("\n;"));
        auto rest = parse_sequence();
        if (rest)
            return create<AST::Sequence>(move(var_decls), move(rest));
        return var_decls;
    }
    default:
        break;
    }

    auto first = parse_or_logical_sequence();

    if (!first)
        return var_decls;

    if (var_decls)
        first = create<AST::Sequence>(move(var_decls), move(first));

    consume_while(is_whitespace);

    switch (peek()) {
    case ';':
    case '\n':
        consume_while(is_any_of("\n;"));
        if (auto expr = parse_sequence()) {
            return create<AST::Sequence>(move(first), move(expr)); // Sequence
        }
        return first;
    case '&': {
        auto execute_pipe_seq = first->would_execute() ? first : static_cast<RefPtr<AST::Node>>(create<AST::Execute>(first));
        consume();
        auto bg = create<AST::Background>(move(first)); // Execute Background
        if (auto rest = parse_sequence())
            return create<AST::Sequence>(move(bg), move(rest)); // Sequence Background Sequence

        return bg;
    }
    default:
        return first;
    }
}

RefPtr<AST::Node> Parser::parse_variable_decls()
{
    auto rule_start = push_start();

    consume_while(is_whitespace);

    auto offset_before_name = m_offset;
    auto var_name = consume_while(is_word_character);
    if (var_name.is_empty())
        return nullptr;

    if (!expect('=')) {
        m_offset = offset_before_name;
        return nullptr;
    }

    auto name_expr = create<AST::BarewordLiteral>(move(var_name));

    auto start = push_start();
    auto expression = parse_expression();
    if (!expression || expression->is_syntax_error()) {
        m_offset = start->offset;
        if (peek() == '(') {
            consume();
            auto command = parse_pipe_sequence();
            if (!command)
                m_offset = start->offset;
            else if (!expect(')'))
                command->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating close paren"));
            expression = command;
        }
    }
    if (!expression) {
        if (is_whitespace(peek())) {
            auto string_start = push_start();
            expression = create<AST::StringLiteral>("");
        } else {
            m_offset = offset_before_name;
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

    ASSERT(rest->is_variable_decls());
    auto* rest_decl = static_cast<AST::VariableDeclarations*>(rest.ptr());

    variables.append(rest_decl->variables());

    return create<AST::VariableDeclarations>(move(variables));
}

RefPtr<AST::Node> Parser::parse_or_logical_sequence()
{
    consume_while(is_whitespace);
    auto rule_start = push_start();
    auto and_sequence = parse_and_logical_sequence();
    if (!and_sequence)
        return nullptr;

    consume_while(is_whitespace);
    auto saved_offset = m_offset;
    if (!expect("||")) {
        m_offset = saved_offset;
        return and_sequence;
    }

    auto right_and_sequence = parse_and_logical_sequence();
    if (!right_and_sequence)
        right_and_sequence = create<AST::SyntaxError>("Expected an expression after '||'");

    return create<AST::Or>(move(and_sequence), move(right_and_sequence));
}

RefPtr<AST::Node> Parser::parse_and_logical_sequence()
{
    consume_while(is_whitespace);
    auto rule_start = push_start();
    auto pipe_sequence = parse_pipe_sequence();
    if (!pipe_sequence)
        return nullptr;

    consume_while(is_whitespace);
    auto saved_offset = m_offset;
    if (!expect("&&")) {
        m_offset = saved_offset;
        return pipe_sequence;
    }

    auto right_and_sequence = parse_and_logical_sequence();
    if (!right_and_sequence)
        right_and_sequence = create<AST::SyntaxError>("Expected an expression after '&&'");

    return create<AST::And>(move(pipe_sequence), move(right_and_sequence));
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

    consume();

    if (auto pipe_seq = parse_pipe_sequence()) {
        return create<AST::Pipe>(move(left), move(pipe_seq)); // Pipe
    }

    putback();
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

        auto cast = create<AST::CastToCommand>(move(list_expr)); // Cast List Command

        auto next_command = parse_command();
        if (!next_command)
            return cast;

        return create<AST::Join>(move(cast), move(next_command)); // Join List Command
    }

    auto command = parse_command();
    if (!command)
        return redir;

    return create<AST::Join>(move(redir), command); // Join Command Command
}

RefPtr<AST::Node> Parser::parse_control_structure()
{
    auto rule_start = push_start();
    consume_while(is_whitespace);
    if (auto for_loop = parse_for_loop())
        return for_loop;

    if (auto if_expr = parse_if_expr())
        return if_expr;

    return nullptr;
}

RefPtr<AST::Node> Parser::parse_for_loop()
{
    auto rule_start = push_start();
    if (!expect("for")) {
        m_offset = rule_start->offset;
        return nullptr;
    }

    if (consume_while(is_any_of(" \t\n")).is_empty()) {
        m_offset = rule_start->offset;
        return nullptr;
    }

    auto variable_name = consume_while(is_word_character);
    Optional<size_t> in_start_position;
    if (variable_name.is_empty()) {
        variable_name = "it";
    } else {
        consume_while(is_whitespace);
        auto in_error_start = push_start();
        in_start_position = in_error_start->offset;
        if (!expect("in")) {
            auto syntax_error = create<AST::SyntaxError>("Expected 'in' after a variable name in a 'for' loop");
            return create<AST::ForLoop>(move(variable_name), move(syntax_error), nullptr); // ForLoop Var Iterated Block
        }
    }

    consume_while(is_whitespace);
    RefPtr<AST::Node> iterated_expression;
    {
        auto iter_error_start = push_start();
        iterated_expression = parse_expression();
        if (!iterated_expression) {
            auto syntax_error = create<AST::SyntaxError>("Expected an expression in 'for' loop");
            return create<AST::ForLoop>(move(variable_name), move(syntax_error), nullptr, move(in_start_position)); // ForLoop Var Iterated Block
        }
    }

    consume_while(is_any_of(" \t\n"));
    {
        auto obrace_error_start = push_start();
        if (!expect('{')) {
            auto syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start a 'for' loop body");
            return create<AST::ForLoop>(move(variable_name), move(iterated_expression), move(syntax_error), move(in_start_position)); // ForLoop Var Iterated Block
        }
    }

    auto body = parse_toplevel();

    {
        auto cbrace_error_start = push_start();
        if (!expect('}')) {
            auto error_start = push_start();
            RefPtr<AST::SyntaxError> syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end a 'for' loop body");
            if (body)
                body->set_is_syntax_error(*syntax_error);
            else
                body = syntax_error;
        }
    }

    return create<AST::ForLoop>(move(variable_name), move(iterated_expression), move(body), move(in_start_position)); // ForLoop Var Iterated Block
}

RefPtr<AST::Node> Parser::parse_if_expr()
{
    auto rule_start = push_start();
    if (!expect("if")) {
        m_offset = rule_start->offset;
        return nullptr;
    }

    if (consume_while(is_any_of(" \t\n")).is_empty()) {
        m_offset = rule_start->offset;
        return nullptr;
    }

    RefPtr<AST::Node> condition;
    {
        auto cond_error_start = push_start();
        condition = parse_or_logical_sequence();
        if (!condition) {
            auto syntax_error = create<AST::SyntaxError>("Expected a logical sequence after 'if'");
            return create<AST::IfCond>(Optional<AST::Position> {}, move(syntax_error), nullptr, nullptr);
        }
    }

    auto parse_braced_toplevel = [&]() -> RefPtr<AST::Node> {
        {
            auto obrace_error_start = push_start();
            if (!expect('{')) {
                auto syntax_error = create<AST::SyntaxError>("Expected an open brace '{' to start an 'if' true branch");
                return syntax_error;
            }
        }

        auto body = parse_toplevel();

        {
            auto cbrace_error_start = push_start();
            if (!expect('}')) {
                auto error_start = push_start();
                RefPtr<AST::SyntaxError> syntax_error = create<AST::SyntaxError>("Expected a close brace '}' to end an 'if' true branch");
                if (body)
                    body->set_is_syntax_error(*syntax_error);
                else
                    body = syntax_error;
            }
        }

        return body;
    };

    consume_while(is_whitespace);
    auto true_branch = parse_braced_toplevel();

    if (true_branch && true_branch->is_syntax_error())
        return create<AST::IfCond>(Optional<AST::Position> {}, move(condition), move(true_branch), nullptr); // If expr syntax_error

    consume_while(is_whitespace);
    Optional<AST::Position> else_position;
    {
        auto else_start = push_start();
        if (expect("else"))
            else_position = AST::Position { else_start->offset, m_offset };
    }

    if (else_position.has_value()) {
        consume_while(is_whitespace);
        if (peek() == '{') {
            auto false_branch = parse_braced_toplevel();
            return create<AST::IfCond>(else_position, move(condition), move(true_branch), move(false_branch)); // If expr true_branch Else false_branch
        }

        auto else_if_branch = parse_if_expr();
        return create<AST::IfCond>(else_position, move(condition), move(true_branch), move(else_if_branch)); // If expr true_branch Else If ...
    }

    return create<AST::IfCond>(else_position, move(condition), move(true_branch), nullptr); // If expr true_branch
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
        ASSERT(fd.has_value());
        pipe_fd = fd.value();
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
                return create<AST::SyntaxError>("Expected a path");
            }
            return create<AST::WriteAppendRedirection>(pipe_fd, move(path)); // Redirection WriteAppend
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
                ASSERT(fd.has_value());
                dest_pipe_fd = fd.value();
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
            return create<AST::SyntaxError>("Expected a path");
        }
        return create<AST::WriteRedirection>(pipe_fd, move(path)); // Redirection Write
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
            return create<AST::SyntaxError>("Expected a path");
        }
        if (mode == Read)
            return create<AST::ReadRedirection>(pipe_fd, move(path)); // Redirection Read

        return create<AST::ReadWriteRedirection>(pipe_fd, move(path)); // Redirection ReadWrite
    }
    default:
        m_offset = rule_start->offset;
        return nullptr;
    }
}

RefPtr<AST::Node> Parser::parse_list_expression()
{
    consume_while(is_whitespace);

    auto rule_start = push_start();
    Vector<RefPtr<AST::Node>> nodes;

    do {
        auto expr = parse_expression();
        if (!expr)
            break;
        nodes.append(move(expr));
    } while (!consume_while(is_whitespace).is_empty());

    if (nodes.is_empty())
        return nullptr;

    return create<AST::ListConcatenate>(move(nodes)); // Concatenate List
}

RefPtr<AST::Node> Parser::parse_expression()
{
    auto rule_start = push_start();
    auto starting_char = peek();

    auto read_concat = [&](auto expr) -> RefPtr<AST::Node> {
        if (is_whitespace(peek()))
            return expr;

        if (auto next_expr = parse_expression())
            return create<AST::Juxtaposition>(move(expr), move(next_expr));

        return expr;
    };

    if (strchr("&|){} ;<>\n", starting_char) != nullptr)
        return nullptr;

    if (isdigit(starting_char)) {
        ScopedValueRollback offset_rollback { m_offset };

        auto redir = parse_redirection();
        if (redir)
            return nullptr;
    }

    if (starting_char == '$') {
        if (auto variable = parse_variable())
            return read_concat(variable);

        if (auto inline_exec = parse_evaluate())
            return read_concat(inline_exec);
    }

    if (starting_char == '#')
        return parse_comment();

    if (starting_char == '(') {
        consume();
        auto list = parse_list_expression();
        if (!expect(')')) {
            m_offset = rule_start->offset;
            return nullptr;
        }
        return read_concat(create<AST::CastToList>(move(list))); // Cast To List
    }

    return read_concat(parse_string_composite());
}

RefPtr<AST::Node> Parser::parse_string_composite()
{
    auto rule_start = push_start();
    if (auto string = parse_string()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(move(string), move(next_part)); // Concatenate String StringComposite

        return string;
    }

    if (auto variable = parse_variable()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(move(variable), move(next_part)); // Concatenate Variable StringComposite

        return variable;
    }

    if (auto glob = parse_glob()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(move(glob), move(next_part)); // Concatenate Glob StringComposite

        return glob;
    }

    if (auto bareword = parse_bareword()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(move(bareword), move(next_part)); // Concatenate Bareword StringComposite

        return bareword;
    }

    if (auto inline_command = parse_evaluate()) {
        if (auto next_part = parse_string_composite())
            return create<AST::Juxtaposition>(move(inline_command), move(next_part)); // Concatenate Execute StringComposite

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
            inner = create<AST::SyntaxError>("Unexpected EOF in string");
        if (!expect('"')) {
            inner = create<AST::DoubleQuotedString>(move(inner));
            inner->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating double quote"));
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
            result->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating single quote"));
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
                    move(variable)); // Compose String Variable

                if (auto string = parse_doublequoted_string_inner()) {
                    return create<AST::StringPartCompose>(move(inner), move(string)); // Compose Composition Composition
                }

                return inner;
            }

            if (auto evaluate = parse_evaluate()) {
                auto composition = create<AST::StringPartCompose>(
                    move(string_literal),
                    move(evaluate)); // Compose String Sequence

                if (auto string = parse_doublequoted_string_inner()) {
                    return create<AST::StringPartCompose>(move(composition), move(string)); // Compose Composition Composition
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
        putback();
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
            inner = create<AST::SyntaxError>("Unexpected EOF in list");
        if (!expect(')'))
            inner->set_is_syntax_error(*create<AST::SyntaxError>("Expected a terminating close paren"));
        return create<AST::Execute>(move(inner), true);
    }
    auto inner = parse_expression();

    if (!inner) {
        inner = create<AST::SyntaxError>("Expected a command");
    } else {
        if (inner->is_list()) {
            auto execute_inner = create<AST::Execute>(move(inner), true);
            inner = execute_inner;
        } else {
            auto dyn_inner = create<AST::DynamicEvaluate>(move(inner));
            inner = dyn_inner;
        }
    }

    return inner;
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
    auto is_acceptable_bareword_character = [](char c) {
        return strchr("\\\"'*$&#|(){} ?;<>\n", c) == nullptr;
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

        if (is_acceptable_bareword_character(ch)) {
            builder.append(consume());
            continue;
        }

        break;
    }

    if (builder.is_empty())
        return nullptr;

    auto current_end = m_offset;
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
            m_offset -= string.length();
            tilde = create<AST::Tilde>(move(username));
        }

        if (string.is_empty())
            return tilde;

        // Synthesize a BarewordLiteral Node with the correct positioning information.
        {
            m_offset = tilde->position().end_offset;
            auto text_start = push_start();
            m_offset = current_end;
            text = create<AST::BarewordLiteral>(move(string));
        }

        return create<AST::Juxtaposition>(move(tilde), move(text)); // Juxtaposition Varible Bareword
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
        consume();
        StringBuilder textbuilder;
        if (bareword_part) {
            StringView text;
            if (bareword_part->is_bareword()) {
                auto bareword = static_cast<AST::BarewordLiteral*>(bareword_part.ptr());
                text = bareword->text();
            } else {
                // FIXME: Allow composition of tilde+bareword with globs: '~/foo/bar/baz*'
                putback();
                bareword_part->set_is_syntax_error(*create<AST::SyntaxError>(String::format("Unexpected %s inside a glob", bareword_part->class_name().characters())));
                return bareword_part;
            }
            textbuilder.append(text);
        }

        textbuilder.append(ch);

        auto glob_after = parse_glob();
        if (glob_after) {
            if (glob_after->is_glob()) {
                auto glob = static_cast<AST::BarewordLiteral*>(glob_after.ptr());
                textbuilder.append(glob->text());
            } else if (glob_after->is_bareword()) {
                auto bareword = static_cast<AST::BarewordLiteral*>(glob_after.ptr());
                textbuilder.append(bareword->text());
            } else {
                ASSERT_NOT_REACHED();
            }
        }

        return create<AST::Glob>(textbuilder.to_string()); // Glob
    }

    return bareword_part;
}

StringView Parser::consume_while(Function<bool(char)> condition)
{
    auto start_offset = m_offset;

    while (!at_end() && condition(peek()))
        consume();

    return m_input.substring_view(start_offset, m_offset - start_offset);
}
