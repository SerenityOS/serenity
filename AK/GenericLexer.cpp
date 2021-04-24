/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/GenericLexer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace AK {

// Consume a number of characters
StringView GenericLexer::consume(size_t count)
{
    if (count == 0)
        return {};

    size_t start = tell();
    size_t length = min(count, tell_remaining());
    advance(length);

    return m_input.substring_view(start, length);
}

// Consume the rest of the input
StringView GenericLexer::consume_all()
{
    if (is_eof())
        return {};

    auto rest = m_input.substring_view(m_index, tell_remaining());
    advance(tell_remaining());
    return rest;
}

// Consume until a new line is found
StringView GenericLexer::consume_line()
{
    size_t start = tell();
    while (!is_eof() && peek() != '\r' && peek() != '\n')
        advance(1);
    size_t length = tell() - start;

    consume_specific('\r');
    consume_specific('\n');

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until `stop` is peek'd
// The `stop` character is ignored, as it is user-defined
StringView GenericLexer::consume_until(char stop)
{
    size_t start = tell();
    while (!is_eof() && peek() != stop)
        advance(1);
    size_t length = tell() - start;

    ignore();

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

// Consume and return characters until the string `stop` is found
// The `stop` string is ignored, as it is user-defined
StringView GenericLexer::consume_until(const char* stop)
{
    size_t start = tell();
    while (!is_eof() && !next_is(stop))
        advance(1);
    size_t length = tell() - start;

    ignore(__builtin_strlen(stop));

    if (length == 0)
        return {};
    return m_input.substring_view(start, length);
}

/*
 * Consume a string surrounded by single or double quotes. The returned
 * StringView does not include the quotes. An escape character can be provided
 * to capture the enclosing quotes. Please note that the escape character will
 * still be in the resulting StringView
 */
StringView GenericLexer::consume_quoted_string(char escape_char)
{
    if (!next_is(is_quote))
        return {};

    auto start_position = position();
    char quote_char = consume();
    size_t start_index = tell();

    while (!is_eof()) {
        if (next_is(escape_char))
            advance(1);
        else if (next_is(quote_char))
            break;
        advance(1);
    }
    size_t length = tell() - start_index;

    if (peek() != quote_char) {
        // Restore the index in case the string is unterminated
        m_index = start_index - 1;
        m_position = start_position;
        return {};
    }

    // Ignore closing quote
    ignore();

    return m_input.substring_view(start_index, length);
}

String GenericLexer::consume_and_unescape_string(char escape_char)
{
    auto view = consume_quoted_string(escape_char);
    if (view.is_null())
        return {};

    StringBuilder builder;
    for (size_t i = 0; i < view.length(); ++i)
        builder.append(consume_escaped_character(escape_char));
    return builder.to_string();
}

}
