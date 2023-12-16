/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"
#include <AK/ByteString.h>

namespace Cpp {

bool Position::operator<(Position const& other) const
{
    return line < other.line || (line == other.line && column < other.column);
}
bool Position::operator>(Position const& other) const
{
    return !(*this < other) && !(*this == other);
}
bool Position::operator==(Position const& other) const
{
    return line == other.line && column == other.column;
}
bool Position::operator<=(Position const& other) const
{
    return !(*this > other);
}

ByteString Token::to_byte_string() const
{
    return ByteString::formatted("{}  {}:{}-{}:{} ({})", type_to_string(m_type), start().line, start().column, end().line, end().column, text());
}

ByteString Token::type_as_byte_string() const
{
    return type_to_string(m_type);
}

}
