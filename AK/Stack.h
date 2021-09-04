/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

namespace AK {

template<typename T, size_t stack_size>
class Stack {
public:
    Stack() = default;
    ~Stack() = default;

    bool push(T const& item)
    {
        if (m_stack.size() >= stack_size)
            return false;

        m_stack.append(item);
        return true;
    }

    bool push(T&& item)
    {
        if (m_stack.size() >= stack_size)
            return false;

        m_stack.append(move(item));
        return true;
    }

    bool is_empty() const
    {
        return m_stack.is_empty();
    }

    size_t size() const
    {
        return m_stack.size();
    }

    bool pop()
    {
        if (is_empty())
            return false;

        m_stack.resize_and_keep_capacity(m_stack.size() - 1);
        return true;
    }

    T& top()
    {
        return m_stack.last();
    }

    T const& top() const
    {
        return m_stack.last();
    }

private:
    Vector<T, stack_size> m_stack;
};

}

using AK::Stack;
