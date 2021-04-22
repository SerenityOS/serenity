/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"

namespace Cpp {

bool Position::operator<(const Position& other) const
{
    return line < other.line || (line == other.line && column < other.column);
}
bool Position::operator>(const Position& other) const
{
    return !(*this < other) && !(*this == other);
}
bool Position::operator==(const Position& other) const
{
    return line == other.line && column == other.column;
}

}
