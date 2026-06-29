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
    ELEMENTWISE_BUILTIN(ceil, num);

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
    ELEMENTWISE_BUILTIN(floor, num);

#if ARCH(AARCH64)
    AARCH64_INSTRUCTION(frintm, num);
#else
    CALL_BUILTIN(floor, num);
#endif
}

template<FloatingPoint T>
constexpr T trunc(T num)
{
    if (is_constant_evaluated()) {
        if (num < NumericLimits<i64>::min() || num > NumericLimits<i64>::max())
            return num;
        return static_cast<T>(static_cast<i64>(num));
    }
    ELEMENTWISE_BUILTIN(trunc, num);

#if ARCH(AARCH64)
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
    // This is only inlined when SSE4.1 is enabled
    // also for some reason this does not work nicely for frndint
    ELEMENTWISE_BUILTIN(rint, x);
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
    ELEMENTWISE_BUILTIN(rint, x);
    AARCH64_INSTRUCTION(frintx, x);
#elif ARCH(RISCV64)
    ELEMENTWISE_BUILTIN(rint, x);
    // NOTE: RISC-V only gives us an lrint instruction, which we could use to round-trip cast
    //       Compilers seem to prefer that over the method below
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
    if (fabs(x) > FloatExtractor<T>::mantissa_max)
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
#if ARCH(AARCH64) || ARCH(RISCV64) || defined(__SSE4_1__)
    ELEMENTWISE_BUILTIN(round, x);
#endif
    // Note: This is break-tie-away-from-zero, so not the hw's understanding of
    //       "nearest", which would be towards even.
    if (x == 0.)
        return x;
    if (x > 0.)
        return floor(x + .5);
    return ceil(x - .5);
}

template<Integral I, FloatingPoint P>
requires(false)
I round_to_impl(P value);

#if ARCH(X86_64)
template<Integral I>
requires(!IsSame<I, u64>) // fistpl can only output signed numbers
                          // so u64 is out
ALWAYS_INLINE I round_to_impl(long double value)
{
    // Note: fistps outputs into a signed integer location (i16, i32, i64),
    //       So we need at least i16 for an output slot,
    //       and must take the next higher int for unsigned values
    // Note: Posix allows us to return undefined values on overflows
    //       (though we should throw a domain error, which we dont in the cases posix lrint doesn't know about)
    if constexpr (IsOneOf<I, i64, u32>) {
        i64 ret;
        asm("fistpll %0"
            : "=m"(ret)
            : "t"(value)
            : "st");
        return static_cast<I>(ret);
    } else if constexpr (IsOneOf<I, i32, u16>) {
        i32 ret;
        asm("fistpl %0"
            : "=m"(ret)
            : "t"(value)
            : "st");
        return static_cast<I>(ret);
    } else {
        i16 ret;
        asm("fistps %0"
            : "=m"(ret)
            : "t"(value)
            : "st");
        return static_cast<I>(ret);
    }
}

template<Integral I>
requires(!IsSame<I, u64>) // cvtss2si can only output signed numbers
                          // so u64 is out
ALWAYS_INLINE I round_to_impl(float value)
{
    if constexpr (IsOneOf<I, i64, u32>) {
        i64 ret;
        asm("cvtss2si %1, %0"
            : "=r"(ret)
            : "xm"(value));
        return static_cast<I>(ret);
    } else {
        i32 ret;
        asm("cvtss2si %1, %0"
            : "=r"(ret)
            : "xm"(value));
        return static_cast<I>(ret);
    }
}

template<Integral I>
requires(!IsSame<I, u64>) // cvtsd2si can only output signed numbers
                          // so u64 is out
ALWAYS_INLINE I round_to_impl(double value)
{
    if constexpr (IsOneOf<I, i64, u32>) {
        i64 ret;
        asm("cvtsd2si %1, %0"
            : "=r"(ret)
            : "xm"(value));
        return static_cast<I>(ret);
    }
    i32 ret;
    asm("cvtsd2si %1, %0"
        : "=r"(ret)
        : "xm"(value));
    return static_cast<I>(ret);
}

#elif ARCH(AARCH64)
template<SignedIntegral I>
ALWAYS_INLINE I round_to_impl(float value)
{
    if constexpr (sizeof(I) <= sizeof(u32)) {
        i32 res;
        asm("fcvtns %w0, %s1"
            : "=r"(res)
            : "w"(value));
        return static_cast<I>(res);
    }
    i64 res;
    asm("fcvtns %0, %s1"
        : "=r"(res)
        : "w"(value));
    return static_cast<I>(res);
}

template<SignedIntegral I>
ALWAYS_INLINE I round_to_impl(double value)
{
    if constexpr (sizeof(I) <= sizeof(u32)) {
        i32 res;
        asm("fcvtns %w0, %d1"
            : "=r"(res)
            : "w"(value));
        return static_cast<I>(res);
    }
    i64 res;
    asm("fcvtns %0, %d1"
        : "=r"(res)
        : "w"(value));
    return static_cast<I>(res);
}

template<UnsignedIntegral U>
ALWAYS_INLINE U round_to_impl(float value)
{
    if constexpr (sizeof(U) <= sizeof(u32)) {
        u32 res;
        asm("fcvtnu %w0, %s1"
            : "=r"(res)
            : "w"(value));
        return static_cast<U>(res);
    }
    u64 res;
    asm("fcvtnu %0, %s1"
        : "=r"(res)
        : "w"(value));
    return static_cast<U>(res);
}

template<UnsignedIntegral U>
ALWAYS_INLINE U round_to_impl(double value)
{
    if constexpr (sizeof(U) <= sizeof(u32)) {
        u32 res;
        asm("fcvtnu %w0, %d1"
            : "=r"(res)
            : "w"(value));
        return static_cast<U>(res);
    }
    u64 res;
    asm("fcvtnu %0, %d1"
        : "=r"(res)
        : "w"(value));
    return static_cast<U>(res);
}

#elif ARCH(RISCV64)
template<SignedIntegral I>
ALWAYS_INLINE I round_to_impl(float value)
{
    if constexpr (sizeof(I) <= sizeof(u32)) {
        i32 res;
        asm("fcvt.w.s %w0, %1"
            : "=r"(res)
            : "f"(value));
        return static_cast<I>(res);
    }
    i64 res;
    asm("fcvt.l.s %0, %1"
        : "=r"(res)
        : "f"(value));
    return static_cast<I>(res);
}

template<SignedIntegral I>
ALWAYS_INLINE I round_to_impl(double value)
{
    if constexpr (sizeof(I) <= sizeof(u32)) {
        i32 res;
        asm("fcvt.w.d %w0, %1"
            : "=r"(res)
            : "f"(value));
        return static_cast<I>(res);
    }
    i64 res;
    asm("fcvt.l.d %0, %1"
        : "=r"(res)
        : "f"(value));
    return static_cast<I>(res);
}

template<UnsignedIntegral U>
ALWAYS_INLINE U round_to_impl(float value)
{
    if constexpr (sizeof(U) <= sizeof(u32)) {
        u32 res;
        asm("fcvt.wu.s %w0, %1"
            : "=r"(res)
            : "f"(value));
        return static_cast<U>(res);
    }
    u64 res;
    asm("fcvt.lu.s %0, %1"
        : "=r"(res)
        : "f"(value));
    return static_cast<U>(res);
}

template<UnsignedIntegral U>
ALWAYS_INLINE U round_to_impl(double value)
{
    if constexpr (sizeof(U) <= sizeof(u32)) {
        u32 res;
        asm("fcvt.wu.d %w0, %1"
            : "=r"(res)
            : "f"(value));
        return static_cast<U>(res);
    }
    u64 res;
    asm("fcvt.lu.d %0, %1"
        : "=r"(res)
        : "f"(value));
    return static_cast<U>(res);
}

#endif

template<Integral I, FloatingPoint P>
constexpr I round_to(P value)
{
    if (is_constant_evaluated()) {
        return static_cast<I>(rint<P>(value));
    }
    if constexpr (requires { round_to_impl<I>(value); }) {
        return round_to_impl<I>(value);
    } else {
        // Naive Implementation
        // Most HW provides a smarter way of doing this
        return static_cast<I>(rint<P>(value));
        // FIXME: Is there any reason to call into libm instead?
    }
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
