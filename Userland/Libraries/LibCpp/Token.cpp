/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"
#include <AK/DeprecatedString.h>

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

DeprecatedString Token::to_deprecated_string() const
{
    return DeprecatedString::formatted("{}  {}:{}-{}:{} ({})", type_to_string(m_type), start().line, start().column, end().line, end().column, text());
}

DeprecatedString Token::type_as_deprecated_string() const
{
    return type_to_string(m_type);
}

}
