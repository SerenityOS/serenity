/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
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

#include <AK/Assertions.h>
#include <AK/GenericLexer.h>

namespace AK {

GenericLexer::GenericLexer(const StringView& input)
    : m_input(input)
{
}

GenericLexer::~GenericLexer()
{
}

// Tells whether the parser's index has reached input's end
bool GenericLexer::is_eof() const
{
    return m_index >= m_input.length();
}

// Returns the current character at the parser index, plus `offset` if specified
char GenericLexer::peek(size_t offset) const
{
    return (m_index + offset < m_input.length()) ? m_input[m_index + offset] : '\0';
}

// Tests the next character in the input
bool GenericLexer::next_is(char expected) const
{
    return peek() == expected;
}

// Tests if the `expected` string comes next in the input
bool GenericLexer::next_is(StringView expected) const
{
    for (size_t i = 0; i < expected.length(); ++i)
        if (peek(i) != expected[i])
            return false;
    return true;
}

// Tests if the `expected` string comes next in the input
bool GenericLexer::next_is(const char* expected) const
{
    for (size_t i = 0; expected[i] != '\0'; ++i)
        if (peek(i) != expected[i])
            return false;
    return true;
}

// Tests the next character against a Condition
bool GenericLexer::next_is(Condition condition) const
{
    return condition(peek());
}

// Consume a character and advance the parser index
char GenericLexer::consume()
{
    ASSERT(!is_eof());
    return m_input[m_index++];
}

// Consume the given character if it is next in the input
bool GenericLexer::consume_specific(char specific)
{
    if (peek() != specific)
        return false;

    ignore();
    return true;
}

// Consume the given string if it is next in the input
bool GenericLexer::consume_specific(StringView str)
{
    if (!next_is(str))
        return false;

    ignore(str.length());
    return true;
}

// Consume the given string if it is next in the input
bool GenericLexer::consume_specific(const char* str)
{
    return consume_specific(StringView(str));
}

// Consume a number of characters
StringView GenericLexer::consume(size_t count)
{
    if (count == 0)
        return {};

    size_t start = m_index;
    size_t length = min(count, m_input.length() - m_index);
    m_index += length;

    return m_input.substring_view(start, length);
}

// Consume the rest of the input
StringView GenericLexer::consume_all()
{
    if (is_eof())
        return {};

    auto rest = m_input.substring_view(m_index, m_input.length() - m_index);
    m_index = m_input.length();
    return rest;
}

// Consume until a new line is found
StringView GenericLexer::consume_line()
{
    size_t start = m_index;
    while (!is_eof() && peek() != '\r' && peek() != '\n')
        m_index++;
    size_t length = m_index - start;

    consume_specific('\r');
    consume_specific('\n');

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters while `condition` returns true
StringView GenericLexer::consume_while(Condition condition)
{
    size_t start = m_index;
    while (!is_eof() && condition(peek()))
        m_index++;
    size_t length = m_index - start;

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until `stop` is peek'd
// The `stop` character is ignored, as it is user-defined
StringView GenericLexer::consume_until(char stop)
{
    size_t start = m_index;
    while (!is_eof() && peek() != stop)
        m_index++;
    size_t length = m_index - start;

    ignore();

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until the string `stop` is found
// The `stop` string is ignored, as it is user-defined
StringView GenericLexer::consume_until(const char* stop)
{
    size_t start = m_index;
    while (!is_eof() && !next_is(stop))
        m_index++;
    size_t length = m_index - start;

    ignore(__builtin_strlen(stop));

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until `condition` return true
StringView GenericLexer::consume_until(Condition condition)
{
    size_t start = m_index;
    while (!is_eof() && !condition(peek()))
        m_index++;
    size_t length = m_index - start;

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume a string surrounded by single or double quotes
// The returned StringView does not include the quotes
StringView GenericLexer::consume_quoted_string()
{
    if (!is_quote(peek()))
        return {};

    char quote_char = consume();
    size_t start = m_index;
    while (!is_eof() && peek() != quote_char)
        m_index++;
    size_t length = m_index - start;

    if (peek() != quote_char) {
        m_index = start - 1; // Restore the index in case the string is unterminated
        return {};
    }

    ignore();

    return m_input.substring_view(start, length);
}

// Ignore a number of characters (1 by default)
void GenericLexer::ignore(size_t count)
{
    count = min(count, m_input.length() - m_index);
    m_index += count;
}

// Ignore characters while `condition` returns true
void GenericLexer::ignore_while(Condition condition)
{
    while (!is_eof() && condition(peek()))
        m_index++;
}

// Ignore characters until `stop` is peek'd
// The `stop` character is ignored as it is user-defined
void GenericLexer::ignore_until(char stop)
{
    while (!is_eof() && peek() != stop)
        m_index++;

    ignore();
}

// Ignore characters until the string `stop` is found
// The `stop` string is ignored, as it is user-defined
void GenericLexer::ignore_until(const char* stop)
{
    while (!is_eof() && !next_is(stop))
        m_index++;

    ignore(__builtin_strlen(stop));
}

// Ignore characters until `condition` return true
// We don't skip the stop character as it may not be a single value
void GenericLexer::ignore_until(Condition condition)
{
    while (!is_eof() && !condition(peek()))
        m_index++;
}

bool is_control(char c)
{
    return (c >= 0 && c <= 31) || c == 127;
}

bool is_whitespace(char c)
{
    return (c >= '\t' && c <= '\r') || c == ' ';
}

bool is_lowercase(char c)
{
    return c >= 'a' && c <= 'z';
}

bool is_uppercase(char c)
{
    return c >= 'A' && c <= 'Z';
}

bool is_digit(char c)
{
    return c >= '0' && c <= '9';
}

bool is_punctuation(char c)
{
    return (c >= '!' && c <= '/')
        || (c >= ':' && c <= '@')
        || (c >= '[' && c <= '`')
        || (c >= '{' && c <= '~');
}

bool is_printable(char c)
{
    return c >= ' ' && c <= '~';
}

bool is_graphic(char c)
{
    return c > ' ' && c <= '~';
}

bool is_alpha(char c)
{
    return is_lowercase(c) || is_uppercase(c);
}

bool is_alphanum(char c)
{
    return is_alpha(c) || is_digit(c);
}

bool is_hex_digit(char c)
{
    return is_digit(c)
        || (c >= 'A' && c <= 'F')
        || (c >= 'a' && c <= 'f');
}

bool is_quote(char c)
{
    return c == '\'' || c == '"';
}

bool is_path_separator(char c)
{
    return c == '/' || c == '\\';
}

}
