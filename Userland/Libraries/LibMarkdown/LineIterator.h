/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Iterator.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Markdown {

template<typename T>
class FakePtr {
public:
    FakePtr(T item)
        : m_item(move(item))
    {
    }

    T const* operator->() const { return &m_item; }
    T* operator->() { return &m_item; }

private:
    T m_item;
};

class LineIterator {
public:
    LineIterator(Vector<StringView>::ConstIterator const& lines, size_t indent = 0)
        : m_iterator(lines)
        , m_indent(indent)
    {
    }

    bool is_end() const;
    StringView operator*() const;

    LineIterator operator++()
    {
        reset_ignore_prefix();
        ++m_iterator;
        return *this;
    }

    LineIterator operator++(int)
    {
        LineIterator tmp = *this;
        reset_ignore_prefix();
        ++m_iterator;
        return tmp;
    }

    LineIterator operator+(ptrdiff_t delta) const { return LineIterator { m_iterator + delta, m_indent }; }
    LineIterator operator-(ptrdiff_t delta) const { return LineIterator { m_iterator - delta, m_indent }; }
    ptrdiff_t operator-(LineIterator other) const { return m_iterator - other.m_iterator; }

    FakePtr<StringView> operator->() const { return FakePtr<StringView>(operator*()); }

    size_t indent() const { return m_indent; }
    void set_indent(size_t indent) { m_indent = indent; }
    void ignore_next_prefix() { m_ignore_prefix_mode = true; }

private:
    void reset_ignore_prefix() { m_ignore_prefix_mode = false; }
    bool is_indented(StringView const& line) const;

    Vector<StringView>::ConstIterator m_iterator;
    size_t m_indent;
    bool m_ignore_prefix_mode { false };
};

}
