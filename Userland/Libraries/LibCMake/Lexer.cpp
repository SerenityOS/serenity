/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Lexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/ScopeLogger.h>

namespace CMake {

static bool is_valid_identifier_initial_char(char c)
{
    return is_ascii_alpha(c) || c == '_';
}

static bool is_valid_identifier_char(char c)
{
    return is_ascii_alphanumeric(c) || c == '_';
}

ErrorOr<Vector<Token>> Lexer::lex(StringView input)
{
    Lexer lexer { input };
    return lexer.lex_file();
}

Lexer::Lexer(StringView input)
    : GenericLexer(input)
{
}

ErrorOr<Vector<Token>> Lexer::lex_file()
{
    m_tokens.clear_with_capacity();

    while (!is_eof()) {
        consume_whitespace_or_comments();

        if (is_eof())
            break;

        if (is_valid_identifier_initial_char(peek())) {
            consume_command_invocation();
        } else {
            consume_garbage();
        }
    }

    return m_tokens;
}

void Lexer::skip_whitespace()
{
    while (!is_eof()) {
        if (next_is('\n')) {
            next_line();
            continue;
        }
        auto consumed = consume_while([&](char c) {
            return c == ' ' || c == '\t';
        });
        if (consumed.is_empty())
            break;
    }
}

void Lexer::consume_whitespace_or_comments()
{
    ScopeLogger<CMAKE_DEBUG> log;
    while (!is_eof()) {
        skip_whitespace();

        if (next_is('#')) {
            consume_comment();
        } else {
            break;
        }
    }
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#command-invocations
void Lexer::consume_command_invocation()
{
    ScopeLogger<CMAKE_DEBUG> log;
    auto identifier_start = position();
    auto identifier = consume_while(is_valid_identifier_char);
    auto control_keyword = control_keyword_from_string(identifier);
    if (control_keyword.has_value()) {
        emit_token(Token::Type::ControlKeyword, identifier, identifier_start, position(), control_keyword.release_value());
    } else {
        emit_token(Token::Type::Identifier, identifier, identifier_start, position());
    }

    consume_whitespace_or_comments();

    if (next_is('('))
        consume_open_paren();

    consume_arguments();

    if (next_is(')'))
        consume_close_paren();
}

void Lexer::consume_arguments()
{
    ScopeLogger<CMAKE_DEBUG> log;
    while (!is_eof()) {
        consume_whitespace_or_comments();

        if (next_is('(')) {
            consume_open_paren();

            consume_whitespace_or_comments();
            consume_arguments();
            consume_whitespace_or_comments();

            if (next_is(')'))
                consume_close_paren();

            continue;
        }

        if (next_is(')'))
            return;

        consume_argument();
    }
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#command-arguments
void Lexer::consume_argument()
{
    ScopeLogger<CMAKE_DEBUG> log;
    consume_whitespace_or_comments();

    if (next_is('[')) {
        consume_bracket_argument();
        return;
    }

    if (next_is('"')) {
        consume_quoted_argument();
        return;
    }

    consume_unquoted_argument();
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#bracket-argument
void Lexer::consume_bracket_argument()
{
    ScopeLogger<CMAKE_DEBUG> log;
    auto start = position();
    auto value = read_bracket_argument();
    emit_token(Token::Type::BracketArgument, value, start, position());
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#quoted-argument
void Lexer::consume_quoted_argument()
{
    ScopeLogger<CMAKE_DEBUG> log;
    auto start = position();
    auto start_offset = tell();

    VERIFY(consume_specific('"'));
    while (!is_eof()) {
        if (next_is('"')) {
            ignore();
            break;
        }

        if (next_is("\\\""sv)) {
            ignore(2);
            continue;
        }

        if (next_is('\n')) {
            next_line();
            continue;
        }

        ignore();
    }

    auto whole_token = m_input.substring_view(start_offset, tell() - start_offset);
    auto value = whole_token.substring_view(1, whole_token.length() - 2);
    auto variable_references = parse_variable_references_from_argument(whole_token, start);
    emit_token(Token::Type::QuotedArgument, value, start, position(), {}, move(variable_references));
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#unquoted-argument
void Lexer::consume_unquoted_argument()
{
    ScopeLogger<CMAKE_DEBUG> log;
    auto start_offset = tell();
    auto start = position();

    while (!is_eof()) {
        if (next_is('\\')) {
            consume_escaped_character('\\');
            continue;
        }

        auto consumed = consume_until([](char c) { return is_ascii_space(c) || "()#\"\\'"sv.contains(c); });
        if (consumed.is_empty())
            break;

        // FIXME: `unquoted_legacy`
    }

    auto value = m_input.substring_view(start_offset, tell() - start_offset);
    auto variable_references = parse_variable_references_from_argument(value, start);
    emit_token(Token::Type::UnquotedArgument, value, start, position(), {}, move(variable_references));
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#comments
void Lexer::consume_comment()
{
    ScopeLogger<CMAKE_DEBUG> log;
    auto start = position();

    VERIFY(consume_specific('#'));
    if (next_is('[')) {
        // Bracket comment
        // https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#bracket-comment
        auto comment = read_bracket_argument();
        emit_token(Token::Type::BracketComment, comment, start, position());
        return;
    }

    // Line comment
    // https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#line-comment
    auto comment = consume_until('\n');
    emit_token(Token::Type::LineComment, comment, start, position());
}

void Lexer::consume_open_paren()
{
    auto start = position();
    VERIFY(consume_specific('('));
    emit_token(Token::Type::OpenParen, "("sv, start, position());
}

void Lexer::consume_close_paren()
{
    auto start = position();
    VERIFY(consume_specific(')'));
    emit_token(Token::Type::CloseParen, ")"sv, start, position());
}

void Lexer::consume_garbage()
{
    ScopeLogger<CMAKE_DEBUG> log;
    auto start = position();
    auto contents = consume_until(is_ascii_space);
    if (!contents.is_empty())
        emit_token(Token::Type::Garbage, contents, start, position());
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#bracket-argument
// Used by both bracket arguments and bracket comments.
StringView Lexer::read_bracket_argument()
{
    VERIFY(consume_specific('['));
    auto leading_equals_signs = consume_while([](char c) { return c == '='; });
    consume_specific('[');
    auto start = tell();
    auto end = start;
    while (!is_eof()) {
        // Read everything until we see `]={len}]`.
        ignore_until(']');
        end = tell();
        ignore();
        if (next_is(leading_equals_signs))
            ignore(leading_equals_signs.length());
        if (consume_specific(']'))
            break;
    }

    return m_input.substring_view(start, end - start);
}

// https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#variable-references
Vector<VariableReference> Lexer::parse_variable_references_from_argument(StringView argument_value, Position argument_start)
{
    auto position = argument_start;
    GenericLexer lexer { argument_value };
    Vector<VariableReference> variable_references;

    while (!lexer.is_eof()) {
        if (lexer.next_is('\n')) {
            lexer.ignore();
            position.column = 0;
            position.line++;
            continue;
        }

        if (lexer.next_is('\\')) {
            lexer.ignore();
            if (lexer.next_is('\n')) {
                lexer.ignore();
                position.column = 0;
                position.line++;
                continue;
            }
            lexer.ignore();
            position.column += 2;
        }

        if (lexer.next_is('$')) {
            auto start = position;
            lexer.ignore();
            position.column++;

            if (lexer.next_is("ENV{"sv)) {
                lexer.ignore(4);
                position.column += 4;
            } else if (lexer.next_is('{')) {
                lexer.ignore();
                position.column++;
            } else {
                auto skipped = lexer.consume_until(is_any_of("$ \n"sv));
                position.column += skipped.length();
                continue;
            }

            auto variable_name = lexer.consume_until(is_any_of("} \n"sv));
            position.column += variable_name.length();
            if (lexer.next_is('}')) {
                lexer.ignore();
                position.column++;
                variable_references.empend(variable_name, start, position);
            }

            continue;
        }

        lexer.ignore();
        position.column++;
    }

    return variable_references;
}

Position Lexer::position() const
{
    return Position {
        .line = m_line,
        .column = tell() - m_string_offset_after_previous_newline,
    };
}

void Lexer::next_line()
{
    VERIFY(consume_specific('\n'));
    m_string_offset_after_previous_newline = tell();
    m_line++;
}

void Lexer::emit_token(Token::Type type, StringView value, Position start, Position end, Optional<ControlKeywordType> control_keyword, Vector<VariableReference> variable_references)
{
    dbgln_if(CMAKE_DEBUG, "Emitting {} token: `{}` ({}:{} to {}:{})", to_string(type), value, start.line, start.column, end.line, end.column);
    m_tokens.empend(type, value, start, end, move(control_keyword), move(variable_references));
}

}
