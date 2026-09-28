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
    // https://en.wikipedia.org/wiki/Stirling%27s_approximation
    // In the formula of the first section, the O(ln(n)) error term is way too big for our requirements.
    // But it's not complicated to do a lot better, instead start with the n! approximation,
    // use n! = Γ(n + 1), apply ln, and you get:
    // ln(Γ(n)) = (n - .5) · (ln(n - 1) - 1) + (ln(2π) + 1) / 2
    // The error term for this formula is O(1/n), much better!
    // Note that this approximation should only be used for x > 0, so that ln(Γ(n)) = ln(|Γ(n)|) = lgamma(x).
    return (x - T(.5)) * (AK::log(x - T(1)) - 1) + (AK::log(2 * AK::Pi<T>) + 1) / T(2);
}

namespace Details {

// FIXME: For the moment we only have an approximation for double, keeping the
//        warning will just make the code uglier with no benefits. Don't forget
//        to re-enable it once we add a float version!
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdouble-promotion"

template<FloatingPoint T>
constexpr T lgamma_for_large_positive_values(T x)
{
    auto base = stirling_approximation(x);
    if (x >= 1e7) {
        // Stirling approximation's error term is bound to be lower than the first term of the
        // Stirling series[1], in other words:
        //  For E(x) = lgamma(x) - stirling_approximation(x), we have E(x) < 1/(12 * x).
        // [1] https://en.wikipedia.org/wiki/Stirling%27s_approximation#Speed_of_convergence_and_error_estimates
        //
        // Given that E is monotonically decreasing and that ULP and lgamma are monotonically
        // increasing on ℝ₊ there exists a threshold t after which we have:
        // ∀ x ∈ [t, +∞], E(x) < ULP(lgamma(x))

        // FIXME: Exploiting this property is required to give an upper-bound to the polynomial approximation bellow,
        //        however it's not as simple as the small demonstration above.
        //        - We need to consider the error of floating point inaccuracies that we introduce when computing
        //          Stirling approximation with our imperfect float types. I suppose this is one of the reason
        //          CORE-MATH uses a double-double representation when computing log(x).
        //        - We need to properly find the actual values for each float types. For double, on [1e7, +inf], the max
        //          error of stirling_approximation is 2 ULP which is objectively incorrect but good enough for now.
        return base;
    }

    // FIXME: Implement the polynomial approximation mentioned above :^)
    return base;
}

#pragma GCC diagnostic pop

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
        return Details::lgamma_for_large_positive_values(x);
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
