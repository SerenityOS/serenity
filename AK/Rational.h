/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Traits.h>
#include <math.h>

namespace AK {

template<typename T>
class Rational {
public:
    Rational()
        : m_numerator(0)
        , m_denominator(1)
    {
    }

    Rational(T const numerator, T const denominator)
        : m_numerator(numerator)
        , m_denominator(denominator)
    {
        static_assert(IsIntegral<T> == true);
    }

    // FIXME: Create function to create type from double

    T numerator() const
    {
        return m_numerator;
    }

    T denominator() const
    {
        return m_denominator;
    }

    double to_double() const
    {
        if (m_denominator == 0) {
            return NAN;
        }
        return static_cast<double>(m_numerator) / m_denominator;
    }

    String to_string() const
    {
        return String::formatted("{}/{}", m_numerator, m_denominator);
    }

    // FIXME: Create calculation functions e.g. addition

    // FIXME: Create a reduction function using GCD

private:
    T m_numerator;
    T m_denominator;
};

}

using AK::Rational;
