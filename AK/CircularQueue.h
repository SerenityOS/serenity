#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>

namespace AK {

template<typename T, int Capacity>
class CircularQueue {
public:
    CircularQueue()
    {
        for (int i = 0; i < Capacity; ++i)
            m_elements[i] = T();
    }

    void clear()
    {
        m_head = 0;
        m_size = 0;
    }

    bool is_empty() const { return !m_size; }
    int size() const { return m_size; }

    int capacity() const { return Capacity; }

    void enqueue(const T& t)
    {
        m_elements[(m_head + m_size) % Capacity] = t;
        if (m_size == Capacity)
            m_head = (m_head + 1) % Capacity;
        else
            ++m_size;
    }

    T dequeue()
    {
        ASSERT(!is_empty());
        T value = m_elements[m_head];
        m_head = (m_head + 1) % Capacity;
        --m_size;
        return value;
    }

    const T& at(int index) const { return m_elements[(m_head + index) % Capacity]; }

    const T& first() const { return at(0); }
    const T& last() const { return at(size() - 1); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) { return m_index != other.m_index; }
        ConstIterator& operator++()
        {
            m_index = (m_index + 1) % Capacity;
            if (m_index == m_queue.m_head)
                m_index = m_queue.m_size;
            return *this;
        }

        const T& operator*() const { return m_queue.m_elements[m_index]; }

    private:
        friend class CircularQueue;
        ConstIterator(const CircularQueue& queue, const int index)
            : m_queue(queue)
            , m_index(index)
        {
        }
        const CircularQueue& m_queue;
        int m_index { 0 };
    };

    ConstIterator begin() const { return ConstIterator(*this, m_head); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

    int head_index() const { return m_head; }

private:
    friend class ConstIterator;
    T m_elements[Capacity];
    int m_size { 0 };
    int m_head { 0 };
};

}

using AK::CircularQueue;
