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
    explicit GenericLexer(const StringView& input);
    virtual ~GenericLexer();

    size_t tell() const { return m_index; }
    size_t tell_remaining() const { return m_input.length() - m_index; }

    StringView remaining() const { return m_input.substring_view(m_index); }

    bool is_eof() const;

    char peek(size_t offset = 0) const;

    bool next_is(char) const;
    bool next_is(StringView) const;
    bool next_is(const char*) const;

    void retreat();

    char consume();
    bool consume_specific(char);
    bool consume_specific(StringView);
    bool consume_specific(const char*);
    char consume_escaped_character(char escape_char = '\\', const StringView& escape_map = "n\nr\rt\tb\bf\f");
    StringView consume(size_t count);
    StringView consume_all();
    StringView consume_line();
    StringView consume_until(char);
    StringView consume_until(const char*);
    StringView consume_quoted_string(char escape_char = 0);
    String consume_and_unescape_string(char escape_char = '\\');

    void ignore(size_t count = 1);
    void ignore_until(char);
    void ignore_until(const char*);

    /*
     * Conditions are used to match arbitrary characters. You can use lambdas,
     * ctype functions, or is_any_of() and its derivatives (see below).
     * A few examples:
     *   - `if (lexer.next_is(isdigit))`
     *   - `auto name = lexer.consume_while([](char c) { return isalnum(c) || c == '_'; });`
     *   - `lexer.ignore_until(is_any_of("<^>"));`
     */

    // Test the next character against a Condition
    template<typename C>
    bool next_is(C condition) const
    {
        return condition(peek());
    }

    // Consume and return characters while `condition` returns true
    template<typename C>
    StringView consume_while(C condition)
    {
        size_t start = m_index;
        while (!is_eof() && condition(peek()))
            m_index++;
        size_t length = m_index - start;

        if (length == 0)
            return {};
        return m_input.substring_view(start, length);
    }

    // Consume and return characters until `condition` return true
    template<typename C>
    StringView consume_until(C condition)
    {
        size_t start = m_index;
        while (!is_eof() && !condition(peek()))
            m_index++;
        size_t length = m_index - start;

        if (length == 0)
            return {};
        return m_input.substring_view(start, length);
    }

    // Ignore characters while `condition` returns true
    template<typename C>
    void ignore_while(C condition)
    {
        while (!is_eof() && condition(peek()))
            m_index++;
    }

    // Ignore characters until `condition` return true
    // We don't skip the stop character as it may not be a unique value
    template<typename C>
    void ignore_until(C condition)
    {
        while (!is_eof() && !condition(peek()))
            m_index++;
    }

protected:
    StringView m_input;
    size_t m_index { 0 };
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
