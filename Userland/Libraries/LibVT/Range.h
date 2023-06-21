/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibVT/Position.h>

namespace VT {

class Range {
public:
    Range() = default;
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

    void offset_row(int delta)
    {
        m_start = Position(m_start.row() + delta, m_start.column());
        m_end = Position(m_end.row() + delta, m_end.column());
    }

    bool operator==(Range const& other) const
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
