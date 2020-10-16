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

#include <AK/Assertions.h>
#include <AK/Forward.h>
#include <AK/StdLibExtras.h>

namespace AK {

template<typename T, size_t Capacity>
class CircularQueue {
    friend CircularDuplexStream<Capacity>;

public:
    constexpr CircularQueue() = default;
    constexpr CircularQueue(const Array<T, Capacity * sizeof(T)>&& a)
        : m_storage(a)
        , m_size(Capacity)
    {
    }

    constexpr ~CircularQueue()
    {
        clear();
    }

    constexpr void clear()
    {
        for (size_t i = 0; i < m_size; ++i)
            elements()[(m_head + i) % Capacity].~T();

        m_head = 0;
        m_size = 0;
    }

    constexpr bool is_empty() const { return !m_size; }
    constexpr size_t size() const { return m_size; }

    constexpr size_t capacity() const { return Capacity; }

    constexpr void enqueue(T&& value)
    {
        auto& slot = elements()[(m_head + m_size) % Capacity];
        slot = value;
        if (m_size == Capacity)
            m_head = (m_head + 1) % Capacity;
        else
            ++m_size;
    }

    constexpr void enqueue(const T& value)
    {
        enqueue(T(value));
    }

    constexpr T dequeue()
    {
        ASSERT(!is_empty());
        auto& slot = elements()[m_head];
        T value = move(slot);
        m_head = (m_head + 1) % Capacity;
        --m_size;
        return value;
    }

    constexpr const T& at(size_t index) const { return elements()[(m_head + index) % Capacity]; }

    constexpr const T& first() const { return at(0); }
    constexpr const T& last() const { return at(size() - 1); }

    class ConstIterator {
    public:
        constexpr bool operator==(const ConstIterator& other) const { return m_index == other.m_index; }
        constexpr bool operator!=(const ConstIterator& other) const { return m_index != other.m_index; }
        constexpr ConstIterator& operator++()
        {
            m_index = (m_index + 1) % Capacity;
            if (m_index == m_queue.m_head)
                m_index = m_queue.m_size;
            return *this;
        }

        constexpr const T& operator*() const { return m_queue.elements()[m_index]; }

    private:
        friend class CircularQueue;
        constexpr ConstIterator(const CircularQueue& queue, const size_t index)
            : m_queue(queue)
            , m_index(index)
        {
        }
        const CircularQueue& m_queue;
        size_t m_index { 0 };
    };

    constexpr ConstIterator begin() const { return ConstIterator(*this, m_head); }
    constexpr ConstIterator end() const { return ConstIterator(*this, size()); }

    constexpr size_t head_index() const { return m_head; }

protected:
    constexpr T* elements() { return &m_storage[0]; }
    constexpr const T* elements() const { return &m_storage[0]; }

    friend class ConstIterator;
    alignas(T) Array<T, sizeof(T) * Capacity> m_storage {};
    size_t m_size { 0 };
    size_t m_head { 0 };
};

}

using AK::CircularQueue;
