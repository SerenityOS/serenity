/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

namespace VT {

class Position {
public:
    Position() {}
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
