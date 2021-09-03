/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Math.h>
#include <YAK/QuickSort.h>
#include <YAK/StdLibExtraDetails.h>
#include <YAK/Vector.h>

namespace YAK {

template<typename T = float>
requires(IsArithmetic<T>) class Statistics {
public:
    Statistics() = default;
    ~Statistics() = default;

    void add(T const& value)
    {
        // FIXME: Check for an overflow
        m_sum += value;
        m_values.append(value);
    }

    T const sum() const { return m_sum; }
    float average() const { return (float)sum() / size(); }

    // FIXME: Implement a better algorithm
    T const median()
    {
        quick_sort(m_values);
        return m_values.at(size() / 2);
    }

    float standard_deviation() const { return sqrt(variance()); }
    float variance() const
    {
        float summation = 0;
        float avg = average();
        for (T number : values()) {
            float difference = (float)number - avg;
            summation += (difference * difference);
        }
        summation = summation / size();
        return summation;
    }

    Vector<T> const& values() const { return m_values; }
    size_t size() const { return m_values.size(); }

private:
    Vector<T> m_values;
    T m_sum {};
};

}
