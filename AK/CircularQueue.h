#pragma once

#include "Assertions.h"
#include "Types.h"

namespace AK {

template<typename T, size_t capacity>
class CircularQueue {
public:
    CircularQueue()
    {
        for (size_t i = 0; i < capacity; ++i)
            m_elements[i] = T();
    }

    bool isEmpty() const { return !m_size; }
    size_t size() const { return m_size; }

    void dump() const
    {
        kprintf("CircularQueue<%zu>:\n", capacity);
        kprintf(" size: %zu\n", m_size);
        for (size_t i = 0; i < capacity; ++i) {
            kprintf(" [%zu] %d %c\n", i, m_elements[i], i == m_head ? '*' : ' ');
        }
    }

    void enqueue(const T& t)
    {
        m_elements[(m_head + m_size) % capacity] = t;
        if (m_size == capacity)
            m_head = (m_head + 1) % capacity;
        else
            ++m_size;
    }

    T dequeue()
    {
        ASSERT(!isEmpty());
        T value = m_elements[m_head];
        m_head = (m_head + 1) % capacity;
        --m_size;
        return value;
    }

private:
    T m_elements[capacity];
    size_t m_size { 0 };
    size_t m_head { 0 };
};

}

using AK::CircularQueue;

