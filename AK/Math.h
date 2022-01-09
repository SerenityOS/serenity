/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BuiltinWrappers.h>
#include <AK/Concepts.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace AK {

template<FloatingPoint T>
constexpr T NaN = __builtin_nan("");
template<FloatingPoint T>
constexpr T Pi = 3.141592653589793238462643383279502884L;
template<FloatingPoint T>
constexpr T E = 2.718281828459045235360287471352662498L;

namespace Details {
#if ARCH(AARCH64)
template<size_t>
constexpr double e_to_power();
template<>
constexpr double e_to_power<0>() { return 1; }
template<size_t exponent>
constexpr double e_to_power() { return E<double> * e_to_power<exponent - 1>(); }

template<size_t>
constexpr size_t factorial();
template<>
constexpr size_t factorial<0>() { return 1; }
template<size_t value>
constexpr size_t factorial() { return value * factorial<value - 1>(); }
#endif

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

#define CONSTEXPR_STATE(function, args...)        \
    if (is_constant_evaluated()) {                \
        if (IsSame<T, long double>)               \
            return __builtin_##function##l(args); \
        if (IsSame<T, double>)                    \
            return __builtin_##function(args);    \
        if (IsSame<T, float>)                     \
            return __builtin_##function##f(args); \
    }

namespace Division {
template<FloatingPoint T>
constexpr T fmod(T x, T y)
{
    CONSTEXPR_STATE(fmod, x, y);
#if ARCH(AARCH64)
    return x - static_cast<int64_t>(x / y) * y;
#else
    u16 fpu_status;
    do {
        asm(
            "fprem\n"
            "fnstsw %%ax\n"
            : "+t"(x), "=a"(fpu_status)
            : "u"(y));
    } while (fpu_status & 0x400);
    return x;
#endif
}
template<FloatingPoint T>
constexpr T remainder(T x, T y)
{
    CONSTEXPR_STATE(remainder, x, y);
#if ARCH(AARCH64)
    T m = fmod(x, y);
    return m > y / 2 ? m : y - m;
#else
    u16 fpu_status;
    do {
        asm(
            "fprem1\n"
            "fnstsw %%ax\n"
            : "+t"(x), "=a"(fpu_status)
            : "u"(y));
    } while (fpu_status & 0x400);
    return x;
#endif
}
}

using Division::fmod;
using Division::remainder;

template<FloatingPoint T>
constexpr T sqrt(T x)
{
    CONSTEXPR_STATE(sqrt, x);
#if ARCH(AARCH64)
    if constexpr (IsSame<T, float>)
        asm volatile ("fsqrt s0, s0\n ret");
    asm volatile ("fsqrt d0, d0\n ret");
    return 0; // Won't be reached, but compiler doesn't see the "ret" in the assembly.
#else
    T res;
    asm("fsqrt"
        : "=t"(res)
        : "0"(x));
    return res;
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

template<FloatingPoint T>
constexpr T fabs(T x)
{
#if ARCH(AARCH64)
    return x < 0 ? -x : x;
#else
    if (is_constant_evaluated())
        return x < 0 ? -x : x;
    asm(
        "fabs"
        : "+t"(x));
    return x;
#endif
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
#if ARCH(AARCH64)
    if (angle < 0)
        angle = -1 * angle;
    angle = fmod(angle, 2 * Pi<T>);
    if (angle > Pi<T>)
        return -1 * sin(angle - Pi<T>);
    if (angle > Pi<T> / 2)
        return sin(Pi<T> - angle);

    T angle2 = angle * angle;
    T angle3 = angle * angle2;
    T angle5 = angle3 * angle2;
    T angle7 = angle5 * angle2;
    return angle - angle3 / 3 / 2 + angle5 / 5 / 4 / 3 / 2 - angle7 / 7 / 6 / 5 / 4 / 3 / 2;
#else
    T ret;
    asm(
        "fsin"
        : "=t"(ret)
        : "0"(angle));
    return ret;
#endif
}

template<FloatingPoint T>
constexpr T cos(T angle)
{
    CONSTEXPR_STATE(cos, angle);
#if ARCH(AARCH64)
    return sin(Pi<T> / 2 - angle);
#else
    T ret;
    asm(
        "fcos"
        : "=t"(ret)
        : "0"(angle));
    return ret;
#endif
}

template<FloatingPoint T>
constexpr T tan(T angle)
{
    CONSTEXPR_STATE(tan, angle);
#if ARCH(AARCH64)
    return sin(angle) / cos(angle);
#else
    double ret, one;
    asm(
        "fptan"
        : "=t"(one), "=u"(ret)
        : "0"(angle));

    return ret;
#endif
}

template<FloatingPoint T>
constexpr T atan(T value)
{
    CONSTEXPR_STATE(atan, value);

#if ARCH(AARCH64)
    if (value < 0)
        return -atan(-value);
    if (value > 1)
        return Pi<T> / 2 - atan(1 / value);
    T squared = value * value;
    return value /
        (1 + 1 * 1 * squared /
        (3 + 2 * 2 * squared /
        (5 + 3 * 3 * squared /
        (7 + 4 * 4 * squared /
        (9 + 5 * 5 * squared /
        (11 + 6 * 6 * squared /
        (13 + 7 * 7 * squared
        )))))));
#else
    T ret;
    asm(
        "fld1\n"
        "fpatan\n"
        : "=t"(ret)
        : "0"(value));
    return ret;
#endif
}

template<FloatingPoint T>
constexpr T asin(T x)
{
    CONSTEXPR_STATE(asin, x);
    if (x > 1 || x < -1)
        return NaN<T>;
    if (x > (T)0.5 || x < (T)-0.5)
        return 2 * atan<T>(x / (1 + sqrt<T>(1 - x * x)));
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
constexpr T atan2(T y, T x)
{
    CONSTEXPR_STATE(atan2, y, x);
#if ARCH(AARCH64)
    if (x > 0)
        return atan(y / x);
    if (x == 0) {
        if (y > 0)
            return Pi<T> / 2;
        if (y < 0)
            return -Pi<T> / 2;
        return 0;
    }
    if (y >= 0)
        return atan(y / x) + Pi<T>;
    return atan(y / x) - Pi<T>;
#else
    T ret;
    asm("fpatan"
        : "=t"(ret)
        : "0"(x), "u"(y)
        : "st(1)");
    return ret;
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
using Trigonometry::tan;

namespace Exponentials {

template<FloatingPoint T>
constexpr T exp(T exponent)
{
    CONSTEXPR_STATE(exp, exponent);
#if ARCH(AARCH64)
    double result = 1;
    if (exponent >= 1) {
        size_t integer_part = (size_t)exponent;
        if (integer_part & 1)
            result *= Details::e_to_power<1>();
        if (integer_part & 2)
            result *= Details::e_to_power<2>();
        if (integer_part > 3) {
            if (integer_part & 4)
                result *= Details::e_to_power<4>();
            if (integer_part & 8)
                result *= Details::e_to_power<8>();
            if (integer_part & 16)
                result *= Details::e_to_power<16>();
            if (integer_part & 32)
                result *= Details::e_to_power<32>();
            if (integer_part >= 64)
                return __builtin_huge_val();
        }
        exponent -= integer_part;
    } else if (exponent < 0)
        return 1 / exp(-exponent);
    double taylor_series_result = 1 + exponent;
    double taylor_series_numerator = exponent * exponent;
    taylor_series_result += taylor_series_numerator / Details::factorial<2>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / Details::factorial<3>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / Details::factorial<4>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / Details::factorial<5>();
    return result * taylor_series_result;
#else
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
#endif
}

template<FloatingPoint T>
constexpr T log(T x)
{
    CONSTEXPR_STATE(log, x);
#if ARCH(AARCH64)
    if (x < 0)
        return NaN<T>;
    if (x == 0)
        return -__builtin_huge_val();
    double y = 1 + 2 * (x - 1) / (x + 1);
    double exponentiated = exp(y);
    y = y + 2 * (x - exponentiated) / (x + exponentiated);
    exponentiated = exp(y);
    y = y + 2 * (x - exponentiated) / (x + exponentiated);
    exponentiated = exp(y);
    return y + 2 * (x - exponentiated) / (x + exponentiated);
#else
    T ret;
    asm(
        "fldln2\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
#endif
}

template<FloatingPoint T>
constexpr T log2(T x)
{
    CONSTEXPR_STATE(log2, x);
#if ARCH(AARCH64)
    return log(x) / log(2.);
#else
    T ret;
    asm(
        "fld1\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
#endif
}

template<Integral T>
constexpr T log2(T x)
{
    return x ? 8 * sizeof(T) - count_leading_zeroes(static_cast<MakeUnsigned<T>>(x)) : 0;
}

template<FloatingPoint T>
constexpr T log10(T x)
{
    CONSTEXPR_STATE(log10, x);
#if ARCH(AARCH64)
    return log(x) / log(10.);
#else
    T ret;
    asm(
        "fldlg2\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
#endif
    return x;
}

template<FloatingPoint T>
constexpr T exp2(T exponent)
{
    CONSTEXPR_STATE(exp2, exponent);
#if ARCH(AARCH64)
    // FIXME: This is incorrect but somewhat close.
    // It should be done in assembly anyways.
    return exp(exponent);
#else
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
#endif
}
template<Integral T>
constexpr T exp2(T exponent)
{
    return 1u << exponent;
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

template<FloatingPoint T>
constexpr T pow(T x, T y)
{
    CONSTEXPR_STATE(pow, x, y);
    // fixme I am naive
    if (__builtin_isnan(y))
        return y;
    if (y == 0)
        return 1;
    if (x == 0)
        return 0;
    if (y == 1)
        return x;
    int y_as_int = (int)y;
    if (y == (T)y_as_int) {
        T result = x;
        for (int i = 0; i < fabs<T>(y) - 1; ++i)
            result *= x;
        if (y < 0)
            result = 1.0l / result;
        return result;
    }

    return exp2<T>(y * log2<T>(x));
}

#undef CONSTEXPR_STATE

}
