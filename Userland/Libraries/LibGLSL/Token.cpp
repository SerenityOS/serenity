/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"
#include <AK/String.h>

namespace GLSL {

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

ErrorOr<String> Token::to_string() const
{
    return String::formatted("{}  {}:{}-{}:{} ({})", type_to_string(m_type), start().line, start().column, end().line, end().column, text());
}

ErrorOr<String> Token::type_as_string() const
{
    auto str = type_to_string(m_type);
    auto view = StringView(str, strlen(str));
    return String::from_utf8(view);
}

}
