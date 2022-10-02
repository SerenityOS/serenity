/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <Shell/PosixLexer.h>

static bool is_operator(StringView text)
{
    return Shell::Posix::Token::operator_from_name(text).has_value();
}

static bool is_part_of_operator(StringView text, char ch)
{
    StringBuilder builder;
    builder.append(text);
    builder.append(ch);

    return Shell::Posix::Token::operator_from_name(builder.string_view()).has_value();
}

namespace Shell::Posix {
Vector<Token> Lexer::batch_next()
{
    if (m_next_reduction == Reduction::None)
        return { Token::eof() };
    auto result = reduce(m_next_reduction);
    m_next_reduction = result.next_reduction;
    return result.tokens;
}

char Lexer::consume()
{
    auto ch = m_lexer.consume();
    if (ch == '\n') {
        m_state.position.end_line.line_number++;
        m_state.position.end_line.line_column = 0;
    }

    m_state.position.end_offset++;
    return ch;
}

bool Lexer::consume_specific(char ch)
{
    if (m_lexer.peek() == ch) {
        consume();
        return true;
    }
    return false;
}

Lexer::ReductionResult Lexer::reduce(Reduction reduction)
{
    switch (reduction) {
    case Reduction::None:
        return { {}, Reduction::None };
    case Reduction::End:
        return reduce_end();
    case Reduction::Operator:
        return reduce_operator();
    case Reduction::Comment:
        return reduce_comment();
    case Reduction::SingleQuotedString:
        return reduce_single_quoted_string();
    case Reduction::DoubleQuotedString:
        return reduce_double_quoted_string();
    case Reduction::Expansion:
        return reduce_expansion();
    case Reduction::CommandExpansion:
        return reduce_command_expansion();
    case Reduction::Start:
        return reduce_start();
    case Reduction::ArithmeticExpansion:
        return reduce_arithmetic_expansion();
    case Reduction::SpecialParameterExpansion:
        return reduce_special_parameter_expansion();
    case Reduction::ParameterExpansion:
        return reduce_parameter_expansion();
    case Reduction::CommandOrArithmeticSubstitutionExpansion:
        return reduce_command_or_arithmetic_substitution_expansion();
    case Reduction::ExtendedParameterExpansion:
        return reduce_extended_parameter_expansion();
    }

    VERIFY_NOT_REACHED();
}

Lexer::ReductionResult Lexer::reduce_end()
{
    return {
        .tokens = { Token::eof() },
        .next_reduction = Reduction::None,
    };
}

Lexer::ReductionResult Lexer::reduce_operator()
{
    if (m_lexer.is_eof()) {
        if (is_operator(m_state.buffer.string_view())) {
            auto tokens = Token::operators_from(m_state);
            m_state.buffer.clear();
            m_state.position.start_offset = m_state.position.end_offset;
            m_state.position.start_line = m_state.position.end_line;

            return {
                .tokens = move(tokens),
                .next_reduction = Reduction::End,
            };
        }

        return reduce(Reduction::Start);
    }

    if (is_part_of_operator(m_state.buffer.string_view(), m_lexer.peek())) {
        m_state.buffer.append(consume());
        return {
            .tokens = {},
            .next_reduction = Reduction::Operator,
        };
    }

    auto tokens = Vector<Token> {};
    if (is_operator(m_state.buffer.string_view())) {
        tokens.extend(Token::operators_from(m_state));
        m_state.buffer.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;
    }

    auto result = reduce(Reduction::Start);
    tokens.extend(move(result.tokens));
    return {
        .tokens = move(tokens),
        .next_reduction = result.next_reduction,
    };
}

Lexer::ReductionResult Lexer::reduce_comment()
{
    if (m_lexer.is_eof()) {
        return {
            .tokens = {},
            .next_reduction = Reduction::End,
        };
    }

    if (consume() == '\n') {
        return {
            .tokens = { Token::newline() },
            .next_reduction = Reduction::Start,
        };
    }

    return {
        .tokens = {},
        .next_reduction = Reduction::Comment,
    };
}

Lexer::ReductionResult Lexer::reduce_single_quoted_string()
{
    if (m_lexer.is_eof()) {
        auto tokens = Token::maybe_from_state(m_state);
        tokens.append(Token::continuation('\''));
        return {
            .tokens = move(tokens),
            .next_reduction = Reduction::None,
        };
    }

    auto ch = consume();
    m_state.buffer.append(ch);

    if (ch == '\'') {
        return {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    return {
        .tokens = {},
        .next_reduction = Reduction::SingleQuotedString,
    };
}

Lexer::ReductionResult Lexer::reduce_double_quoted_string()
{
    m_state.previous_reduction = Reduction::DoubleQuotedString;
    if (m_lexer.is_eof()) {
        auto tokens = Token::maybe_from_state(m_state);
        tokens.append(Token::continuation('"'));
        return {
            .tokens = move(tokens),
            .next_reduction = Reduction::None,
        };
    }

    auto ch = consume();
    m_state.buffer.append(ch);

    if (m_state.escaping) {
        m_state.escaping = false;

        return {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    }

    switch (ch) {
    case '\\':
        m_state.escaping = true;
        return {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    case '"':
        m_state.previous_reduction = Reduction::Start;
        return {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    case '$':
        if (m_lexer.next_is("("))
            m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .location = m_state.position });
        else
            m_state.expansions.empend(ParameterExpansion { .parameter = StringBuilder {} });
        return {
            .tokens = {},
            .next_reduction = Reduction::Expansion,
        };
    case '`':
        m_state.expansions.empend(CommandExpansion { StringBuilder {}, m_state.position });
        return {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    default:
        return {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    }
}

Lexer::ReductionResult Lexer::reduce_expansion()
{
    if (m_lexer.is_eof())
        return reduce(m_state.previous_reduction);

    auto ch = m_lexer.peek();

    switch (ch) {
    case '{':
        consume();
        m_state.buffer.append(ch);
        return {
            .tokens = {},
            .next_reduction = Reduction::ExtendedParameterExpansion,
        };
    case '(':
        consume();
        m_state.buffer.append(ch);
        return {
            .tokens = {},
            .next_reduction = Reduction::CommandOrArithmeticSubstitutionExpansion,
        };
    case 'a' ... 'z':
    case 'A' ... 'Z':
    case '_':
        consume();
        m_state.buffer.append(ch);
        m_state.expansions.last().get<ParameterExpansion>().parameter.append(ch);
        return {
            .tokens = {},
            .next_reduction = Reduction::ParameterExpansion,
        };
    case '0' ... '9':
    case '-':
    case '!':
    case '@':
    case '#':
    case '?':
    case '*':
    case '$':
        return reduce(Reduction::SpecialParameterExpansion);
    default:
        m_state.buffer.append(ch);
        return reduce(m_state.previous_reduction);
    }
}

Lexer::ReductionResult Lexer::reduce_command_expansion()
{
    if (m_lexer.is_eof()) {
        auto& expansion = m_state.expansions.last().get<CommandExpansion>();
        expansion.location.end_line = m_state.position.end_line;
        expansion.location.end_offset = m_state.position.end_offset;

        return {
            .tokens = { Token::continuation('`') },
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = consume();

    if (!m_state.escaping && ch == '`') {
        m_state.buffer.append(ch);
        auto& expansion = m_state.expansions.last().get<CommandExpansion>();
        expansion.location.end_line = m_state.position.end_line;
        expansion.location.end_offset = m_state.position.end_offset;

        return {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    if (!m_state.escaping && ch == '\\') {
        m_state.escaping = true;
        return {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    }

    m_state.escaping = false;
    m_state.buffer.append(ch);
    m_state.expansions.last().get<CommandExpansion>().command.append(ch);
    return {
        .tokens = {},
        .next_reduction = Reduction::CommandExpansion,
    };
}

Lexer::ReductionResult Lexer::reduce_start()
{
    if (m_lexer.is_eof()) {
        auto tokens = Token::maybe_from_state(m_state);
        dbgln("EOF in start, {} tokens, buffer was '{}'", tokens.size(), m_state.buffer.string_view());
        m_state.buffer.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return {
            .tokens = move(tokens),
            .next_reduction = Reduction::End,
        };
    }

    if (m_state.escaping && consume_specific('\n')) {
        m_state.escaping = false;

        auto buffer = m_state.buffer.to_string().substring(0, m_state.buffer.length() - 1);
        m_state.buffer.clear();
        m_state.buffer.append(buffer);

        return {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && m_lexer.peek() == '#' && m_state.buffer.is_empty()) {
        consume();
        return {
            .tokens = {},
            .next_reduction = Reduction::Comment,
        };
    }

    if (!m_state.escaping && consume_specific('\n')) {
        auto tokens = Token::maybe_from_state(m_state);
        tokens.append(Token::newline());

        m_state.buffer.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return {
            .tokens = move(tokens),
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && consume_specific('\\')) {
        m_state.escaping = true;
        m_state.buffer.append('\\');
        return {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && is_part_of_operator(""sv, m_lexer.peek())) {
        auto tokens = Token::maybe_from_state(m_state);
        m_state.buffer.clear();
        m_state.buffer.append(consume());
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return {
            .tokens = move(tokens),
            .next_reduction = Reduction::Operator,
        };
    }

    if (!m_state.escaping && consume_specific('\'')) {
        m_state.buffer.append('\'');
        return {
            .tokens = {},
            .next_reduction = Reduction::SingleQuotedString,
        };
    }

    if (!m_state.escaping && consume_specific('"')) {
        m_state.buffer.append('"');
        return {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    }

    if (!m_state.escaping && is_ascii_space(m_lexer.peek())) {
        consume();
        auto tokens = Token::maybe_from_state(m_state);
        m_state.buffer.clear();
        m_state.expansions.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return {
            .tokens = move(tokens),
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && consume_specific('$')) {
        m_state.buffer.append('$');
        if (m_lexer.next_is("("))
            m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .location = m_state.position });
        else
            m_state.expansions.empend(ParameterExpansion { .parameter = StringBuilder {} });

        return {
            .tokens = {},
            .next_reduction = Reduction::Expansion,
        };
    }

    if (!m_state.escaping && consume_specific('`')) {
        m_state.buffer.append('`');
        m_state.expansions.empend(CommandExpansion { StringBuilder {}, m_state.position });
        return {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    }

    m_state.escaping = false;
    m_state.buffer.append(consume());
    return {
        .tokens = {},
        .next_reduction = Reduction::Start,
    };
}

Lexer::ReductionResult Lexer::reduce_arithmetic_expansion()
{
    if (m_lexer.is_eof()) {
        auto& expansion = m_state.expansions.last().get<ArithmeticExpansion>();
        expansion.location.end_line = m_state.position.end_line;
        expansion.location.end_offset = m_state.position.end_offset;

        return {
            .tokens = { Token::continuation("$((") },
            .next_reduction = m_state.previous_reduction,
        };
    }

    if (m_lexer.peek() == ')' && m_state.buffer.string_view().ends_with(')')) {
        m_state.buffer.append(consume());
        auto& expansion = m_state.expansions.last().get<ArithmeticExpansion>();
        expansion.expression = expansion.value.to_string().substring(0, expansion.value.length() - 1);
        expansion.value.clear();
        expansion.location.end_line = m_state.position.end_line;
        expansion.location.end_offset = m_state.position.end_offset;

        return {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = consume();
    m_state.buffer.append(ch);
    m_state.expansions.last().get<ArithmeticExpansion>().value.append(ch);
    return {
        .tokens = {},
        .next_reduction = Reduction::ArithmeticExpansion,
    };
}

Lexer::ReductionResult Lexer::reduce_special_parameter_expansion()
{
    auto ch = consume();
    m_state.buffer.append(ch);
    m_state.expansions.last() = ParameterExpansion {
        StringBuilder {}
    };
    m_state.expansions.last().get<ParameterExpansion>().parameter.append(ch);

    return {
        .tokens = {},
        .next_reduction = m_state.previous_reduction,
    };
}

Lexer::ReductionResult Lexer::reduce_parameter_expansion()
{
    if (m_lexer.is_eof()) {
        return {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    auto next = m_lexer.peek();
    if (is_ascii_alphanumeric(next)) {
        m_state.buffer.append(consume());
        m_state.expansions.last().get<ParameterExpansion>().parameter.append(next);
        return {
            .tokens = {},
            .next_reduction = Reduction::ParameterExpansion,
        };
    }

    return reduce(m_state.previous_reduction);
}

Lexer::ReductionResult Lexer::reduce_command_or_arithmetic_substitution_expansion()
{
    if (m_lexer.is_eof()) {
        return {
            .tokens = { Token::continuation("$(") },
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = m_lexer.peek();
    if (ch == '(' && m_state.buffer.string_view().ends_with("$("sv)) {
        m_state.buffer.append(consume());
        m_state.expansions.last() = ArithmeticExpansion {
            .expression = "",
            .value = StringBuilder {},
            .location = m_state.position,
        };
        return {
            .tokens = {},
            .next_reduction = Reduction::ArithmeticExpansion,
        };
    }

    if (ch == ')') {
        m_state.buffer.append(consume());
        return {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    m_state.buffer.append(consume());
    m_state.expansions.last().get<CommandExpansion>().command.append(ch);
    return {
        .tokens = {},
        .next_reduction = Reduction::CommandOrArithmeticSubstitutionExpansion,
    };
}

Lexer::ReductionResult Lexer::reduce_extended_parameter_expansion()
{
    if (m_lexer.is_eof()) {
        return {
            .tokens = { Token::continuation("${") },
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = m_lexer.peek();
    if (ch == '}') {
        m_state.buffer.append(consume());
        return {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    m_state.buffer.append(consume());
    m_state.expansions.last().get<ParameterExpansion>().parameter.append(ch);

    return {
        .tokens = {},
        .next_reduction = Reduction::ExtendedParameterExpansion,
    };
}
}
