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

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

template<typename T>
class History final {
public:
    void push(const T& item);
    T current() const;

    void go_back();
    void go_forward();

    bool can_go_back() { return m_current > 0; }
    bool can_go_forward() { return m_current + 1 < m_items.size(); }

    void clear();

private:
    Vector<T> m_items;
    int m_current { -1 };
};

template<typename T>
inline void History<T>::push(const T& item)
{
    m_items.shrink(m_current + 1);
    m_items.append(item);
    m_current++;
}

template<typename T>
inline T History<T>::current() const
{
    if (m_current == -1)
        return {};
    return m_items[m_current];
}

template<typename T>
inline void History<T>::go_back()
{
    ASSERT(can_go_back());
    m_current--;
}

template<typename T>
inline void History<T>::go_forward()
{
    ASSERT(can_go_forward());
    m_current++;
}

template<typename T>
inline void History<T>::clear()
{
    m_items = {};
    m_current = -1;
}
