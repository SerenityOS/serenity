/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BuiltinWrappers.h>
#include <AK/Concepts.h>
#include <AK/FloatingPoint.h>
#include <AK/NumericLimits.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>
#include <math.h>

#ifdef KERNEL
#    error "Including AK/Math.h from the Kernel is never correct! Floating point is disabled."
#endif

namespace AK {

template<FloatingPoint T>
constexpr T NaN = __builtin_nan("");
template<FloatingPoint T>
constexpr T Infinity = __builtin_huge_vall();
template<FloatingPoint T>
constexpr T Pi = 3.141592653589793238462643383279502884L;
template<FloatingPoint T>
constexpr T E = 2.718281828459045235360287471352662498L;
template<FloatingPoint T>
constexpr T Sqrt2 = 1.414213562373095048801688724209698079L;
template<FloatingPoint T>
constexpr T Sqrt1_2 = 0.707106781186547524400844362104849039L;

template<FloatingPoint T>
constexpr T L2_10 = 3.321928094887362347870319429489390175864L;
template<FloatingPoint T>
constexpr T L2_E = 1.442695040888963407359924681001892137L;

namespace Details {
template<size_t>
constexpr size_t product_even();
template<>
constexpr size_t product_even<2>() { return 2; }
template<size_t value>
constexpr size_t product_even() { return value * product_even<value - 2>(); }

template<size_t>
constexpr size_t product_odd();
template<>
constexpr size_t product_odd<1>() { return 1; }
template<size_t value>
constexpr size_t product_odd() { return value * product_odd<value - 2>(); }
}

template<FloatingPoint T>
constexpr T to_radians(T degrees)
{
    return degrees * AK::Pi<T> / 180;
}

template<FloatingPoint T>
constexpr T to_degrees(T radians)
{
    return radians * 180 / AK::Pi<T>;
}

template<FloatingPoint FloatT>
FloatT copysign(FloatT x, FloatT y)
{
    using Extractor = FloatExtractor<FloatT>;
    auto ex = Extractor::from_float(x);
    auto ey = Extractor::from_float(y);
    ex.sign = ey.sign;
    return ex.to_float();
}

#define CONSTEXPR_STATE(function, args...)        \
    if (is_constant_evaluated()) {                \
        if (IsSame<T, long double>)               \
            return __builtin_##function##l(args); \
        if (IsSame<T, double>)                    \
            return __builtin_##function(args);    \
        if (IsSame<T, float>)                     \
            return __builtin_##function##f(args); \
    }

#define AARCH64_INSTRUCTION(instruction, arg) \
    if constexpr (IsSame<T, long double>)     \
        TODO();                               \
    if constexpr (IsSame<T, double>) {        \
        double res;                           \
        asm(#instruction " %d0, %d1"          \
            : "=w"(res)                       \
            : "w"(arg));                      \
        return res;                           \
    }                                         \
    if constexpr (IsSame<T, float>) {         \
        float res;                            \
        asm(#instruction " %s0, %s1"          \
            : "=w"(res)                       \
            : "w"(arg));                      \
        return res;                           \
    }

template<FloatingPoint T>
constexpr T fabs(T x)
{
    // Both GCC and Clang inline fabs by default, so this is just a cmath like wrapper
    if constexpr (IsSame<T, long double>)
        return __builtin_fabsl(x);
    if constexpr (IsSame<T, double>)
        return __builtin_fabs(x);
    if constexpr (IsSame<T, float>)
        return __builtin_fabsf(x);
}

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
    if constexpr (IsSame<T, long double>)
        return __builtin_ceill(num);
    if constexpr (IsSame<T, double>)
        return __builtin_ceil(num);
    if constexpr (IsSame<T, float>)
        return __builtin_ceilf(num);
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
    if constexpr (IsSame<T, long double>)
        return __builtin_floorl(num);
    if constexpr (IsSame<T, double>)
        return __builtin_floor(num);
    if constexpr (IsSame<T, float>)
        return __builtin_floorf(num);
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
#    else
    asm(
        "frndint"
        : "+t"(x));
    return x;
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
    TODO();
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
ALWAYS_INLINE I round_to(P value);

#if ARCH(X86_64)
template<Integral I>
ALWAYS_INLINE I round_to(long double value)
{
    // Note: fistps outputs into a signed integer location (i16, i32, i64),
    //       so lets be nice and tell the compiler that.
    Conditional<sizeof(I) >= sizeof(i16), MakeSigned<I>, i16> ret;
    if constexpr (sizeof(I) == sizeof(i64)) {
        asm("fistpll %0"
            : "=m"(ret)
            : "t"(value)
            : "st");
    } else if constexpr (sizeof(I) == sizeof(i32)) {
        asm("fistpl %0"
            : "=m"(ret)
            : "t"(value)
            : "st");
    } else {
        asm("fistps %0"
            : "=m"(ret)
            : "t"(value)
            : "st");
    }
    return static_cast<I>(ret);
}

template<Integral I>
ALWAYS_INLINE I round_to(float value)
{
    // FIXME: round_to<u64> might will cause issues, aka the indefinite value being set,
    //        if the value surpasses the i64 limit, even if the result could fit into an u64
    //        To solve this we would either need to detect that value or do a range check and
    //        then do a more specialized conversion, which might include a division (which is expensive)
    if constexpr (sizeof(I) == sizeof(i64) || IsSame<I, u32>) {
        i64 ret;
        asm("cvtss2si %1, %0"
            : "=r"(ret)
            : "xm"(value));
        return static_cast<I>(ret);
    }
    i32 ret;
    asm("cvtss2si %1, %0"
        : "=r"(ret)
        : "xm"(value));
    return static_cast<I>(ret);
}

template<Integral I>
ALWAYS_INLINE I round_to(double value)
{
    // FIXME: round_to<u64> might will cause issues, aka the indefinite value being set,
    //        if the value surpasses the i64 limit, even if the result could fit into an u64
    //        To solve this we would either need to detect that value or do a range check and
    //        then do a more specialized conversion, which might include a division (which is expensive)
    if constexpr (sizeof(I) == sizeof(i64) || IsSame<I, u32>) {
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
template<Signed I>
ALWAYS_INLINE I round_to(float value)
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

template<Signed I>
ALWAYS_INLINE I round_to(double value)
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

template<Unsigned U>
ALWAYS_INLINE U round_to(float value)
{
    if constexpr (sizeof(U) <= sizeof(u32)) {
        u32 res;
        asm("fcvtnu %w0, %s1"
            : "=r"(res)
            : "w"(value));
        return static_cast<U>(res);
    }
    i64 res;
    asm("fcvtnu %0, %s1"
        : "=r"(res)
        : "w"(value));
    return static_cast<U>(res);
}

template<Unsigned U>
ALWAYS_INLINE U round_to(double value)
{
    if constexpr (sizeof(U) <= sizeof(u32)) {
        u32 res;
        asm("fcvtns %w0, %d1"
            : "=r"(res)
            : "w"(value));
        return static_cast<U>(res);
    }
    i64 res;
    asm("fcvtns %0, %d1"
        : "=r"(res)
        : "w"(value));
    return static_cast<U>(res);
}

#else
template<Integral I, FloatingPoint P>
ALWAYS_INLINE I round_to(P value)
{
    if constexpr (IsSame<P, long double>)
        return static_cast<I>(__builtin_llrintl(value));
    if constexpr (IsSame<P, double>)
        return static_cast<I>(__builtin_llrint(value));
    if constexpr (IsSame<P, float>)
        return static_cast<I>(__builtin_llrintf(value));
}
#endif

}

using Rounding::ceil;
using Rounding::floor;
using Rounding::rint;
using Rounding::round;
using Rounding::round_to;
using Rounding::trunc;

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
    if constexpr (IsSame<T, long double>)
        return __builtin_fmodl(x, y);
    if constexpr (IsSame<T, double>)
        return __builtin_fmod(x, y);
    if constexpr (IsSame<T, float>)
        return __builtin_fmodf(x, y);
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
    if constexpr (IsSame<T, long double>)
        return __builtin_remainderl(x, y);
    if constexpr (IsSame<T, double>)
        return __builtin_remainder(x, y);
    if constexpr (IsSame<T, float>)
        return __builtin_remainderf(x, y);
#endif
}
}

using Division::fmod;
using Division::remainder;

template<FloatingPoint T>
constexpr T sqrt(T x)
{
    CONSTEXPR_STATE(sqrt, x);

#if ARCH(X86_64)
    if constexpr (IsSame<T, float>) {
        float res;
        asm("sqrtss %1, %0"
            : "=x"(res)
            : "x"(x));
        return res;
    }
    if constexpr (IsSame<T, double>) {
        double res;
        asm("sqrtsd %1, %0"
            : "=x"(res)
            : "x"(x));
        return res;
    }
    T res;
    asm("fsqrt"
        : "=t"(res)
        : "0"(x));
    return res;
#elif ARCH(AARCH64)
    AARCH64_INSTRUCTION(fsqrt, x);
#elif ARCH(RISCV64)
    if constexpr (IsSame<T, float>) {
        float res;
        asm("fsqrt.s %0, %1"
            : "=f"(res)
            : "f"(x));
        return res;
    }
    if constexpr (IsSame<T, double>) {
        double res;
        asm("fsqrt.d %0, %1"
            : "=f"(res)
            : "f"(x));
        return res;
    }
    if constexpr (IsSame<T, long double>)
        TODO_RISCV64();
#else
#    if defined(AK_OS_SERENITY)
    // TODO: Add implementation for this function.
    TODO();
#    endif
    return __builtin_sqrt(x);
#endif
}

template<FloatingPoint T>
constexpr T cbrt(T x)
{
    CONSTEXPR_STATE(cbrt, x);
    if (__builtin_isinf(x) || x == 0)
        return x;
    if (x < 0)
        return -cbrt(-x);

    T r = x;
    T ex = 0;

    while (r < 0.125l) {
        r *= 8;
        ex--;
    }
    while (r > 1.0l) {
        r *= 0.125l;
        ex++;
    }

    r = (-0.46946116l * r + 1.072302l) * r + 0.3812513l;

    while (ex < 0) {
        r *= 0.5l;
        ex++;
    }
    while (ex > 0) {
        r *= 2.0l;
        ex--;
    }

    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);
    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);
    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);
    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);

    return r;
}

namespace Trigonometry {

template<FloatingPoint T>
constexpr T hypot(T x, T y)
{
    return sqrt(x * x + y * y);
}

template<FloatingPoint T>
constexpr T sin(T angle)
{
    CONSTEXPR_STATE(sin, angle);

#if ARCH(X86_64)
    T ret;
    asm(
        "fsin"
        : "=t"(ret)
        : "0"(angle));
    return ret;
#else
#    if defined(AK_OS_SERENITY)
    if (angle < 0)
        return -sin(-angle);

    angle = fmod(angle, 2 * Pi<T>);

    if (angle >= Pi<T>)
        return -sin(angle - Pi<T>);

    if (angle > Pi<T> / 2)
        return sin(Pi<T> - angle);

    // https://en.wikipedia.org/wiki/Bh%C4%81skara_I%27s_sine_approximation_formula
    // FIXME: This is not a good formula! It requires divisions, so it's slow, and it's not very accurate either.
    return 16 * angle * (Pi<T> - angle) / (5 * Pi<T> * Pi<T> - 4 * angle * (Pi<T> - angle));
#    else
    return __builtin_sin(angle);
#    endif
#endif
}

template<FloatingPoint T>
constexpr T cos(T angle)
{
    CONSTEXPR_STATE(cos, angle);

#if ARCH(X86_64)
    T ret;
    asm(
        "fcos"
        : "=t"(ret)
        : "0"(angle));
    return ret;
#else
#    if defined(AK_OS_SERENITY)
    if (angle < 0)
        return cos(-angle);

    angle = fmod(angle, 2 * Pi<T>);

    if (angle >= Pi<T>)
        return -cos(angle - Pi<T>);

    if (angle > Pi<T> / 2)
        return -cos(Pi<T> - angle);

    // https://en.wikipedia.org/wiki/Bh%C4%81skara_I%27s_sine_approximation_formula
    // FIXME: This is not a good formula! It requires divisions, so it's slow, and it's not very accurate either.
    return (Pi<T> * Pi<T> - 4 * angle * angle) / (Pi<T> * Pi<T> + angle * angle);
#    else
    return __builtin_cos(angle);
#    endif
#endif
}

template<FloatingPoint T>
constexpr void sincos(T angle, T& sin_val, T& cos_val)
{
    if (is_constant_evaluated()) {
        sin_val = sin(angle);
        cos_val = cos(angle);
        return;
    }
#if ARCH(X86_64)
    asm(
        "fsincos"
        : "=t"(cos_val), "=u"(sin_val)
        : "0"(angle));
#else
    sin_val = sin(angle);
    cos_val = cos(angle);
#endif
}

template<FloatingPoint T>
constexpr T tan(T angle)
{
    CONSTEXPR_STATE(tan, angle);

#if ARCH(X86_64)
    T ret, one;
    asm(
        "fptan"
        : "=t"(one), "=u"(ret)
        : "0"(angle));

    return ret;
#else
#    if defined(AK_OS_SERENITY)
    return sin(angle) / cos(angle);
#    else
    return __builtin_tan(angle);
#    endif
#endif
}

template<FloatingPoint T>
constexpr T asin(T x)
{
    CONSTEXPR_STATE(asin, x);

    if (x < 0)
        return -asin(-x);

    if (x > 1)
        return NaN<T>;

    if (x > (T)0.5) {
        // asin(x) = pi/2 - 2 * asin(sqrt((1 - x) / 2))
        // If 0.5 < x <= 1, then sqrt((1 - x) / 2 ) < 0.5,
        // and the recursion will go down the `<= 0.5` branch.
        return Pi<T> / 2 - 2 * asin(sqrt((1 - x) / 2));
    }

    T squared = x * x;
    T value = x;
    T i = x * squared;
    value += i * Details::product_odd<1>() / Details::product_even<2>() / 3;
    i *= squared;
    value += i * Details::product_odd<3>() / Details::product_even<4>() / 5;
    i *= squared;
    value += i * Details::product_odd<5>() / Details::product_even<6>() / 7;
    i *= squared;
    value += i * Details::product_odd<7>() / Details::product_even<8>() / 9;
    i *= squared;
    value += i * Details::product_odd<9>() / Details::product_even<10>() / 11;
    i *= squared;
    value += i * Details::product_odd<11>() / Details::product_even<12>() / 13;
    i *= squared;
    value += i * Details::product_odd<13>() / Details::product_even<14>() / 15;
    i *= squared;
    value += i * Details::product_odd<15>() / Details::product_even<16>() / 17;
    return value;
}

template<FloatingPoint T>
constexpr T acos(T value)
{
    CONSTEXPR_STATE(acos, value);

    // FIXME: I am naive
    return static_cast<T>(0.5) * Pi<T> - asin<T>(value);
}

template<FloatingPoint T>
constexpr T atan(T value)
{
    CONSTEXPR_STATE(atan, value);

#if ARCH(X86_64)
    T ret;
    asm(
        "fld1\n"
        "fpatan\n"
        : "=t"(ret)
        : "0"(value));
    return ret;
#else
#    if defined(AK_OS_SERENITY)
    return asin(value / sqrt(1 + value * value));
#    endif
    return __builtin_atan(value);
#endif
}

template<FloatingPoint T>
constexpr T atan2(T y, T x)
{
    CONSTEXPR_STATE(atan2, y, x);

#if ARCH(X86_64)
    T ret;
    asm("fpatan"
        : "=t"(ret)
        : "0"(x), "u"(y)
        : "st(1)");
    return ret;
#else
#    if defined(AK_OS_SERENITY)
    if (__builtin_isnan(y))
        return y;
    if (__builtin_isnan(x))
        return x;

    // SPECIAL VALUES
    //      atan2(±0, -0) returns ±pi.
    if (y == 0 && x == 0 && signbit(x))
        return copysign(Pi<T>, y);

    //      atan2(±0, +0) returns ±0.
    if (y == 0 && x == 0 && !signbit(x))
        return y;

    //      atan2(±0, x) returns ±pi for x < 0.
    if (y == 0 && x < 0)
        return copysign(Pi<T>, y);

    //      atan2(±0, x) returns ±0 for x > 0.
    if (y == 0 && x > 0)
        return y;

    //      atan2(y, ±0) returns +pi/2 for y > 0.
    if (y > 0 && x == 0)
        return Pi<T> / 2;

    //      atan2(y, ±0) returns -pi/2 for y < 0.
    if (y < 0 && x == 0)
        return -Pi<T> / 2;

    //      atan2(±y, -infinity) returns ±pi for finite y > 0.
    if (!__builtin_isinf(y) && y > 0 && __builtin_isinf(x) && signbit(x))
        return copysign(Pi<T>, y);

    //      atan2(±y, +infinity) returns ±0 for finite y > 0.
    if (!__builtin_isinf(y) && y > 0 && __builtin_isinf(x) && !signbit(x))
        return copysign(static_cast<T>(0), y);

    //      atan2(±infinity, x) returns ±pi/2 for finite x.
    if (__builtin_isinf(y) && !__builtin_isinf(x))
        return copysign(Pi<T> / 2, y);

    //      atan2(±infinity, -infinity) returns ±3*pi/4.
    if (__builtin_isinf(y) && __builtin_isinf(x) && signbit(x))
        return copysign(3 * Pi<T> / 4, y);

    //      atan2(±infinity, +infinity) returns ±pi/4.
    if (__builtin_isinf(y) && __builtin_isinf(x) && !signbit(x))
        return copysign(Pi<T> / 4, y);

    // Check quadrant, going counterclockwise.
    if (y > 0 && x > 0)
        return atan(y / x);
    if (y > 0 && x < 0)
        return atan(y / x) + Pi<T>;
    if (y < 0 && x < 0)
        return atan(y / x) - Pi<T>;
    // y < 0 && x > 0
    return atan(y / x);
#    else
    return __builtin_atan2(y, x);
#    endif
#endif
}

}

using Trigonometry::acos;
using Trigonometry::asin;
using Trigonometry::atan;
using Trigonometry::atan2;
using Trigonometry::cos;
using Trigonometry::hypot;
using Trigonometry::sin;
using Trigonometry::sincos;
using Trigonometry::tan;

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

    auto ext = FloatExtractor<T>::from_float(x);
    T exponent = ext.exponent - FloatExtractor<T>::exponent_bias;

    // When the mantissa shows 0b00 (implicitly 1.0) we are on a power of 2
    if (ext.mantissa == 0)
        return exponent;

    // FIXME: Handle denormalized numbers separately

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
    return __builtin_log(x);
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
    return __builtin_log10(x);
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

namespace Hyperbolic {

template<FloatingPoint T>
constexpr T sinh(T x)
{
    T exponentiated = exp<T>(x);
    if (x > 0)
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    return (exponentiated - 1 / exponentiated) / 2;
}

template<FloatingPoint T>
constexpr T cosh(T x)
{
    CONSTEXPR_STATE(cosh, x);

    T exponentiated = exp(-x);
    if (x < 0)
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    return (1 / exponentiated + exponentiated) / 2;
}

template<FloatingPoint T>
constexpr T tanh(T x)
{
    if (x > 0) {
        T exponentiated = exp<T>(2 * x);
        return (exponentiated - 1) / (exponentiated + 1);
    }
    T plusX = exp<T>(x);
    T minusX = 1 / plusX;
    return (plusX - minusX) / (plusX + minusX);
}

template<FloatingPoint T>
constexpr T asinh(T x)
{
    return log<T>(x + sqrt<T>(x * x + 1));
}

template<FloatingPoint T>
constexpr T acosh(T x)
{
    return log<T>(x + sqrt<T>(x * x - 1));
}

template<FloatingPoint T>
constexpr T atanh(T x)
{
    return log<T>((1 + x) / (1 - x)) / (T)2.0l;
}

}

using Hyperbolic::acosh;
using Hyperbolic::asinh;
using Hyperbolic::atanh;
using Hyperbolic::cosh;
using Hyperbolic::sinh;
using Hyperbolic::tanh;

// Calculate x^y with fast exponentiation when the power is a natural number.
template<FloatingPoint F, Unsigned U>
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

#undef CONSTEXPR_STATE
#undef AARCH64_INSTRUCTION
}

#if USING_AK_GLOBALLY
using AK::round_to;
#endif
