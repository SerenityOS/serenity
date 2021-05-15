/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Vector.h>

namespace AK {

template<typename T, int segment_size = 1000>
class Queue {
public:
    Queue() = default;
    ~Queue() = default;

    size_t size() const { return m_size; }
    bool is_empty() const { return m_size == 0; }

    template<typename U = T>
    void enqueue(U&& value)
    {
        if (m_segments.is_empty() || m_segments.last()->size() >= segment_size)
            m_segments.append(make<Vector<T, segment_size>>());
        m_segments.last()->append(forward<U>(value));
        ++m_size;
    }

    T dequeue()
    {
        VERIFY(!is_empty());
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
        VERIFY(!is_empty());
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
    size_t m_index_into_first { 0 };
    size_t m_size { 0 };
};

}

using AK::Queue;
