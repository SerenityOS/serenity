/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BuiltinWrappers.h>
#include <AK/Concepts.h>
#include <AK/FloatingPoint.h>
#include <AK/Math/Constants.h>
#include <AK/Math/Fabs.h>
#include <AK/NumericLimits.h>

#include <AK/Math/Macros.h>

namespace AK {

namespace Exponentials {

template<FloatingPoint T>
constexpr T log2(T x)
{
    CONSTEXPR_STATE(log2, x);

#if ARCH(X86_64)
    if constexpr (IsSame<T, long double>) {
        T ret;
        asm(
            "fld1\n"
            "fxch %%st(1)\n"
            "fyl2x\n"
            : "=t"(ret)
            : "0"(x));
        return ret;
    }
#endif
    // References:
    // Gist comparing some implementations
    // * https://gist.github.com/Hendiadyoin1/f58346d66637deb9156ef360aa158bf9

    if (x == 0)
        return -Infinity<T>;
    if (x <= 0 || __builtin_isnan(x))
        return NaN<T>;
    if (__builtin_isinf(x))
        return Infinity<T>;

    auto ext = FloatExtractor<T>::from_float(x);

    if (ext.exponent == 0) {
        // Denormal. log2(mantissa * 2 ** (-exponent_bias)) == log2(mantissa) - exponent_bias.
        // Shift mantissa until we have an implicit leading 1. ext.mantissa is always != 0 here.
        // Mantissa is mantissa_bits long, count_leading_zeroes() counts in ComponentType, adjust:
        auto const always_zero_bits = sizeof(typename FloatExtractor<T>::ComponentType) * 8 - (FloatExtractor<T>::mantissa_bits);
        unsigned leading_mantissa_zeroes = count_leading_zeroes(ext.mantissa) - always_zero_bits;

        int exponent = -FloatExtractor<T>::exponent_bias - leading_mantissa_zeroes;
        ext.mantissa <<= leading_mantissa_zeroes + 1;
        ext.exponent = FloatExtractor<T>::exponent_bias;
        return exponent + log2(ext.to_float());
    }

    // When the mantissa shows 0b00 (implicitly 1.0) we are on a power of 2
    T exponent = ext.exponent - FloatExtractor<T>::exponent_bias;
    if (ext.mantissa == 0)
        return exponent;

    FloatExtractor<T> mantissa_ext {
        .mantissa = ext.mantissa,
        .exponent = FloatExtractor<T>::exponent_bias,
        .sign = ext.sign
    };

    // (1 <= mantissa < 2)
    T m = mantissa_ext.to_float();

    // This is a reconstruction of one of Sun's algorithms
    // They use a transformation to lower the problem space,
    // while keeping the precision, and a 14th degree polynomial,
    // which is mirrored at sqrt(2)
    // TODO: Sun has some more algorithms for this and other math functions,
    //       leveraging LUTs, investigate those, if they are better in performance and/or precision.
    //       These seem to be related to crLibM's implementations, for which papers and references
    //       are available.
    // FIXME: Dynamically adjust the amount of precision between floats and doubles
    //        AKA floats might need less accuracy here, in comparison to doubles

    bool inverted = false;
    // m > sqrt(2)
    if (m > Sqrt2<T>) {
        inverted = true;
        m = 2 / m;
    }
    T s = (m - 1) / (m + 1);
    // ln((1 + s) / (1 - s)) == ln(m)
    T s2 = s * s;
    // clang-format off
    T high_approx = s2 * (static_cast<T>(0.6666666666666735130)
                  + s2 * (static_cast<T>(0.3999999999940941908)
                  + s2 * (static_cast<T>(0.2857142874366239149)
                  + s2 * (static_cast<T>(0.2222219843214978396)
                  + s2 * (static_cast<T>(0.1818357216161805012)
                  + s2 * (static_cast<T>(0.1531383769920937332)
                  + s2 *  static_cast<T>(0.1479819860511658591)))))));
    // clang-format on

    // ln(m) == 2 * s + s * high_approx
    // log2(m) == log2(e) * ln(m)
    T log2_mantissa = L2_E<T> * (2 * s + s * high_approx);
    if (inverted)
        log2_mantissa = 1 - log2_mantissa;
    return exponent + log2_mantissa;
}

template<FloatingPoint T>
constexpr T log(T x)
{
    CONSTEXPR_STATE(log, x);

#if ARCH(X86_64)
    T ret;
    asm(
        "fldln2\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
#elif defined(AK_OS_SERENITY)
    // FIXME: Adjust the polynomial and formula in log2 to fit this
    return log2<T>(x) / L2_E<T>;
#else
    CALL_BUILTIN(log, x);
#endif
}

template<FloatingPoint T>
constexpr T log10(T x)
{
    CONSTEXPR_STATE(log10, x);

#if ARCH(X86_64)
    T ret;
    asm(
        "fldlg2\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
#elif defined(AK_OS_SERENITY)
    // FIXME: Adjust the polynomial and formula in log2 to fit this
    return log2<T>(x) / L2_10<T>;
#else
    CALL_BUILTIN(log10, x);
#endif
}

template<FloatingPoint T>
constexpr T exp2(T exponent)
{
    CONSTEXPR_STATE(exp2, exponent);

#if ARCH(X86_64)
    T res;
    asm("fld1\n"
        "fld %%st(1)\n"
        "fprem\n"
        "f2xm1\n"
        "faddp\n"
        "fscale\n"
        "fstp %%st(1)"
        : "=t"(res)
        : "0"(exponent));
    return res;
#else
    // TODO: Add better implementation of this function.
    // This is just fast exponentiation for the integer part and
    // the first couple terms of the taylor series for the fractional part.

    if (exponent < 0)
        return 1 / exp2(-exponent);

    if (exponent >= log2(NumericLimits<T>::max()))
        return Infinity<T>;

    // Integer exponentiation part.
    int int_exponent = static_cast<int>(exponent);
    T exponent_fraction = exponent - int_exponent;

    T int_result = 1;
    T base = 2;
    for (;;) {
        if (int_exponent & 1)
            int_result *= base;
        int_exponent >>= 1;
        if (!int_exponent)
            break;
        base *= base;
    }

    // Fractional part.
    // Uses:
    // exp(x) = sum(n, 0, \infty, x ** n / n!)
    // 2**x = exp(log2(e) * x)
    // FIXME: Pick better step size (and make it dependent on T).
    T result = 0;
    T power = 1;
    T factorial = 1;
    for (int i = 1; i < 16; ++i) {
        result += power / factorial;
        power *= exponent_fraction / L2_E<T>;
        factorial *= i;
    }

    return int_result * result;
#endif
}

template<FloatingPoint T>
constexpr T exp(T exponent)
{
    CONSTEXPR_STATE(exp, exponent);

#if ARCH(X86_64)
    T res;
    asm("fldl2e\n"
        "fmulp\n"
        "fld1\n"
        "fld %%st(1)\n"
        "fprem\n"
        "f2xm1\n"
        "faddp\n"
        "fscale\n"
        "fstp %%st(1)"
        : "=t"(res)
        : "0"(exponent));
    return res;
#else
    // TODO: Add better implementation of this function.
    return exp2(exponent * L2_E<T>);
#endif
}

}

using Exponentials::exp;
using Exponentials::exp2;
using Exponentials::log;
using Exponentials::log10;
using Exponentials::log2;

// Calculate x^y with fast exponentiation when the power is a natural number.
template<FloatingPoint F, UnsignedIntegral U>
constexpr F pow_int(F x, U y)
{
    auto result = static_cast<F>(1);
    while (y > 0) {
        if (y % 2 == 1) {
            result *= x;
        }
        x = x * x;
        y >>= 1;
    }
    return result;
}

template<FloatingPoint T>
constexpr T pow(T x, T y)
{
    CONSTEXPR_STATE(pow, x, y);
    if (__builtin_isnan(y))
        return y;
    if (y == 0)
        return 1;
    if (x == 0)
        return 0;
    if (y == 1)
        return x;

    // Take an integer fast path as long as the value fits within a 64-bit integer.
    if (y >= static_cast<T>(NumericLimits<i64>::min()) && y < static_cast<T>(NumericLimits<i64>::max())) {
        i64 y_as_int = static_cast<i64>(y);
        if (y == static_cast<T>(y_as_int)) {
            T result = pow_int(x, static_cast<u64>(fabs<T>(y)));
            if (y < 0)
                result = static_cast<T>(1.0l) / result;
            return result;
        }
    }

    // FIXME: This formula suffers from error magnification.
    return exp2<T>(y * log2<T>(x));
}

#include <AK/Math/UndefMacros.h>

}
