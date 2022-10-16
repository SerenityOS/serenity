/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SinglyLinkedList.h>

namespace AK {

template<typename T>
class SinglyLinkedListWithCount : private SinglyLinkedList<T> {

public:
    SinglyLinkedListWithCount() = default;
    ~SinglyLinkedListWithCount() = default;

    using List = SinglyLinkedList<T>;

    using List::is_empty;
    using List::size_slow;

    inline size_t size() const
    {
        return m_count;
    }

    void clear()
    {
        List::clear();
        m_count = 0;
    }

    T& first()
    {
        return List::first();
    }

    T const& first() const
    {
        return List::first();
    }

    T& last()
    {
        return List::last();
    }

    T const& last() const
    {
        return List::last();
    }

    T take_first()
    {
        m_count--;
        return List::take_first();
    }

    template<typename U = T>
    ErrorOr<void> try_append(U&& value)
    {
        auto result = List::try_append(forward<T>(value));
        if (!result.is_error())
            m_count++;
        return result;
    }

#ifndef KERNEL
    template<typename U = T>
    void append(U&& value)
    {
        MUST(try_append(forward<T>(value)));
    }
#endif

    bool contains_slow(T const& value) const
    {
        return List::contains_slow(value);
    }

    using Iterator = typename List::Iterator;
    friend Iterator;
    Iterator begin() { return List::begin(); }
    Iterator end() { return List::end(); }

    using ConstIterator = typename List::ConstIterator;
    friend ConstIterator;
    ConstIterator begin() const { return List::begin(); }
    ConstIterator end() const { return List::end(); }

    template<typename TUnaryPredicate>
    ConstIterator find(TUnaryPredicate&& pred) const
    {
        return List::find_if(forward<TUnaryPredicate>(pred));
    }

    template<typename TUnaryPredicate>
    Iterator find(TUnaryPredicate&& pred)
    {
        return List::find_if(forward<TUnaryPredicate>(pred));
    }

    ConstIterator find(T const& value) const
    {
        return List::find(value);
    }

    Iterator find(T const& value)
    {
        return List::find(value);
    }

    void remove(Iterator iterator)
    {
        m_count--;
        return List::remove(iterator);
    }

    template<typename U = T>
    void insert_before(Iterator iterator, U&& value)
    {
        m_count++;
        List::insert_before(iterator, forward<T>(value));
    }

    template<typename U = T>
    void insert_after(Iterator iterator, U&& value)
    {
        m_count++;
        List::insert_after(iterator, forward<T>(value));
    }

private:
    size_t m_count { 0 };
};

}

#if USING_AK_GLOBALLY
using AK::SinglyLinkedListWithCount;
#endif
