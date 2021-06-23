/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <compare>
// Unfortunatly, operator<=> is unusable on fundamental types without including <compare>

namespace AK {

class StrongOrdering {
public:
    template<typename T>
    constexpr StrongOrdering(const T& other)
    {
        if (other < 0) {
            m_value = -1;
        } else if (other > 0) {
            m_value = 1;
        } else {
            m_value = 0;
        }
    }

    bool constexpr operator==(const StrongOrdering& other) const { return m_value == other.m_value; }

    bool constexpr operator==(int zero) const { return m_value == zero; }
    bool constexpr operator<(int zero) const { return m_value < zero; }
    bool constexpr operator<=(int zero) const { return m_value <= zero; }
    bool constexpr operator>(int zero) const { return m_value > zero; }
    bool constexpr operator>=(int zero) const { return m_value >= zero; }

    StrongOrdering constexpr operator<=>(int) const { return *this; }

    static const StrongOrdering LessThan;
    static const StrongOrdering Equivalent;
    static const StrongOrdering Equal;
    static const StrongOrdering Greater;

private:
    i8 m_value;
};

constexpr StrongOrdering StrongOrdering::LessThan(-1);
constexpr StrongOrdering StrongOrdering::Equivalent(0);
constexpr StrongOrdering StrongOrdering::Equal(0);
constexpr StrongOrdering StrongOrdering::Greater(1);

}

using AK::StrongOrdering;
