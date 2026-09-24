/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Math/Constants.h>
#include <AK/Math/Exponentials.h>

namespace AK {

namespace Gamma {

template<FloatingPoint T>
constexpr T lgamma_r(T x, int* sign)
{
    if (x == T { 1.0 } || x == T { 2.0 })
        return 0.0;
    if (isinf(x) || x == T { 0.0 })
        return Infinity<T>;

    // Use the Stirling approximation for log(gamma(x)) directly.
    // This allows us to support bigger values of x, where gamma(x) would have returned inf.
    // https://en.wikipedia.org/wiki/Stirling%27s_approximation
    T result = x * AK::log(x) - x;
    *sign = signbit(result) ? -1 : 1;
    return result;
}

}

using Gamma::lgamma_r;

}
