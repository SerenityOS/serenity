/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/FloatingPoint.h>
#include <AK/Math/Copysign.h>
#include <AK/Math/Division.h>
#include <AK/Math/Fabs.h>
#include <AK/NumericLimits.h>

#include <AK/Math/Macros.h>

namespace AK {

namespace Rounding {
template<FloatingPoint T>
constexpr T ceil(T num)
{
    // FIXME: SSE4.1 rounds[sd] num, res, 0b110
    if (is_constant_evaluated()) {
        if (num < NumericLimits<i64>::min() || num > NumericLimits<i64>::max())
            return num;
        return (static_cast<T>(static_cast<i64>(num)) == num)
            ? static_cast<i64>(num)
            : static_cast<i64>(num) + ((num > 0) ? 1 : 0);
    }
#if ARCH(AARCH64)
    AARCH64_INSTRUCTION(frintp, num);
#else
    CALL_BUILTIN(ceil, num);
#endif
}

template<FloatingPoint T>
constexpr T floor(T num)
{
    // FIXME: SSE4.1 rounds[sd] num, res, 0b101
    if (is_constant_evaluated()) {
        if (num < NumericLimits<i64>::min() || num > NumericLimits<i64>::max())
            return num;
        return (static_cast<T>(static_cast<i64>(num)) == num)
            ? static_cast<i64>(num)
            : static_cast<i64>(num) - ((num > 0) ? 0 : 1);
    }
#if ARCH(AARCH64)
    AARCH64_INSTRUCTION(frintm, num);
#else
    CALL_BUILTIN(floor, num);
#endif
}

template<FloatingPoint T>
constexpr T trunc(T num)
{
#if ARCH(AARCH64)
    if (is_constant_evaluated()) {
        if (num < NumericLimits<i64>::min() || num > NumericLimits<i64>::max())
            return num;
        return static_cast<T>(static_cast<i64>(num));
    }
    AARCH64_INSTRUCTION(frintz, num);
#endif
    // FIXME: Use dedicated instruction in the non constexpr case
    //        SSE4.1: rounds[sd] %num, %res, 0b111
    if (num < NumericLimits<i64>::min() || num > NumericLimits<i64>::max())
        return num;
    return static_cast<T>(static_cast<i64>(num));
}

template<FloatingPoint T>
constexpr T rint(T x)
{
    CONSTEXPR_STATE(rint, x);
    // Note: This does break tie to even
    //       But the behavior of frndint/rounds[ds]/frintx can be configured
    //       through the floating point control registers.
    // FIXME: We should decide if we rename this to allow us to get away from
    //        the configurability "burden" rint has
    //        this would make us use `rounds[sd] %num, %res, 0b100`
    //        and `frintn` respectively,
    //        no such guaranteed round exists for x87 `frndint`
#if ARCH(X86_64)
    if constexpr (IsSame<T, long double>) {
        asm(
            "frndint"
            : "+t"(x));
        return x;
    }
#    ifdef __SSE4_1__
    if constexpr (IsSame<T, double>) {
        T r;
        asm(
            "roundsd %1, %0"
            : "=x"(r)
            : "x"(x));
        return r;
    }
    if constexpr (IsSame<T, float>) {
        T r;
        asm(
            "roundss %1, %0"
            : "=x"(r)
            : "x"(x));
        return r;
    }
#    endif
#elif ARCH(AARCH64)
    AARCH64_INSTRUCTION(frintx, x);
#elif ARCH(RISCV64)
    if (__builtin_isnan(x))
        return x;

    // Floating point values have a gap size of >= 1 for values above 2^mantissa_bits - 1.
    if (fabs(x) > FloatExtractor<T>::mantissa_max)
        return x;

    if constexpr (IsSame<T, float>) {
        i64 r;
        asm("fcvt.l.s %0, %1, dyn"
            : "=r"(r)
            : "f"(x));
        return copysign(static_cast<float>(r), x);
    }
    if constexpr (IsSame<T, double>) {
        i64 r;
        asm("fcvt.l.d %0, %1, dyn"
            : "=r"(r)
            : "f"(x));
        return copysign(static_cast<double>(r), x);
    }
    if constexpr (IsSame<T, long double>)
        TODO_RISCV64();
#endif

    // Move the value out of the sub integer precision range and back,
    // This forces a rounding operation according to the current rounding mode
    if (fabs(x) > (T)FloatExtractor<T>::mantissa_max + 1)
        return x;
    auto adjust = copysign((T)FloatExtractor<T>::mantissa_max + 1, x);
    auto r = x + adjust;
    r -= adjust;
    return copysign(r, x); // This last copysign is to preserve the sign of 0s after rounding
}

template<FloatingPoint T>
constexpr T round(T x)
{
    CONSTEXPR_STATE(round, x);
    // Note: This is break-tie-away-from-zero, so not the hw's understanding of
    //       "nearest", which would be towards even.
    if (x == 0.)
        return x;
    if (x > 0.)
        return floor(x + .5);
    return ceil(x - .5);
}

template<Integral I, FloatingPoint P>
constexpr I round_to(P value)
{
    // Note: This implements a generic lrint,
    //       But we do not use the lrint builtin,
    //       as that is not well handled by compilers
    //       Also only Clang (20+) on risc-v and 21+ on x86
    //       Seem to merge the integer conversion and the rounding step,
    //       While all architectures have single instruction solutions for this
    //       So we "waste" one instruction in most cases
    //       https://serenity.godbolt.org/z/obPbMKs1d
    // Note: On x86 there are technically equivalent builtins available,
    //       but those are not handled as constexpr
    //       ( `__builtin_ia32_cvts[sd]2si64(float vector(4))` )
    return static_cast<I>(rint<P>(value));
}

}

using Rounding::ceil;
using Rounding::floor;
using Rounding::rint;
using Rounding::round;
using Rounding::round_to;
using Rounding::trunc;

template<Integral I, typename T>
constexpr I clamp_to(T value)
{
    constexpr auto max = static_cast<T>(NumericLimits<I>::max());
    if constexpr (max > 0) {
        if (value >= static_cast<T>(NumericLimits<I>::max()))
            return NumericLimits<I>::max();
    }

    constexpr auto min = static_cast<T>(NumericLimits<I>::min());
    if constexpr (min <= 0) {
        if (value <= static_cast<T>(NumericLimits<I>::min()))
            return NumericLimits<I>::min();
    }

    if constexpr (IsFloatingPoint<T>)
        return round_to<I>(value);

    return value;
}

// Wrap a to keep it in the range [-b, b].
template<typename T>
constexpr T wrap_to_range(T a, IdentityType<T> b)
{
    return fmod(fmod(a + b, 2 * b) + 2 * b, 2 * b) - b;
}

#include <AK/Math/UndefMacros.h>

}

#if USING_AK_GLOBALLY
using AK::round_to;
#endif
