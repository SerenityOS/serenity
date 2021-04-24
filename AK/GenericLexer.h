/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace AK {

class GenericLexer {
public:
    struct Position {
        size_t line { 1 };
        size_t column { 1 };
    };

    constexpr explicit GenericLexer(const StringView& input)
        : m_input(input)
    {
    }

    constexpr size_t tell() const { return m_index; }
    constexpr size_t tell_remaining() const { return m_input.length() - m_index; }
    constexpr const Position& position() const { return m_position; }

    StringView remaining() const { return m_input.substring_view(m_index); }

    // Tells whether the parser's index has reached input's end
    constexpr bool is_eof() const { return m_index >= m_input.length(); }

    // Returns the current character at the parser index, plus `offset` if specified
    constexpr char peek(size_t offset = 0) const
    {
        return (m_index + offset < m_input.length()) ? m_input[m_index + offset] : '\0';
    }

    // Tests the next character in the input
    constexpr bool next_is(char expected) const
    {
        return peek() == expected;
    }

    // Tests if the `expected` StringView comes next in the input
    constexpr bool next_is(StringView expected) const
    {
        for (size_t i = 0; i < expected.length(); ++i)
            if (peek(i) != expected[i])
                return false;
        return true;
    }

    // Tests if the `expected` c-string comes next in the input
    constexpr bool next_is(const char* expected) const
    {
        for (size_t i = 0; expected[i] != '\0'; ++i)
            if (peek(i) != expected[i])
                return false;
        return true;
    }

    // Go back to the previous character
    constexpr void retreat()
    {
        VERIFY(tell() > 0);
        advance(1, false);
    }

    // Consume a character and advance the parser index
    constexpr char consume()
    {
        VERIFY(!is_eof());
        auto consumed = peek();
        advance(1);
        return consumed;
    }

    // Consume the given character if it is next in the input
    template<typename T>
    constexpr bool consume_specific(const T& next)
    {
        if (!next_is(next))
            return false;

        if constexpr (requires { next.length(); }) {
            ignore(next.length());
        } else {
            ignore(sizeof(next));
        }
        return true;
    }

    // Consume the given String if it is next in the input
    bool consume_specific(const String& next)
    {
        return consume_specific(StringView { next });
    }

    // Consume the given c-string if it is next in the input
    constexpr bool consume_specific(const char* next)
    {
        return consume_specific(StringView { next });
    }

    constexpr char consume_escaped_character(char escape_char = '\\', const StringView& escape_map = "n\nr\rt\tb\bf\f")
    {
        if (!consume_specific(escape_char))
            return consume();

        auto c = consume();

        for (size_t i = 0; i < escape_map.length(); i += 2) {
            if (c == escape_map[i])
                return escape_map[i + 1];
        }

        return c;
    }

    StringView consume(size_t count);
    StringView consume_all();
    StringView consume_line();
    StringView consume_until(char);
    StringView consume_until(const char*);
    StringView consume_quoted_string(char escape_char = 0);
    String consume_and_unescape_string(char escape_char = '\\');

    // Ignore a number of characters (1 by default)
    constexpr void ignore(size_t count = 1)
    {
        count = min(count, tell_remaining());
        advance(count);
    }

    // Ignore characters until `stop` is peek'd
    // The `stop` character is ignored as it is user-defined
    constexpr void ignore_until(char stop)
    {
        while (!is_eof() && peek() != stop) {
            advance(1);
        }
        ignore();
    }

    // Ignore characters until the c-string `stop` is found
    // The `stop` string is ignored, as it is user-defined
    constexpr void ignore_until(const char* stop)
    {
        while (!is_eof() && !next_is(stop)) {
            advance(1);
        }
        ignore(__builtin_strlen(stop));
    }

    // Ignore a whole line
    constexpr void ignore_line()
    {
        ignore_until('\n');
    }

    /*
     * Predicates are used to match arbitrary characters. You can use lambdas,
     * ctype functions, or is_any_of() and its derivatives (see below).
     * A few examples:
     *   - `if (lexer.next_is(isdigit))`
     *   - `auto name = lexer.consume_while([](char c) { return isalnum(c) || c == '_'; });`
     *   - `lexer.ignore_until(is_any_of("<^>"));`
     */

    // Test the next character against a Condition
    template<typename TPredicate>
    constexpr bool next_is(TPredicate pred) const
    {
        return pred(peek());
    }

    // Consume and return characters while `pred` returns true
    template<typename TPredicate>
    StringView consume_while(TPredicate pred)
    {
        size_t start = tell();
        while (!is_eof() && pred(peek()))
            advance(1);
        size_t length = tell() - start;

        if (length == 0)
            return {};
        return m_input.substring_view(start, length);
    }

    // Consume and return characters until `pred` return true
    template<typename TPredicate>
    StringView consume_until(TPredicate pred)
    {
        size_t start = tell();
        while (!is_eof() && !pred(peek()))
            advance(1);
        size_t length = tell() - start;

        if (length == 0)
            return {};
        return m_input.substring_view(start, length);
    }

    // Ignore characters while `pred` returns true
    template<typename TPredicate>
    constexpr void ignore_while(TPredicate pred)
    {
        while (!is_eof() && pred(peek()))
            advance(1);
    }

    // Ignore characters until `pred` return true
    // We don't skip the stop character as it may not be a unique value
    template<typename TPredicate>
    constexpr void ignore_until(TPredicate pred)
    {
        while (!is_eof() && !pred(peek()))
            advance(1);
    }

private:
    // Go forward or backwards in the source string while keeping track of the
    // line and column number
    constexpr void advance(size_t count, bool forward = true)
    {
        if (count == 0)
            return;

        if (forward)
            VERIFY(count <= tell_remaining());
        else
            VERIFY(count <= m_index);

        while (count--) {
            if (forward) {
                if (m_input[m_index++] == '\n') {
                    m_position.line++;
                    m_position.column = 1;
                } else {
                    m_position.column++;
                }
            } else {
                if (m_input[--m_index] == '\n') {
                    m_position.line--;
                    m_position.column = 1;
                    // Iterate backwards to the beginning of the line
                    for (size_t i = 0; i < m_index && m_input[m_index - i - 1] != '\n'; ++i)
                        m_position.column++;
                } else {
                    m_position.column--;
                }
            }
        }
    }

protected:
    StringView m_input;
    size_t m_index { 0 };
    Position m_position;
};

constexpr auto is_any_of(const StringView& values)
{
    return [values](auto c) { return values.contains(c); };
}

constexpr auto is_path_separator = is_any_of("/\\");
constexpr auto is_quote = is_any_of("'\"");

}

using AK::GenericLexer;
using AK::is_any_of;
using AK::is_path_separator;
using AK::is_quote;
