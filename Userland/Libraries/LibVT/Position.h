/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace VT {

class Position {
public:
    Position() { }
    Position(int row, int column)
        : m_row(row)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_row >= 0 && m_column >= 0; }
    int row() const { return m_row; }
    int column() const { return m_column; }

    bool operator<(const Position& other) const
    {
        return m_row < other.m_row || (m_row == other.m_row && m_column < other.m_column);
    }

    bool operator<=(const Position& other) const
    {
        return *this < other || *this == other;
    }

    bool operator>=(const Position& other) const
    {
        return !(*this < other);
    }

    bool operator==(const Position& other) const
    {
        return m_row == other.m_row && m_column == other.m_column;
    }

    bool operator!=(const Position& other) const
    {
        return !(*this == other);
    }

private:
    int m_row { -1 };
    int m_column { -1 };
};

}
