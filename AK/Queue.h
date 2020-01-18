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

#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Vector.h>

namespace AK {

template<typename T, int segment_size = 1000>
class Queue {
public:
    Queue() { }
    ~Queue() { }

    int size() const { return m_size; }
    bool is_empty() const { return m_size == 0; }

    void enqueue(T&& value)
    {
        if (m_segments.is_empty() || m_segments.last()->size() >= segment_size)
            m_segments.append(make<Vector<T, segment_size>>());
        m_segments.last()->append(move(value));
        ++m_size;
    }

    void enqueue(const T& value)
    {
        enqueue(T(value));
    }

    T dequeue()
    {
        ASSERT(!is_empty());
        auto value = move((*m_segments.first())[m_index_into_first++]);
        if (m_index_into_first == segment_size) {
            m_segments.take_first();
            m_index_into_first = 0;
        }
        --m_size;
        return value;
    }

    const T& head() const
    {
        ASSERT(!is_empty());
        return (*m_segments.first())[m_index_into_first];
    }

    void clear()
    {
        m_segments.clear();
        m_index_into_first = 0;
        m_size = 0;
    }

private:
    SinglyLinkedList<OwnPtr<Vector<T, segment_size>>> m_segments;
    int m_index_into_first { 0 };
    int m_size { 0 };
};

}

using AK::Queue;

