/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>

namespace AK {

// A fixed-size stack that can never allocate.
template<typename T, size_t stack_size>
class Stack {
public:
    Stack() = default;
    ~Stack() = default;

    bool push(T const& item)
    {
        if (index >= stack_size)
            return false;

        m_stack[index++] = item;
        return true;
    }

    bool push(T&& item)
    {
        if (index >= stack_size)
            return false;

        m_stack[index++] = item;
        return true;
    }

    bool is_empty() const
    {
        return index == 0;
    }

    size_t size() const
    {
        return index;
    }

    bool pop()
    {
        if (is_empty())
            return false;

        m_stack[--index].~T();
        return true;
    }

    T& top()
    {
        VERIFY(!is_empty());
        return m_stack[index - 1];
    }

    T const& top() const
    {
        VERIFY(!is_empty());
        return m_stack[index - 1];
    }

    bool contains_slow(T const& value) const
    {
        for (size_t i = 0; i < index; ++i) {
            if (Traits<T>::equals(m_stack[i], value))
                return true;
        }
        return false;
    }

private:
    Array<T, stack_size> m_stack;
    size_t index { 0 };
};

}

#if USING_AK_GLOBALLY
using AK::Stack;
#endif
