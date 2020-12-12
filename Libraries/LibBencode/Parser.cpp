/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/ByteBuffer.h>
#include <LibBencode/Dictionary.h>
#include <LibBencode/List.h>
#include <LibBencode/Parser.h>
#include <LibBencode/Value.h>

namespace Bencode {

bool Parser::at_eof() const
{
    return m_index >= m_input.length();
}

char Parser::peek() const
{
    if (!at_eof())
        return m_input[m_index];
    return '\0';
}

char Parser::consume()
{
    if (!at_eof())
        return m_input[m_index++];
    return '\0';
}

bool Parser::consume_specific(char expected_ch)
{
    char consumed_ch = consume();
    return consumed_ch == expected_ch;
}

StringView Parser::consume_string(int length)
{
    if (m_index + length >= m_input.length())
        return "";
    m_index += length;
    return m_input.substring_view(m_index - length, length);
}

Optional<Value> Parser::parse()
{
    switch (peek()) {
    case 'd':
        return parse_dictionary();
    case 'l':
        return parse_list();
    case 'i':
        return parse_integer();
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
        return parse_string();
    }

    return {};
}

Optional<Value> Parser::parse_dictionary()
{
    if (!consume_specific('d'))
        return {};

    Dictionary dictionary;

    while (peek() != 'e' && !at_eof()) {
        auto key = parse_string();
        if (!key.has_value())
            return {};

        auto val = parse();
        if (!val.has_value())
            return {};

        dictionary.set(key.value().as_string(), move(val.value()));
    }

    if (!consume_specific('e'))
        return {};

    return dictionary;
}

Optional<Value> Parser::parse_list()
{
    if (!consume_specific('l'))
        return {};

    List list;

    while (peek() != 'e' && !at_eof()) {
        auto val = parse();
        if (!val.has_value())
            return {};

        list.append(val.value());
    }

    if (!consume_specific('e'))
        return {};

    return list;
}

Optional<Value> Parser::parse_integer()
{
    if (!consume_specific('i'))
        return {};

    bool negative = false;

    if (peek() == '-') {
        consume();
        negative = true;
    }

    i64 value = 0;

    while (peek() != 'e' && !at_eof() && peek() >= '0' && peek() <= '9') {
        value *= 10;
        value += consume() - '0';
    }

    if (negative)
        value = 0 - value;

    if (!consume_specific('e'))
        return {};

    return value;
}

Optional<Value> Parser::parse_string()
{
    size_t length = 0;

    while (peek() != 'e' && !at_eof() && peek() >= '0' && peek() <= '9') {
        length *= 10;
        length += consume() - '0';
    }

    if (!consume_specific(':'))
        return {};

    auto value = consume_string(length);
    if (value.length() != length)
        return {};

    return value;
}

}
