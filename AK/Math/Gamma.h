/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Concepts.h>
#include <AK/Math/Constants.h>
#include <AK/Math/Exponentials.h>
#include <AK/Optional.h>

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
constexpr T lgamma_for_small_positive_values(T x)
{
    // FIXME: All threshold values and polynomial parameters are a bit arbitrary
    //        (read not precise enough) and fitted for f64. We can do better!

    Optional<Array<double, 13> const&> coefficients {};
    auto evaluate_polynomial = [&](double x) {
        auto& c = coefficients.value();

        T u = c[12];
        u = u * x + c[11];
        u = u * x + c[10];
        u = u * x + c[9];
        u = u * x + c[8];
        u = u * x + c[7];
        u = u * x + c[6];
        u = u * x + c[5];
        u = u * x + c[4];
        u = u * x + c[3];
        u = u * x + c[2];
        u = u * x + c[1];
        return u * x + c[0];
    };

    auto log_x = AK::log(x);

    if (x < 1e-75) {
        // From https://en.wikipedia.org/wiki/Gamma_function#Properties, we have
        //   Γ(x + 1) = x·Γ(x)
        // So for x > 0,
        //   lgamma(x) = lgamma(x + 1) - log(x)
        // For extremely small values of x, lgamma(x + 1) ≅ lgamma(1) = 0, thus
        //   lgamma(x) ≅ -log(x)
        // Such a fun result!

        // The maximum estimated error on the range (0, 1e-75) is of one ULP,
        // and it might come from log itself.
        return -log_x;
    }

    if (x < 0.7) {
        if (x < 0.1) {
            // funremez --array-coefficients --double --degree 12 --relative-error --range "1e-75:0.1" "(lgamma(x)+log(x))/x"
            // Estimated max error: 2.1095471969290096e-22
            static constexpr Array<double, 13> c = to_array({ -0.57721566490153287, 0.8224670334241132, -0.40068563438653121, 0.27058080842773413, -0.20738555102290712, 0.16955717660399525, -0.14404987950524956, 0.12550916046156002, -0.11132394890992627, 0.09995496071263206, -0.089571716421241981, 0.074622044450233571, -0.042935380788544232 });
            coefficients = c;
        } else if (x < 0.35) {
            // funremez --array-coefficients --double --degree 12 --relative-error --range "0.1:0.35" "(lgamma(x)+log(x))/x"
            // Estimated max error: 5.6331095247099388e-18
            static constexpr Array<double, 13> c = to_array({ -0.57721566489376896, 0.82246703287880096, -0.40068561695891258, 0.2705804725519832, -0.20738118973155248, 0.16951680417945111, -0.14377495655893524, 0.12410853230869183, -0.10595000663628681, 0.084482847437041236, -0.056865555816824655, 0.027406202694124891, -0.0067962738715122636 });
            coefficients = c;
        } else if (x < 0.5) {
            // funremez --array-coefficients --double --degree 12 --relative-error --range "0.35:0.5" "(lgamma(x)+log(x))/x"
            // Estimated max error: 1.4342138113890455e-21
            static constexpr Array<double, 13> c = to_array({ -0.57721565184616153, 0.82246661608038441, -0.40067945015115769, 0.2705244653935921, -0.20703252648816578, 0.16794474821026975, -0.13849441009202518, 0.11075392483351122, -0.080648836636775495, 0.0494068169434159, -0.023078676384150329, 0.0071247905058533494, -0.0010721689730627507 });
            coefficients = c;
        } else /* x < 0.7 */ {
            // funremez --array-coefficients --double --degree 12 --relative-error --range "0.5:0.7" "(lgamma(x)+log(x))/x"
            // Estimated max error: 2.0308193170383977e-20
            static constexpr Array<double, 13> c = to_array({ -0.57721537732229411, 0.82246046259629368, -0.40061584789131716, 0.27012341664207912, -0.20531342872813999, 0.16266496280205611, -0.12657630971460135, 0.090824646194702999, -0.056141952126849998, 0.027791645234529098, -0.010098760939205578, 0.0023605595041679365, -0.00026401691057724799 });
            coefficients = c;
        }

        return -log_x + x * evaluate_polynomial(x);
    }

    if (x < 2.2) {
        if (x < 0.9) {
            // funremez --array-coefficients --double --degree 12 --range "0.7:0.9" "lgamma(x)"
            // Estimated max error: 3.6074401384205048e-17
            static constexpr Array<double, 13> c = to_array({ 3.3345072755827614, -15.695023661598183, 53.16352155464299, -146.74101828038948, 310.76558816934494, -499.57831131692114, 609.7869609114972, -562.14526879008793, 385.50562588387277, -190.85861716984331, 64.537288742688617, -13.350839579027262, 1.275586260648041 });
            coefficients = c;
        } else if (x < 1) {
            // funremez --array-coefficients --double --degree 12 --range "0.9:1" "lgamma(x)"
            // Estimated max error: 4.5093224300846917e-22
            static constexpr Array<double, 13> c = to_array({ 3.1559282749776907, -13.226043455535558, 37.483070445699269, -86.252616305379561, 152.92198119030229, -206.06023653413729, 210.98233505699321, -163.25327666177367, 94.024010652134535, -39.116161718330929, 11.120636083909234, -1.9352537704282928, 0.15562674156907208 });
            coefficients = c;
        } else if (x < 1.1) {
            // funremez --array-coefficients --double --degree 12 --range "1:1.1" "lgamma(x)"
            // Estimated max error: 1.2255591040010462e-22
            static constexpr Array<double, 13> c = to_array({ 3.0555715944394501, -12.018363522850409, 30.817195654788371, -63.937660463519173, 102.46107182743258, -124.85828307762495, 115.63298224111564, -80.936884710900244, 42.1690766560627, -15.870770564055764, 4.0819788023940573, -0.64267259837872048, 0.046758161096466556 });
            coefficients = c;
        } else if (x < 1.4) {
            // funremez --array-coefficients --double --degree 12 --range "1.1:1.4" "lgamma(x)"
            // Estimated max error: 2.1137401252443663e-17
            static constexpr Array<double, 13> c = to_array({ 2.8875048499601226, -10.245826410738902, 22.231680366147298, -38.683413216035731, 52.216488166548814, -53.62817237114426, 41.852228321111703, -24.676758638938626, 10.825568628923328, -3.4289855363608996, 0.74188862505457598, -0.098207358273133247, 0.0060045737258323569 });
            coefficients = c;
        } else if (x < 1.8) {
            // funremez --array-coefficients --double --degree 12 --range "1.4:1.8" "lgamma(x)"
            // Estimated max error: 3.6137700628638454e-17
            static constexpr Array<double, 13> c = to_array({ 2.6410431903202203, -8.1332523448907228, 13.895462973941534, -18.660049072603371, 19.61232942266313, -15.716910781592727, 9.5777480750611943, -4.4109613050637524, 1.5116522259792744, -0.37406308633334673, 0.063226740484754129, -0.0065385942560778525, 0.00031231343007138979 });
            coefficients = c;
        } else /* x < 2.2 */ {
            // funremez --array-coefficients --double --degree 12 --range "1.8:2.2" "lgamma(x)"
            // Estimated max error: 1.9545356578917884e-18
            static constexpr Array<double, 13> c = to_array({ 2.4142226357941139, -6.5995539643503438, 9.1247453516993406, -9.6333548223238807, 8.0423003398435959, -5.1341702424025772, 2.4955931534945548, -0.91742134338956793, 0.25109138497385131, -0.049641025635058365, 0.0067059289893968005, -0.00055441685894539112, 2.1176740263715671e-05 });
            coefficients = c;
        }

        return evaluate_polynomial(x);
    }

    if (x < 3) {
        // funremez --array-coefficients --double --degree 12 --range "2.2:3" --relative-error "lgamma(x)/log(x)"
        // Estimated max error: 4.0747988315206307e-17
        static constexpr Array<double, 13> c = to_array({ -1.0140092863669461, 0.27308919290682188, 0.27459277895227646, -0.17895034813671823, 0.10155868910105534, -0.046280807592226178, 0.016487715343187961, -0.0045115306481199099, 0.00092797886161830197, -0.0001387482608136261, 1.4235322005005569e-05, -8.9647145852989569e-07, 2.6135972245433392e-08 });
        coefficients = c;
    } else if (x < 4) {
        // funremez --array-coefficients --double --degree 12 --range "3:4" --relative-error "lgamma(x)/log(x)"
        // Estimated max error: 8.3167597266564167e-18
        static constexpr Array<double, 13> c = to_array({ -1.0227936906321167, 0.30825302814639755, 0.20964938500115074, -0.10577586565765774, 0.045540998999265579, -0.015590268158598337, 0.0041509675678744074, -0.00084643912719587714, 0.00012952352208944848, -1.4391859324505774e-05, 1.0965893149889444e-06, -5.1264195457822302e-08, 1.1091728601456273e-09 });
        coefficients = c;
    } else if (x < 5) {
        // funremez --array-coefficients --double --degree 12 --range "4:5" --relative-error "lgamma(x)/log(x)"
        // Estimated max error: 2.4985921938024782e-19
        static constexpr Array<double, 13> c = to_array({ -1.0340030668137508, 0.34228881393157551, 0.1620563339262924, -0.065250404886817881, 0.022139148627473151, -0.00593636398281913, 0.0012340684205629894, -0.00019613119027219895, 2.3368589542380473e-05, -2.0206787080492976e-06, 1.1978462871510022e-07, -4.3561475255205255e-09, 7.3320096480979537e-11 });
        coefficients = c;
    } else if (x < 6) {
        // funremez --array-coefficients --double --degree 12 --range "5:6" --relative-error "lgamma(x)/log(x)"
        // Estimated max error: 1.6199532235666497e-20
        static constexpr Array<double, 13> c = to_array({ -1.0459612617199867, 0.37121811393603033, 0.12988319009896904, -0.043500291504055044, 0.012184675690738007, -0.0026871360733712571, 0.00045851122392306216, -5.9744807813313884e-05, 5.8322221459349584e-06, -4.130200894436517e-07, 2.0046804945131536e-08, -5.9684588516394061e-10, 8.2238580854342196e-12 });
        coefficients = c;
    } else /* x > 6 */ {
        // funremez --array-coefficients --double --degree 12 --range "6:8" --relative-error "lgamma(x)/log(x)"
        // Estimated max error: 5.415359627191459e-18
        static constexpr Array<double, 13> c = to_array({ -1.0637471370645954, 0.4058378169319185, 0.098874829873795117, -0.026600364756663185, 0.0059426323979211741, -0.0010411534443801481, 0.00014079006619985984, -1.4513831377632881e-05, 1.1194844974703888e-06, -6.257567007053987e-08, 2.3952139490086125e-09, -5.6193266225403641e-11, 6.0968932377962285e-13 });
        coefficients = c;
    }

    return log_x * evaluate_polynomial(x);
}

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
        if (x < 8)
            return Detail::lgamma_for_small_positive_values(x);
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
