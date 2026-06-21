/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 * Copyright (c) 2024-2026, Nico Weber <thakis@chromium.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BuiltinWrappers.h>
#include <AK/Concepts.h>
#include <AK/FloatingPoint.h>
#include <AK/Math/Constants.h>

#include <AK/Math/Macros.h>

namespace AK {

namespace Division {
template<FloatingPoint T>
constexpr T fmod(T x, T y)
{
    CONSTEXPR_STATE(fmod, x, y);

#if ARCH(X86_64)
    u16 fpu_status;
    do {
        asm(
            "fprem\n"
            "fnstsw %%ax\n"
            : "+t"(x), "=a"(fpu_status)
            : "u"(y));
    } while (fpu_status & 0x400);
    return x;
#else
#    if defined(AK_OS_SERENITY)
    // FIXME: This is a very naive implementation.

    if (__builtin_isnan(x))
        return x;
    if (__builtin_isnan(y))
        return y;

    // SPECIAL VALUES
    //      fmod(±0, y) returns ±0 if y is neither 0 nor NaN.
    if (x == 0 && y != 0)
        return x;

    //      fmod(x, y) returns a NaN and raises the "invalid" floating-point exception for x infinite or y zero.
    // FIXME: Exception.
    if (__builtin_isinf(x) || y == 0)
        return NaN<T>;

    //      fmod(x, ±infinity) returns x for x not infinite.
    if (__builtin_isinf(y))
        return x;

    // Range reduction: handle negative x and y.
    if (y < 0)
        return fmod(x, -y);
    if (x < 0)
        return -fmod(-x, y);

    // x is x_mantissa * 2**x_exponent, y is y_mantissa * 2**y_exponent.
    // (Both are positive at this point.)
    // If y_exponent > x_exponent, we are done and can return x.
    // If y_exponent == x_exponent, we can return (x_mantissa % y_mantissa) * 2**x_exponent.
    // If y_exponent < x_exponent, we'll iteratively reduce x_exponent by shifting from
    // the exponent into the mantissa.

    auto x_bits = FloatExtractor<T>::from_float(x);
    typename FloatExtractor<T>::ComponentType x_exponent = x_bits.exponent;

    auto y_bits = FloatExtractor<T>::from_float(y);
    typename FloatExtractor<T>::ComponentType y_exponent = y_bits.exponent;

    // FIXME: Handle denormals. For now, treat them as 0.
    if (x_exponent == 0 && y_exponent != 0)
        return 0;
    if (y_exponent == 0)
        return NaN<T>;

    if (y_exponent > x_exponent)
        return x;

    // FIXME: This is wrong for f80.
    typename FloatExtractor<T>::ComponentType implicit_mantissa_bit = 1;
    implicit_mantissa_bit <<= FloatExtractor<T>::mantissa_bits;

    typename FloatExtractor<T>::ComponentType x_mantissa = x_bits.mantissa | implicit_mantissa_bit;
    typename FloatExtractor<T>::ComponentType y_mantissa = y_bits.mantissa | implicit_mantissa_bit;

    while (y_exponent < x_exponent) {
        // This is ok because (x % (y * 2**n)) divides (x % y) for all n > 0.
        x_mantissa %= y_mantissa;

        x_mantissa <<= 1;
        --x_exponent;
    }

    x_mantissa %= y_mantissa;

    if (x_mantissa == 0)
        return 0;

    // We're done and want to return x_mantissa * 2 ** x_exponent.
    // But x_mantissa might not have a leading 1 bit, so we have to realign first.
    // Mantissa is mantissa_bits long, count_leading_zeroes() counts in ComponentType, adjust:
    auto const always_zero_bits = sizeof(typename FloatExtractor<T>::ComponentType) * 8 - (FloatExtractor<T>::mantissa_bits + 1); // +1 for implicit 1 bit
    auto shift = count_leading_zeroes(x_mantissa) - always_zero_bits;

    if (x_exponent < shift) {
        // FIXME: Make a real denormal.
        return 0;
    }

    x_mantissa <<= shift;
    x_exponent -= shift;

    x_bits.exponent = x_exponent;
    x_bits.mantissa = x_mantissa;
    return x_bits.to_float();
#    else
    CALL_BUILTIN(fmod, x, y);
#    endif
#endif
}

template<FloatingPoint T>
constexpr T remainder(T x, T y)
{
    CONSTEXPR_STATE(remainder, x, y);

#if ARCH(X86_64)
    u16 fpu_status;
    do {
        asm(
            "fprem1\n"
            "fnstsw %%ax\n"
            : "+t"(x), "=a"(fpu_status)
            : "u"(y));
    } while (fpu_status & 0x400);
    return x;
#else
#    if defined(AK_OS_SERENITY)
    // TODO: Add implementation for this function.
    TODO();
#    endif
    CALL_BUILTIN(remainder, x, y);
#endif
}
}

#include <AK/Math/UndefMacros.h>

using Division::fmod;
using Division::remainder;

}
