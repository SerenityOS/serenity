/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Lexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/ScopeLogger.h>

namespace CMake::Cache {

static bool is_identifier_start_character(u32 c)
{
    return AK::is_ascii_alpha(c) || c == '_' || c == '-';
}

static bool is_identifier_character(u32 c)
{
    return AK::is_ascii_alphanumeric(c) || c == '_' || c == '-';
}

Lexer::Lexer(StringView input)
    : GenericLexer(input)
{
}

ErrorOr<Vector<Token>> Lexer::lex(StringView input)
{
    Lexer lexer { input };
    return lexer.lex_file();
}

ErrorOr<Vector<Token>> Lexer::lex_file()
{
    ScopeLogger<CMAKE_DEBUG> logger;

    while (!is_eof()) {
        skip_whitespace();

        if (is_eof())
            break;

        if (next_is('#')) {
            consume_comment();
            continue;
        }

        if (next_is("//"sv)) {
            consume_help_text();
            continue;
        }

        if (next_is(is_identifier_start_character)) {
            consume_variable_definition();
            continue;
        }

        consume_garbage();
    }

    return m_tokens;
}

void Lexer::skip_whitespace()
{
    ScopeLogger<CMAKE_DEBUG> log;

    while (!is_eof()) {
        if (next_is('\n')) {
            next_line();
            continue;
        }
        auto consumed = consume_while(AK::is_ascii_space);
        if (consumed.is_empty())
            break;
    }
}

void Lexer::consume_comment()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    VERIFY(consume_specific('#'));
    auto comment = consume_until('\n');
    emit_token(Token::Type::Comment, comment, start, position());
}

void Lexer::consume_help_text()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    VERIFY(consume_specific("//"sv));
    auto help_text = consume_until('\n');
    emit_token(Token::Type::HelpText, help_text, start, position());
}

void Lexer::consume_variable_definition()
{
    ScopeLogger<CMAKE_DEBUG> log;

    consume_key();

    if (!next_is(':')) {
        consume_garbage();
        return;
    }
    consume_colon();

    if (!next_is(is_identifier_start_character)) {
        consume_garbage();
        return;
    }
    consume_type();

    if (!next_is('=')) {
        consume_garbage();
        return;
    }
    consume_equals();

    consume_value();
}

void Lexer::consume_key()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    auto key = consume_while(is_identifier_character);
    emit_token(Token::Type::Key, key, start, position());
}

void Lexer::consume_colon()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    VERIFY(consume_specific(':'));
    emit_token(Token::Type::Colon, ":"sv, start, position());
}

void Lexer::consume_type()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    auto type = consume_while(is_identifier_character);
    emit_token(Token::Type::Type, type, start, position());
}

void Lexer::consume_equals()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    VERIFY(consume_specific('='));
    emit_token(Token::Type::Colon, "="sv, start, position());
}

void Lexer::consume_value()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    auto value = consume_until('\n');
    emit_token(Token::Type::Value, value, start, position());
}

void Lexer::consume_garbage()
{
    ScopeLogger<CMAKE_DEBUG> log;

    auto start = position();
    auto garbage = consume_until('\n');
    emit_token(Token::Type::Garbage, garbage, start, position());
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

void Lexer::emit_token(Token::Type type, StringView value, Position start, Position end)
{
    dbgln_if(CMAKE_DEBUG, "Emitting {} token: `{}` ({}:{} to {}:{})", to_string(type), value, start.line, start.column, end.line, end.column);
    m_tokens.empend(type, value, start, end);
}

}
