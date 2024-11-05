/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibShell/PosixLexer.h>

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

ErrorOr<Vector<Token>> Lexer::batch_next(Optional<Reduction> starting_reduction)
{
    if (starting_reduction.has_value())
        m_next_reduction = *starting_reduction;

    for (; m_next_reduction != Reduction::None;) {
        auto result = TRY(reduce(m_next_reduction));
        m_next_reduction = result.next_reduction;
        if (!result.tokens.is_empty())
            return result.tokens;
    }

    return Vector<Token> {};
}

ExpansionRange Lexer::range(ssize_t offset) const
{
    return {
        m_state.position.end_offset - m_state.position.start_offset + offset,
        0,
    };
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

void Lexer::reconsume(StringView string)
{
    for (auto byte : string.bytes()) {
        if (byte == '\n') {
            m_state.position.end_line.line_number++;
            m_state.position.end_line.line_column = 0;
        }

        m_state.position.end_offset++;
    }
}

bool Lexer::consume_specific(char ch)
{
    if (m_lexer.peek() == ch) {
        consume();
        return true;
    }
    return false;
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce(Reduction reduction)
{
    switch (reduction) {
    case Reduction::None:
        return ReductionResult { {}, Reduction::None };
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
    case Reduction::HeredocContents:
        return reduce_heredoc_contents();
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_end()
{
    return ReductionResult {
        .tokens = { Token::eof() },
        .next_reduction = Reduction::None,
    };
}

Lexer::HeredocKeyResult Lexer::process_heredoc_key(Token const& token)
{
    StringBuilder builder;
    enum ParseState {
        Free,
        InDoubleQuotes,
        InSingleQuotes,
    };
    Vector<ParseState, 4> parse_state;
    parse_state.append(Free);
    bool escaped = false;
    bool had_a_single_quote_segment = false;

    for (auto byte : token.value.bytes()) {
        switch (parse_state.last()) {
        case Free:
            switch (byte) {
            case '"':
                if (escaped) {
                    builder.append(byte);
                    escaped = false;
                } else {
                    parse_state.append(InDoubleQuotes);
                }
                break;
            case '\'':
                if (escaped) {
                    builder.append(byte);
                    escaped = false;
                } else {
                    had_a_single_quote_segment = true;
                    parse_state.append(InSingleQuotes);
                }
                break;
            case '\\':
                if (escaped) {
                    builder.append(byte);
                    escaped = false;
                } else {
                    escaped = true;
                }
                break;
            default:
                // NOTE: bash eats the backslash outside quotes :shrug:
                if (escaped && parse_state.last() != Free) {
                    builder.append('\\');
                    escaped = false;
                }
                builder.append(byte);
                break;
            }
            break;
        case InDoubleQuotes:
            if (!escaped && byte == '"') {
                parse_state.take_last();
                break;
            }
            if (escaped) {
                if (byte != '"')
                    builder.append('\\');
                builder.append(byte);
                break;
            }
            if (byte == '\\')
                escaped = true;
            else
                builder.append(byte);
            break;
        case InSingleQuotes:
            if (byte == '\'') {
                parse_state.take_last();
                break;
            }
            builder.append(byte);
            break;
        }
    }

    // NOTE: Not checking the final state as any garbage that even partially parses is allowed to be used as a key :/

    return {
        .key = builder.to_string().release_value_but_fixme_should_propagate_errors(),
        .allow_interpolation = !had_a_single_quote_segment,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_operator()
{
    if (m_lexer.is_eof()) {
        if (is_operator(m_state.buffer.string_view())) {
            auto tokens = TRY(Token::operators_from(m_state));
            m_state.buffer.clear();
            m_state.position.start_offset = m_state.position.end_offset;
            m_state.position.start_line = m_state.position.end_line;

            return ReductionResult {
                .tokens = move(tokens),
                .next_reduction = Reduction::End,
            };
        }

        return reduce(Reduction::Start);
    }

    if (is_part_of_operator(m_state.buffer.string_view(), m_lexer.peek())) {
        m_state.buffer.append(consume());
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Operator,
        };
    }

    auto tokens = Vector<Token> {};
    if (is_operator(m_state.buffer.string_view())) {
        tokens.extend(TRY(Token::operators_from(m_state)));
        m_state.buffer.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;
    }

    auto expect_heredoc_entry = !tokens.is_empty() && (tokens.last().type == Token::Type::DoubleLessDash || tokens.last().type == Token::Type::DoubleLess);

    auto result = TRY(reduce(Reduction::Start));
    tokens.extend(move(result.tokens));

    while (expect_heredoc_entry && tokens.size() == 1 && result.next_reduction != Reduction::None) {
        result = TRY(reduce(result.next_reduction));
        tokens.extend(move(result.tokens));
    }

    if (expect_heredoc_entry && tokens.size() > 1) {
        auto [key, interpolation] = process_heredoc_key(tokens[1]);
        m_state.heredoc_entries.append(HeredocEntry {
            .key = key,
            .allow_interpolation = interpolation,
            .dedent = tokens[0].type == Token::Type::DoubleLessDash,
        });
    }

    return ReductionResult {
        .tokens = move(tokens),
        .next_reduction = result.next_reduction,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_comment()
{
    if (m_lexer.is_eof()) {
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::End,
        };
    }

    if (consume() == '\n') {
        m_state.on_new_line = true;
        return ReductionResult {
            .tokens = { Token::newline() },
            .next_reduction = Reduction::Start,
        };
    }

    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::Comment,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_single_quoted_string()
{
    if (m_lexer.is_eof()) {
        auto tokens = TRY(Token::maybe_from_state(m_state));
        tokens.append(Token::continuation('\''));
        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::End,
        };
    }

    auto ch = consume();
    m_state.buffer.append(ch);

    if (ch == '\'') {
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::SingleQuotedString,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_double_quoted_string()
{
    m_state.previous_reduction = Reduction::DoubleQuotedString;
    if (m_lexer.is_eof()) {
        auto tokens = TRY(Token::maybe_from_state(m_state));
        tokens.append(Token::continuation('"'));
        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::End,
        };
    }

    auto ch = consume();
    m_state.buffer.append(ch);

    if (m_state.escaping) {
        m_state.escaping = false;

        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    }

    switch (ch) {
    case '\\':
        m_state.escaping = true;
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    case '"':
        m_state.previous_reduction = Reduction::Start;
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    case '$':
        if (m_lexer.next_is("("))
            m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .range = range(-1) });
        else
            m_state.expansions.empend(ParameterExpansion { .parameter = StringBuilder {}, .range = range(-1) });
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Expansion,
        };
    case '`':
        m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .range = range(-1) });
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    default:
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    }
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_expansion()
{
    if (m_lexer.is_eof())
        return reduce(m_state.previous_reduction);

    auto ch = m_lexer.peek();

    switch (ch) {
    case '{':
        consume();
        m_state.buffer.append(ch);
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::ExtendedParameterExpansion,
        };
    case '(':
        consume();
        m_state.buffer.append(ch);
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::CommandOrArithmeticSubstitutionExpansion,
        };
    case 'a' ... 'z':
    case 'A' ... 'Z':
    case '_': {
        consume();
        m_state.buffer.append(ch);
        auto& expansion = m_state.expansions.last().get<ParameterExpansion>();
        expansion.parameter.append(ch);
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::ParameterExpansion,
        };
    }
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

ErrorOr<Lexer::ReductionResult> Lexer::reduce_command_expansion()
{
    if (m_lexer.is_eof()) {
        auto& expansion = m_state.expansions.last().get<CommandExpansion>();
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = { Token::continuation('`') },
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = consume();

    if (!m_state.escaping && ch == '`') {
        m_state.buffer.append(ch);
        auto& expansion = m_state.expansions.last().get<CommandExpansion>();
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    if (!m_state.escaping && ch == '\\') {
        m_state.escaping = true;
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    }

    m_state.escaping = false;
    m_state.buffer.append(ch);
    m_state.expansions.last().get<CommandExpansion>().command.append(ch);
    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::CommandExpansion,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_heredoc_contents()
{
    if (m_lexer.is_eof()) {
        auto tokens = TRY(Token::maybe_from_state(m_state));
        m_state.buffer.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::End,
        };
    }

    if (!m_state.escaping && consume_specific('\\')) {
        m_state.escaping = true;
        m_state.buffer.append('\\');
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::HeredocContents,
        };
    }

    if (!m_state.escaping && consume_specific('$')) {
        m_state.buffer.append('$');
        if (m_lexer.next_is("("))
            m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .range = range(-1) });
        else
            m_state.expansions.empend(ParameterExpansion { .parameter = StringBuilder {}, .range = range(-1) });

        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Expansion,
        };
    }

    if (!m_state.escaping && consume_specific('`')) {
        m_state.buffer.append('`');
        m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .range = range(-1) });
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    }

    m_state.escaping = false;
    m_state.buffer.append(consume());
    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::HeredocContents,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_start()
{
    auto was_on_new_line = m_state.on_new_line;
    m_state.on_new_line = false;

    if (m_lexer.is_eof()) {
        auto tokens = TRY(Token::maybe_from_state(m_state));
        m_state.buffer.clear();
        m_state.expansions.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::End,
        };
    }

    if (was_on_new_line && !m_state.heredoc_entries.is_empty()) {
        auto const& entry = m_state.heredoc_entries.first();

        auto start_index = m_lexer.tell();
        Optional<size_t> end_index;

        for (; !m_lexer.is_eof();) {
            auto index = m_lexer.tell();
            auto possible_end_index = m_lexer.tell();
            if (m_lexer.consume_specific('\n')) {
                if (entry.dedent)
                    m_lexer.ignore_while(is_any_of("\t"sv));
                if (m_lexer.consume_specific(entry.key.bytes_as_string_view())) {
                    if (m_lexer.consume_specific('\n') || m_lexer.is_eof()) {
                        end_index = possible_end_index;
                        break;
                    }
                }
            }
            if (m_lexer.tell() == index)
                m_lexer.ignore();
        }

        auto contents = m_lexer.input().substring_view(start_index, end_index.value_or(m_lexer.tell()) - start_index);
        reconsume(contents);
        if (end_index.has_value())
            reconsume(m_lexer.input().substring_view_starting_after_substring(contents).substring_view(0, m_lexer.tell() - *end_index));

        m_state.buffer.clear();
        m_state.buffer.append(contents);

        auto token = TRY(Token::maybe_from_state(m_state)).first();
        token.relevant_heredoc_key = entry.key;
        token.type = Token::Type::HeredocContents;

        m_state.heredoc_entries.take_first();

        m_state.on_new_line = true;

        m_state.buffer.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        Vector<Token> tokens { move(token), Token::newline() };

        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::Start,
        };
    }

    if (m_state.escaping && consume_specific('\n')) {
        m_state.escaping = false;

        auto buffer = m_state.buffer.to_byte_string().substring(0, m_state.buffer.length() - 1);
        m_state.buffer.clear();
        m_state.buffer.append(buffer);

        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && m_lexer.peek() == '#' && m_state.buffer.is_empty()) {
        consume();
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Comment,
        };
    }

    if (!m_state.escaping && consume_specific('\n')) {
        auto tokens = TRY(Token::maybe_from_state(m_state));
        tokens.append(Token::newline());

        m_state.on_new_line = true;

        m_state.buffer.clear();
        m_state.expansions.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && consume_specific('\\')) {
        m_state.escaping = true;
        m_state.buffer.append('\\');
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && consume_specific('\'')) {
        m_state.buffer.append('\'');
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::SingleQuotedString,
        };
    }

    if (!m_state.escaping && consume_specific('"')) {
        m_state.buffer.append('"');
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::DoubleQuotedString,
        };
    }

    if (!m_state.escaping && is_ascii_space(m_lexer.peek())) {
        consume();
        auto tokens = TRY(Token::maybe_from_state(m_state));
        m_state.buffer.clear();
        m_state.expansions.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::Start,
        };
    }

    if (!m_state.escaping && consume_specific('$')) {
        m_state.buffer.append('$');
        if (m_lexer.next_is("("))
            m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .range = range(-1) });
        else
            m_state.expansions.empend(ParameterExpansion { .parameter = StringBuilder {}, .range = range(-1) });

        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Expansion,
        };
    }

    if (!m_state.escaping && consume_specific('`')) {
        m_state.buffer.append('`');
        m_state.expansions.empend(CommandExpansion { .command = StringBuilder {}, .range = range(-1) });
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::CommandExpansion,
        };
    }

    if (!m_state.escaping && m_state.in_skip_mode && is_any_of("})"sv)(m_lexer.peek())) {
        // That's an eof for us.
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::None,
        };
    }

    if (!m_state.escaping && is_part_of_operator(""sv, m_lexer.peek())) {
        auto tokens = TRY(Token::maybe_from_state(m_state));
        m_state.buffer.clear();
        m_state.buffer.append(consume());
        m_state.expansions.clear();
        m_state.position.start_offset = m_state.position.end_offset;
        m_state.position.start_line = m_state.position.end_line;

        return ReductionResult {
            .tokens = move(tokens),
            .next_reduction = Reduction::Operator,
        };
    }

    m_state.escaping = false;
    m_state.buffer.append(consume());
    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::Start,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_arithmetic_expansion()
{
    if (m_lexer.is_eof()) {
        auto& expansion = m_state.expansions.last().get<ArithmeticExpansion>();
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = { Token::continuation("$(("_string) },
            .next_reduction = m_state.previous_reduction,
        };
    }

    if (m_lexer.peek() == ')' && m_state.buffer.string_view().ends_with(')')) {
        m_state.buffer.append(consume());
        auto& expansion = m_state.expansions.last().get<ArithmeticExpansion>();
        expansion.expression = TRY(String::from_utf8(expansion.value.string_view().substring_view(0, expansion.value.length() - 1)));
        expansion.value.clear();
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = consume();
    m_state.buffer.append(ch);
    m_state.expansions.last().get<ArithmeticExpansion>().value.append(ch);
    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::ArithmeticExpansion,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_special_parameter_expansion()
{
    auto ch = consume();
    m_state.buffer.append(ch);
    m_state.expansions.last() = ParameterExpansion {
        .parameter = StringBuilder {},
        .range = range(-2),
    };
    auto& expansion = m_state.expansions.last().get<ParameterExpansion>();
    expansion.parameter.append(ch);
    expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

    return ReductionResult {
        .tokens = {},
        .next_reduction = m_state.previous_reduction,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_parameter_expansion()
{
    auto& expansion = m_state.expansions.last().get<ParameterExpansion>();

    if (m_lexer.is_eof()) {
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::Start,
        };
    }

    auto next = m_lexer.peek();
    if (is_ascii_alphanumeric(next) || next == '_') {
        m_state.buffer.append(consume());
        expansion.parameter.append(next);
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::ParameterExpansion,
        };
    }

    return reduce(m_state.previous_reduction);
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_command_or_arithmetic_substitution_expansion()
{
    auto ch = m_lexer.peek();
    if (ch == '(' && m_state.buffer.string_view().ends_with("$("sv)) {
        m_state.buffer.append(consume());
        m_state.expansions.last() = ArithmeticExpansion {
            .expression = {},
            .value = StringBuilder {},
            .range = range(-2)
        };
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::ArithmeticExpansion,
        };
    }

    auto saved_position = m_state.position;
    {
        auto skip_mode = switch_to_skip_mode();

        auto next_reduction = Reduction::Start;
        do {
            auto result = TRY(reduce(next_reduction));
            next_reduction = result.next_reduction;
        } while (next_reduction != Reduction::None);
        saved_position = m_state.position;
    }

    auto const skipped_text = m_lexer.input().substring_view(m_state.position.end_offset, saved_position.end_offset - m_state.position.end_offset);
    m_state.position.end_offset = saved_position.end_offset;
    m_state.position.end_line = saved_position.end_line;

    m_state.buffer.append(skipped_text);
    m_state.expansions.last().get<CommandExpansion>().command.append(skipped_text);
    m_state.expansions.last().visit([&](auto& expansion) {
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;
    });

    if (m_lexer.is_eof()) {
        return ReductionResult {
            .tokens = { Token::continuation("$("_string) },
            .next_reduction = m_state.previous_reduction,
        };
    }

    ch = m_lexer.peek();
    if (ch == '(' && m_state.buffer.string_view().ends_with("$("sv)) {
        m_state.buffer.append(consume());
        m_state.expansions.last() = ArithmeticExpansion {
            .expression = {},
            .value = m_state.expansions.last().get<CommandExpansion>().command,
            .range = range(-2)
        };
        return ReductionResult {
            .tokens = {},
            .next_reduction = Reduction::ArithmeticExpansion,
        };
    }

    if (ch == ')') {
        m_state.buffer.append(consume());
        m_state.expansions.last().visit([&](auto& expansion) {
            expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;
        });
        return ReductionResult {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    m_state.buffer.append(consume());
    m_state.expansions.last().get<CommandExpansion>().command.append(ch);
    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::CommandOrArithmeticSubstitutionExpansion,
    };
}

ErrorOr<Lexer::ReductionResult> Lexer::reduce_extended_parameter_expansion()
{
    auto& expansion = m_state.expansions.last().get<ParameterExpansion>();

    if (m_lexer.is_eof()) {
        return ReductionResult {
            .tokens = { Token::continuation("${"_string) },
            .next_reduction = m_state.previous_reduction,
        };
    }

    auto ch = m_lexer.peek();
    if (ch == '}') {
        m_state.buffer.append(consume());
        expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

        return ReductionResult {
            .tokens = {},
            .next_reduction = m_state.previous_reduction,
        };
    }

    m_state.buffer.append(consume());
    expansion.parameter.append(ch);
    expansion.range.length = m_state.position.end_offset - expansion.range.start - m_state.position.start_offset;

    return ReductionResult {
        .tokens = {},
        .next_reduction = Reduction::ExtendedParameterExpansion,
    };
}

StringView Token::type_name() const
{
    switch (type) {
    case Type::Eof:
        return "Eof"sv;
    case Type::Newline:
        return "Newline"sv;
    case Type::Continuation:
        return "Continuation"sv;
    case Type::Token:
        return "Token"sv;
    case Type::And:
        return "And"sv;
    case Type::Pipe:
        return "Pipe"sv;
    case Type::OpenParen:
        return "OpenParen"sv;
    case Type::CloseParen:
        return "CloseParen"sv;
    case Type::Great:
        return "Great"sv;
    case Type::Less:
        return "Less"sv;
    case Type::AndIf:
        return "AndIf"sv;
    case Type::OrIf:
        return "OrIf"sv;
    case Type::DoubleSemicolon:
        return "DoubleSemicolon"sv;
    case Type::DoubleLess:
        return "DoubleLess"sv;
    case Type::DoubleGreat:
        return "DoubleGreat"sv;
    case Type::LessAnd:
        return "LessAnd"sv;
    case Type::GreatAnd:
        return "GreatAnd"sv;
    case Type::LessGreat:
        return "LessGreat"sv;
    case Type::DoubleLessDash:
        return "DoubleLessDash"sv;
    case Type::Clobber:
        return "Clobber"sv;
    case Type::Semicolon:
        return "Semicolon"sv;
    case Type::HeredocContents:
        return "HeredocContents"sv;
    case Type::AssignmentWord:
        return "AssignmentWord"sv;
    case Type::ListAssignmentWord:
        return "ListAssignmentWord"sv;
    case Type::Bang:
        return "Bang"sv;
    case Type::Case:
        return "Case"sv;
    case Type::CloseBrace:
        return "CloseBrace"sv;
    case Type::Do:
        return "Do"sv;
    case Type::Done:
        return "Done"sv;
    case Type::Elif:
        return "Elif"sv;
    case Type::Else:
        return "Else"sv;
    case Type::Esac:
        return "Esac"sv;
    case Type::Fi:
        return "Fi"sv;
    case Type::For:
        return "For"sv;
    case Type::If:
        return "If"sv;
    case Type::In:
        return "In"sv;
    case Type::IoNumber:
        return "IoNumber"sv;
    case Type::OpenBrace:
        return "OpenBrace"sv;
    case Type::Then:
        return "Then"sv;
    case Type::Until:
        return "Until"sv;
    case Type::VariableName:
        return "VariableName"sv;
    case Type::While:
        return "While"sv;
    case Type::Word:
        return "Word"sv;
    }
    return "Idk"sv;
}

}
