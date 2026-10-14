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

/* The compilers will always inline these functions according to the following table
 * https://serenity.godbolt.org/z/ea5PW5cef
 *
 * Clang Inline table v20.1:
 * | Arch    | rint      | round     | ceil      | floor     | trunc     |
 * | ------- | --------- | --------- | --------- | --------- | --------- |
 * | X86     | +sse4.1   | +sse4.1*  | +sse4.1   | +sse4.1   | +sse4.1   |
 * | aarch64 | yes       | yes       | yes       | yes       | yes       |
 * | riscv   | roundtrip | roundtrip | roundtrip | roundtrip | roundtrip |
 *
 * *: `trunc(x + copysign(0.5-eps, x))`
 *
 * GCC Inline table v15.1:
 * | Arch    | rint      | round     | ceil       | floor       | trunc             |
 * | ------- | --------- | --------- | ---------- | ----------- | ----------------- |
 * | X86     | *,+sse4.1 | no        | **,+sse4.1 | ***,+sse4.1 | roundtrip,+sse4.1 |
 * | aarch64 | yes       | yes       | yes        | yes         | yes               |
 * | riscv   | no        | roundtrip | roundtrip  | roundtrip   | roundtrip         |
 *
 * GCC Actually uses x87 CW manipulation for `long double`
 * *   : Addition trick
 * **  : `copysign(x + (x>(long)x), x)` + Range check
 * *** : `copysign(x - (x>(long)x), x)` + Range check
 *
 * To maximize the usefulness we should always call the builtins,
 * if they are guaranteed to be inlined,
 * while providing fallbacks for long double (which even on x87 is rarely handled)
 * and equivalent implementations when the calls are not inlined
 */

namespace Rounding {
template<FloatingPoint T>
constexpr T ceil(T num)
{
    if (is_constant_evaluated() && !IsSame<T, long double>) {
        if (fabs(num) > FloatExtractor<T>::mantissa_max + 1)
            return num;
        auto trunc = static_cast<T>(static_cast<i64>(num));
        trunc += trunc < num;
        return copysign(trunc, num);
    }

    if constexpr (!IsSame<T, long double>) {

#if defined(__SSE_4_1__) || ARCH(AARCH64) || ARCH(RISCV64)
        CALL_BUILTIN(ceil, num);
#endif
    } else {
        // FIXME: The max mantissa for f80 long doubles is the limit of a u64
        //        And for f128 even higher, so we cant easily round trip
        //        GCC solves this with a rounding mode change and rint on x86,
        //        but that is hard to generalize
        return __builtin_ceil(num);
    }

    if (fabs(num) > FloatExtractor<T>::mantissa_max + 1)
        return num;
    auto trunc = static_cast<T>(static_cast<i64>(num));
    trunc += (trunc < num);
    return copysign(trunc, num);
}

template<FloatingPoint T>
constexpr T floor(T num)
{
    if (is_constant_evaluated() && !IsSame<T, long double>) {
        if (fabs(num) > FloatExtractor<T>::mantissa_max + 1)
            return num;
        auto trunc = static_cast<T>(static_cast<i64>(num));
        trunc -= (trunc < num);
        return copysign(trunc, num);
    }

    if constexpr (!IsSame<T, long double>) {
#if defined(__SSE_4_1__) || ARCH(AARCH64) || ARCH(RISCV64)
        CALL_BUILTIN(floor, num);
#endif
    } else {
        // FIXME: The max mantissa for f80 long doubles is the limit of a u64
        //        And for f128 even higher, so we cant easily round trip
        //        GCC solves this with a rounding mode change and rint on x86,
        //        but that is hard to generalize
        return __builtin_floorl(num);
    }

    if (fabs(num) > FloatExtractor<T>::mantissa_max + 1)
        return num;
    auto trunc = static_cast<T>(static_cast<i64>(num));
    trunc -= (trunc > num);
    return copysign(trunc, num);
}

template<FloatingPoint T>
constexpr T trunc(T num)
{
    if (is_constant_evaluated() && !IsSame<T, long double>) {
        if (fabs(num) > (FloatExtractor<T>::mantissa_max + 1))
            return num;
        auto trunc = static_cast<T>(static_cast<i64>(num));
        return copysign(trunc, num);
    }

    if constexpr (!IsSame<T, long double>) {
#if defined(__SSE_4_1__) || ARCH(AARCH64) || ARCH(RISCV64)
        CALL_BUILTIN(trunc, num);
#endif
    } else {
        // FIXME: The max mantissa for f80 long doubles is the limit of a u64
        //        And for f128 even higher, so we cant easily round trip
        //        GCC solves this with a rounding mode change and rint on x86,
        //        but that is hard to generalize
        return __builtin_truncl(num);
    }

    if (fabs(num) > (FloatExtractor<T>::mantissa_max + 1))
        return num;
    auto trunc = static_cast<T>(static_cast<i64>(num));
    return copysign(trunc, num);
}

template<FloatingPoint T>
constexpr T rint(T x)
{
    // Note: This does break tie to even
    //       But the behavior of frndint/rounds[ds]/frintx can be configured
    //       through the floating point control registers.
    if (is_constant_evaluated() && !IsSame<T, long double>) {
        // Move the value out of the sub integer precision range and back,
        // This forces a rounding operation according to the current rounding mode
        if (fabs(x) > FloatExtractor<T>::mantissa_max)
            return x;
        auto adjust = copysign((T)FloatExtractor<T>::mantissa_max + 1, x);
        auto r = x + adjust;
        r -= adjust;
        // This last copysign is to preserve the sign of 0s after rounding
        return copysign(r, x);
    }

#if ARCH(X86_64)
    // FIXME: There is no builtin for this
    // Note: GCC seems to inline it though
    if constexpr (IsSame<T, long double>) {
        asm(
            "frndint"
            : "+t"(x));
        return x;
    }
#endif

    if constexpr (!IsSame<T, long double>) {
#if defined(__SSE4_1__) || ARCH(AARCH64)
        CALL_BUILTIN(rint, x);
#elif defined(AK_COMPILER_CLANG) && ARCH(RISCV64)
        CALL_BUILTIN(rint, x);
        // FIXME: We should likely use rounding aware roundtrip-casts, similar to what clang expands this to,
        //        as the risc-v fallback when on GCC, but there are no builtins for those instructions
#endif
    }

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
#if (defined(AK_COMPILER_CLANG) && defined(__SSE4_1__)) || ARCH(AARCH64) || ARCH(RISCV64)
    CALL_BUILTIN(round, x);
#endif

    // This works by using the roundoff towards even to achieve break-tie away from 0,
    // as the addition of 0.49... is odd and rounded "up" to the even full number before the truncation
    // all other cases get truncated to the correct value after the addition
    // Proof for f16 <https://alive2.llvm.org/ce/z/siYYG9>
    // This method is nicer than the ceil/floor dance,
    // as it is ideally branchless, and I could not get that one to verify

    // FIXME: Is there a nicer way of getting these values?
    //        (1. - EPSILON/2)/2 seems to work,
    //        but not sure if that is generally the case
    T not_half;
    if constexpr (IsSame<T, float>) {
        not_half = 0x0.ffffffp-1f;
    } else if constexpr (IsSame<T, double>) {
        not_half = 0x1.fffffffffffffp-2;
    } else if constexpr (IsSame<T, long double>) {
#ifdef AK_HAS_FLOAT_80
        not_half = 0x0.ffffffffffffffffp-1L;
#else
        not_half = 0x1.ffffffffffffffffffffffffffffp-2l;
#endif
    }
    return trunc(x + copysign(not_half, x));
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
