/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <LibIMAP/Parser.h>

namespace IMAP {

ParseStatus Parser::parse(ByteBuffer&& buffer, bool expecting_tag)
{
    if (m_incomplete) {
        m_buffer += buffer;
        m_incomplete = false;
    } else {
        m_buffer = move(buffer);
        position = 0;
        m_response = SolidResponse();
    }

    if (try_consume("+")) {
        consume(" ");
        auto data = parse_while([](u8 x) { return x != '\r'; });
        consume("\r\n");
        return { true, { ContinueRequest { data } } };
    }

    if (expecting_tag) {
        if (at_end()) {
            m_incomplete = true;
            return { true, {} };
        }
        parse_response_done();
    }

    if (m_parsing_failed) {
        return { false, {} };
    } else {
        return { true, { { move(m_response) } } };
    }
}

bool Parser::try_consume(StringView x)
{
    size_t i = 0;
    auto previous_position = position;
    while (i < x.length() && !at_end() && to_ascii_lowercase(x[i]) == to_ascii_lowercase(m_buffer[position])) {
        i++;
        position++;
    }
    if (i != x.length()) {
        // We didn't match the full string.
        position = previous_position;
        return false;
    }

    return true;
}

void Parser::parse_response_done()
{
    consume("A");
    auto tag = parse_number();
    consume(" ");

    ResponseStatus status = parse_status();
    consume(" ");

    m_response.m_tag = tag;
    m_response.m_status = status;

    StringBuilder response_data;

    while (!at_end() && m_buffer[position] != '\r') {
        response_data.append((char)m_buffer[position]);
        position += 1;
    }

    consume("\r\n");
    m_response.m_response_text = response_data.build();
}

void Parser::consume(StringView x)
{
    if (!try_consume(x)) {
        dbgln("{} not matched at {}, buffer: {}", x, position, StringView(m_buffer.data(), m_buffer.size()));

        m_parsing_failed = true;
    }
}

Optional<unsigned> Parser::try_parse_number()
{
    auto number_matched = 0;
    while (!at_end() && 0 <= m_buffer[position] - '0' && m_buffer[position] - '0' <= 9) {
        number_matched++;
        position++;
    }
    if (number_matched == 0)
        return {};

    auto number = StringView(m_buffer.data() + position - number_matched, number_matched);

    return number.to_uint();
}

unsigned Parser::parse_number()
{
    auto number = try_parse_number();
    if (!number.has_value()) {
        m_parsing_failed = true;
        return -1;
    }

    return number.value();
}

StringView Parser::parse_atom()
{
    auto is_non_atom_char = [](u8 x) {
        auto non_atom_chars = { '(', ')', '{', ' ', '%', '*', '"', '\\', ']' };
        return AK::find(non_atom_chars.begin(), non_atom_chars.end(), x) != non_atom_chars.end();
    };

    auto start = position;
    auto count = 0;
    while (!at_end() && !is_ascii_control(m_buffer[position]) && !is_non_atom_char(m_buffer[position])) {
        count++;
        position++;
    }

    return StringView(m_buffer.data() + start, count);
}

ResponseStatus Parser::parse_status()
{
    auto atom = parse_atom();

    if (atom.matches("OK")) {
        return ResponseStatus::OK;
    } else if (atom.matches("BAD")) {
        return ResponseStatus::Bad;
    } else if (atom.matches("NO")) {
        return ResponseStatus::No;
    }

    m_parsing_failed = true;
    return ResponseStatus::Bad;
}

StringView Parser::parse_while(Function<bool(u8)> should_consume)
{
    int chars = 0;
    while (!at_end() && should_consume(m_buffer[position])) {
        position++;
        chars++;
    }
    return StringView(m_buffer.data() + position - chars, chars);
}

}
