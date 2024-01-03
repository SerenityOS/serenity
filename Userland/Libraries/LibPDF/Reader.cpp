/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Reader.h>
#include <ctype.h>

namespace PDF {

bool Reader::is_eol(char c)
{
    return c == 0xa || c == 0xd;
}

bool Reader::is_whitespace(char c)
{
    return is_eol(c) || is_non_eol_whitespace(c);
}

bool Reader::is_non_eol_whitespace(char c)
{
    // 3.1.1 Character Set
    return c == 0 || c == 0x9 || c == 0xc || c == ' ';
}

bool Reader::matches_eol() const
{
    return !done() && is_eol(peek());
}

bool Reader::matches_whitespace() const
{
    return !done() && is_whitespace(peek());
}

bool Reader::matches_non_eol_whitespace() const
{
    return !done() && is_non_eol_whitespace(peek());
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
    return !done() && !matches_delimiter() && !matches_whitespace();
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
    if (matches_eol()) {
        consume();
        return true;
    }
    return false;
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

bool Reader::consume_non_eol_whitespace()
{
    bool consumed = false;
    while (matches_non_eol_whitespace()) {
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
