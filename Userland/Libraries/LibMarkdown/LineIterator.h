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
    struct Context {
        enum class Type {
            ListItem,
            BlockQuote,
        };

        Type type;
        size_t indent;
        bool ignore_prefix;

        static Context list_item(size_t indent) { return { Type::ListItem, indent, true }; }
        static Context block_quote() { return { Type::BlockQuote, 0, false }; }
    };

    LineIterator(Vector<StringView>::ConstIterator const& lines)
        : m_iterator(lines)
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

    LineIterator operator+(ptrdiff_t delta) const
    {
        LineIterator copy = *this;
        copy.reset_ignore_prefix();
        copy.m_iterator = copy.m_iterator + delta;
        return copy;
    }

    LineIterator operator-(ptrdiff_t delta) const
    {
        LineIterator copy = *this;
        copy.reset_ignore_prefix();
        copy.m_iterator = copy.m_iterator - delta;
        return copy;
    }

    ptrdiff_t operator-(LineIterator const& other) const { return m_iterator - other.m_iterator; }

    FakePtr<StringView> operator->() const { return FakePtr<StringView>(operator*()); }

    void push_context(Context context) { m_context_stack.append(move(context)); }
    void pop_context() { m_context_stack.take_last(); }

private:
    void reset_ignore_prefix();
    Optional<StringView> match_context(StringView line) const;

    Vector<StringView>::ConstIterator m_iterator;
    Vector<Context> m_context_stack;
};

}
