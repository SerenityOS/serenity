/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

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

    [[nodiscard]] V const& peek_min() const
    {
        VERIFY(!is_empty());
        return m_elements[0].value;
    }

    [[nodiscard]] K const& peek_min_key() const
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

#if USING_AK_GLOBALLY
using AK::BinaryHeap;
#endif
