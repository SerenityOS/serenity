#pragma once

#include <AK/Assertions.h>
#include <AK/CircularQueue.h>
#include <AK/Types.h>

namespace AK {

template<typename T, int Capacity>
class CircularDeque : public CircularQueue<T, Capacity> {

public:
    T dequeue_end()
    {
        ASSERT(!this->is_empty());
        T value = this->m_elements[(this->m_head + this->m_size - 1) % Capacity];
        this->m_size--;
        return value;
    }
};

}

using AK::CircularDeque;
