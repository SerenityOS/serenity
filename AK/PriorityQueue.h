/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace AK {

// Implementation of a Priority Queue using a heap
template<typename T>
class PriorityQueue {
public:
    PriorityQueue() = default;
    template<class... Args>
    void emplace(Args&&... args)
    {
        m_values.template empend(forward<Args>(args)...);
        bubble_up(m_values.size() - 1);
    }
    void insert(T value)
    {
        m_values.append(move(value));
        bubble_up(m_values.size() - 1);
    }
    T const& peek() const
    {
        VERIFY(!m_values.is_empty());
        return m_values[0];
    }
    T take()
    {
        VERIFY(!m_values.is_empty());
        T return_value = move(m_values[0]);
        size_t i;
        // Iterate until i is a leaf node
        for (i = 1; 2 * i <= m_values.size();) {
            auto& current_value = m_values[i - 1];
            auto& left_child = m_values[2 * i - 1];
            VERIFY(current_value <= left_child);
            if (2 * i + 1 <= m_values.size()) {
                // Both children are valid
                auto& right_child = m_values[2 * i];
                VERIFY(current_value <= right_child);
                if (left_child < right_child) {
                    current_value = left_child;
                    i = 2 * i;
                } else {
                    current_value = right_child;
                    i = 2 * i + 1;
                }
            } else {
                // Only the left child is valid
                VERIFY(current_value <= left_child);
                current_value = left_child;
                i = i * 2;
            }
        }
        if (i < m_values.size()) {
            // We have created a hole in our vector, we will fill this with
            // the last value in the vector
            m_values[i - 1] = m_values.last();
            bubble_up(i - 1);
        }
        m_values.take_last();
        return return_value;
    }
    size_t size() const { return m_values.size(); }
    bool is_empty() const { return m_values.is_empty(); }

private:
    void bubble_up(size_t index)
    {
        for (size_t i = index + 1; i > 1; i = i / 2) {
            auto& current_position = m_values[i - 1];
            auto& parent = m_values[(i / 2) - 1];
            if (current_position < parent)
                swap(current_position, parent);
            else
                break;
        }
    }
    Vector<T> m_values;
};

}
