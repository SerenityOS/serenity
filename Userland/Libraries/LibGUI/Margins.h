/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace GUI {

class Margins {
public:
    Margins() { }
    Margins(int left, int top, int right, int bottom)
        : m_left(left)
        , m_top(top)
        , m_right(right)
        , m_bottom(bottom)
    {
    }
    ~Margins() { }

    bool is_null() const { return !m_left && !m_top && !m_right && !m_bottom; }

    int left() const { return m_left; }
    int top() const { return m_top; }
    int right() const { return m_right; }
    int bottom() const { return m_bottom; }

    void set_left(int value) { m_left = value; }
    void set_top(int value) { m_top = value; }
    void set_right(int value) { m_right = value; }
    void set_bottom(int value) { m_bottom = value; }

    bool operator==(const Margins& other) const
    {
        return m_left == other.m_left
            && m_top == other.m_top
            && m_right == other.m_right
            && m_bottom == other.m_bottom;
    }

private:
    int m_left { 0 };
    int m_top { 0 };
    int m_right { 0 };
    int m_bottom { 0 };
};
}
