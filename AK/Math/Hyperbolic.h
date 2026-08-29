/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math/Copysign.h>
#include <AK/Math/Sqrt.h>
#include <math.h>

#include <AK/Math/Macros.h>

namespace AK {

namespace Hyperbolic {

template<FloatingPoint T>
constexpr T sinh(T x)
{
    // "If x is NaN, a NaN shall be returned."
    if (isnan(x))
        return NaN<T>;
    // "If x is ±0 or ±Inf, x shall be returned."
    if (x == 0 || isinf(x))
        return x;

    T exponentiated = exp<T>(x);
    if (x > 0)
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    return (exponentiated - 1 / exponentiated) / 2;
}

template<FloatingPoint T>
constexpr T cosh(T x)
{
    CONSTEXPR_STATE(cosh, x);

    // "If x is NaN, a NaN shall be returned."
    if (isnan(x))
        return NaN<T>;
    // "If x is ±0, the value 1.0 shall be returned."
    if (x == 0)
        return T(1.0);
    // "If x is ±Inf, +Inf shall be returned."
    if (isinf(x))
        return Infinity<T>;

    T exponentiated = exp(-x);
    if (x < 0)
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    return (1 / exponentiated + exponentiated) / 2;
}

template<FloatingPoint T>
constexpr T tanh(T x)
{
    // "If x is NaN, a NaN shall be returned."
    if (isnan(x))
        return NaN<T>;
    // "If x is ±0, x shall be returned."
    if (x == 0)
        return x;
    // "If x is ±Inf, ±1 shall be returned."
    if (isinf(x))
        return AK::copysign(T(1), x);

    if (x > 0) {
        T exponentiated = exp<T>(2 * x);
        return (exponentiated - 1) / (exponentiated + 1);
    }
    T plusX = exp<T>(x);
    T minusX = 1 / plusX;
    return (plusX - minusX) / (plusX + minusX);
}

template<FloatingPoint T>
constexpr T asinh(T x)
{
    return log<T>(x + sqrt<T>(x * x + 1));
}

template<FloatingPoint T>
constexpr T acosh(T x)
{
    return log<T>(x + sqrt<T>(x * x - 1));
}

template<FloatingPoint T>
constexpr T atanh(T x)
{
    return log<T>((1 + x) / (1 - x)) / (T)2.0l;
}

}

#include <AK/Math/UndefMacros.h>

using Hyperbolic::acosh;
using Hyperbolic::asinh;
using Hyperbolic::atanh;
using Hyperbolic::cosh;
using Hyperbolic::sinh;
using Hyperbolic::tanh;

}
