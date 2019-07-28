#pragma once

#include <AK/OwnPtr.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Vector.h>

namespace AK {

template<typename T>
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

    void clear()
    {
        m_segments.clear();
        m_index_into_first = 0;
        m_size = 0;
    }

private:
    static const int segment_size = 1000;

    SinglyLinkedList<OwnPtr<Vector<T, segment_size>>> m_segments;
    int m_index_into_first { 0 };
    int m_size { 0 };
};

}

using AK::Queue;

