/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Reader.h>
#include <ctype.h>

namespace PDF {

bool Reader::matches_eol() const
{
    return matches_any(0xa, 0xd);
}

bool Reader::matches_whitespace() const
{
    return matches_eol() || matches_any(0, 0x9, 0xc, ' ');
}

bool Reader::matches_number() const
{
    if (done())
        return false;
    auto ch = peek();
    return isdigit(ch) || ch == '-' || ch == '+' || ch == '.';
}

bool Reader::matches_delimiter() const
{
    return matches_any('(', ')', '<', '>', '[', ']', '{', '}', '/', '%');
}

bool Reader::matches_regular_character() const
{
    return !matches_delimiter() && !matches_whitespace();
}

bool Reader::consume_eol()
{
    if (done()) {
        return false;
    }
    if (matches("\r\n")) {
        consume(2);
        return true;
    }
    auto consumed = consume();
    return consumed == 0xd || consumed == 0xa;
}

bool Reader::consume_whitespace()
{
    bool consumed = false;
    while (matches_whitespace()) {
        consumed = true;
        consume();
    }
    return consumed;
}

char Reader::consume()
{
    return read();
}

void Reader::consume(int amount)
{
    for (size_t i = 0; i < static_cast<size_t>(amount); i++)
        consume();
}

bool Reader::consume(char ch)
{
    return consume() == ch;
}

}
