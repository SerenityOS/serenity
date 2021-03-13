/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

namespace AK {

template<typename K, typename V, size_t Capacity>
class BinaryHeap {
public:
    BinaryHeap() = default;
    ~BinaryHeap() = default;

    // This constructor allows for O(n) construction of the heap (instead of O(nlogn) for repeated insertions)
    BinaryHeap(K keys[], V values[], size_t size)
    {
        VERIFY(size <= Capacity);
        m_size = size;
        for (size_t i = 0; i < size; i++) {
            m_elements[i].key = keys[i];
            m_elements[i].value = values[i];
        }

        for (ssize_t i = size / 2; i >= 0; i--) {
            heapify_down(i);
        }
    }

    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] bool is_empty() const { return m_size == 0; }

    void insert(K key, V value)
    {
        VERIFY(m_size < Capacity);
        auto index = m_size++;
        m_elements[index].key = key;
        m_elements[index].value = value;
        heapify_up(index);
    }

    V pop_min()
    {
        VERIFY(!is_empty());
        auto index = --m_size;
        swap(m_elements[0], m_elements[index]);
        heapify_down(0);
        return m_elements[index].value;
    }

    const V& peek_min() const
    {
        VERIFY(!is_empty());
        return m_elements[0].value;
    }

    const K& peek_min_key() const
    {
        VERIFY(!is_empty());
        return m_elements[0].key;
    }

    void clear()
    {
        m_size = 0;
    }

private:
    void heapify_down(size_t index)
    {
        while (index * 2 + 1 < m_size) {
            auto left_child = index * 2 + 1;
            auto right_child = index * 2 + 2;

            auto min_child = left_child;
            if (right_child < m_size && m_elements[right_child].key < m_elements[min_child].key)
                min_child = right_child;

            if (m_elements[index].key <= m_elements[min_child].key)
                break;
            swap(m_elements[index], m_elements[min_child]);
            index = min_child;
        }
    }

    void heapify_up(size_t index)
    {
        while (index != 0) {
            auto parent = (index - 1) / 2;

            if (m_elements[index].key >= m_elements[parent].key)
                break;
            swap(m_elements[index], m_elements[parent]);
            index = parent;
        }
    }

    struct {
        K key;
        V value;
    } m_elements[Capacity];
    size_t m_size { 0 };
};

}

using AK::BinaryHeap;
