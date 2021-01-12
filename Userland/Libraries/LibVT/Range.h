/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibVT/Position.h>

namespace VT {

class Range {
public:
    Range() { }
    Range(const VT::Position& start, const VT::Position& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.is_valid() && m_end.is_valid(); }
    void clear()
    {
        m_start = {};
        m_end = {};
    }

    VT::Position& start() { return m_start; }
    VT::Position& end() { return m_end; }
    const VT::Position& start() const { return m_start; }
    const VT::Position& end() const { return m_end; }

    Range normalized() const { return Range(normalized_start(), normalized_end()); }

    void set_start(const VT::Position& position) { m_start = position; }
    void set_end(const VT::Position& position) { m_end = position; }

    void set(const VT::Position& start, const VT::Position& end)
    {
        m_start = start;
        m_end = end;
    }

    bool operator==(const Range& other) const
    {
        return m_start == other.m_start && m_end == other.m_end;
    }

    bool contains(const VT::Position& position) const
    {
        if (!(position.row() > m_start.row() || (position.row() == m_start.row() && position.column() >= m_start.column())))
            return false;
        if (!(position.row() < m_end.row() || (position.row() == m_end.row() && position.column() <= m_end.column())))
            return false;
        return true;
    }

private:
    VT::Position normalized_start() const { return m_start < m_end ? m_start : m_end; }
    VT::Position normalized_end() const { return m_start < m_end ? m_end : m_start; }

    VT::Position m_start;
    VT::Position m_end;
};

};
