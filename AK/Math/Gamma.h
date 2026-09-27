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

// The lgamma function has some properties that are exploitable to make a good approximation.
//  - For x < 0, use Gamma reflection formula [1] to transpose the range to positive x values.
// The general approximation for the Gamma function (and by extension lgamma) is the Stirling
// approximation [1]. The error term of this approximation is ~O(1/x), so it's not exploitable
// for small values of x.
//  - For small values of x use a direct polynomial approximation.
//  - For bigger values of x, use the Stirling approximation.

// Also the lgamma_r variant is particular as it uses an out parameter to return a sign value.
// lgamma can be written as lgamma(x) = log(|Γ(x)|), the out parameter corresponds to the sign
// of Γ(x).

// [1] https://en.wikipedia.org/wiki/Gamma_function#Properties
// [2] https://en.wikipedia.org/wiki/Stirling%27s_approximation

template<FloatingPoint T>
constexpr T stirling_approximation(T x)
{
    return x * AK::log(x) - x;
}

template<FloatingPoint T>
constexpr T lgamma_r(T x, int* sign)
{
    if (x == T { 1.0 } || x == T { 2.0 })
        return 0.0;
    if (isinf(x) || x == T { 0.0 })
        return Infinity<T>;

    if (x > 0) {
        // ∀ x ∈ ℝ, x > 0 ⇒ Γ(x) > 0
        *sign = 1;
        // This value is arbitrary but somewhat close to what CORE-MATH is using.
        if (x < 8) {
            // FIXME: Use polynomial approximations for small values of x.
            return stirling_approximation(x);
        }
        return stirling_approximation(x);
    }

    // x < 0
    // FIXME: Use the reflection formula and compute the actual value of sign.
    *sign = -1;
    return stirling_approximation(x);
}

template<FloatingPoint T>
constexpr T lgamma(T x)
{
    int dummy {};
    return lgamma_r(x, &dummy);
}

}

using Gamma::lgamma;
using Gamma::lgamma_r;

}
