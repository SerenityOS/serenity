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

// [1] https://en.wikipedia.org/wiki/Gamma_function#Properties
// [2] https://en.wikipedia.org/wiki/Stirling%27s_approximation

namespace Detail {

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

    // Let's have A be the same approximation as the one we use in the stirling_approximation function
    // (so the base stirling, with none of the terms of the series). So we have the general error term like:
    //   lgamma(x) - A(x) = E(x)
    // For the current range, (8, 1e7) we're going to reduce the error by approximating E.

    // We also know that the E is a polynomial in x⁻¹, so by applying a change of variable with y = 1 / x, we have:
    //   lgamma(1 / y) - A(1 / y) = Eₚ(y), on the range (1e-7, 1/8)
    // With Eₚ being a polynomial, finding a good approximation will be easier.

    // Finally, the funremez invocation looks like this:
    // funremez --double --degree 10 --range "1e-7:0.125" --relative-error "lgamma(1/x)-((1/x -0.5)*log(1/x - 1) - 1/x + 1 + log(2*pi)/2)"

    // FIXME: On the (8, 1e7) range, this gives us an ULP of 2.0. Again, this is objectively incorrect but good enough
    //        for now. Also, CORE-MATH is using a degree four polynomial in this step, find why we need a lot more!
    // FIXME: This polynomial is optimized for f64, add a specialized path for other floating point types.

    auto x_1 = 1 / x;
    T u = 0.07417342784580741;
    u = u * x_1 + 0.03536709695072373;
    u = u * x_1 + 0.050154352952855051;
    u = u * x_1 + 0.052834436386305993;
    u = u * x_1 + 0.059531774134891954;
    u = u * x_1 + 0.067460046884613073;
    u = u * x_1 + 0.075000005257110938;
    u = u * x_1 + 0.080555555503817405;
    u = u * x_1 + 0.083333333333530824;
    u = u * x_1 + 0.083333333333333204;
    auto error_correction = u * x_1 + 2.4840492255009263e-23;

    return base + error_correction;
}

#pragma GCC diagnostic pop

}

template<FloatingPoint T>
constexpr T lgamma(T x)
{
    if (x == T { 1.0 } || x == T { 2.0 })
        return 0.0;
    if (isinf(x) || x == T { 0.0 })
        return Infinity<T>;

    if (x > 0) {
        // This value is arbitrary but somewhat close to what CORE-MATH is using.
        if (x < 8) {
            // FIXME: Use polynomial approximations for small values of x.
            return Detail::stirling_approximation(x);
        }
        return Detail::lgamma_for_large_positive_values(x);
    }

    // x < 0
    // FIXME: Use the reflection formula.
    return Detail::stirling_approximation(x);
}

// The lgamma_r variant uses an out parameter to return a sign value.
// lgamma can be written as lgamma(x) = log(|Γ(x)|), the out parameter
// corresponds to the sign of Γ(x).
template<FloatingPoint T>
constexpr T lgamma_r(T x, int* sign)
{
    if (x > 0) {
        // ∀ x ∈ ℝ, x > 0 ⇒ Γ(x) > 0
        *sign = 1;
    } else {
        // FIXME: Correctly compute sign's value.
        *sign = -1;
    }

    return lgamma(x);
}

}

using Gamma::lgamma;
using Gamma::lgamma_r;

}
