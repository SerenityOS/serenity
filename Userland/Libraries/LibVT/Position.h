/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace VT {

class Position {
public:
    Position() = default;
    Position(int row, int column)
        : m_row(row)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_row >= 0 && m_column >= 0; }
    int row() const { return m_row; }
    int column() const { return m_column; }

    bool operator<(Position const& other) const
    {
        return m_row < other.m_row || (m_row == other.m_row && m_column < other.m_column);
    }

    bool operator<=(Position const& other) const
    {
        return *this < other || *this == other;
    }

    bool operator>=(Position const& other) const
    {
        return !(*this < other);
    }

    bool operator==(Position const& other) const
    {
        return m_row == other.m_row && m_column == other.m_column;
    }

    bool operator!=(Position const& other) const
    {
        return !(*this == other);
    }

private:
    int m_row { -1 };
    int m_column { -1 };
};

struct CursorPosition {
    size_t row { 0 };
    size_t column { 0 };

    void clamp(u16 max_row, u16 max_column)
    {
        row = min(row, max_row);
        column = min(column, max_column);
    }
};

}
