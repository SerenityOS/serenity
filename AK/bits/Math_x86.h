/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>
#include <AK/bits/Math_common.h>
#include <LibM/math_defines.h>
#include <fenv.h>

#if !(ARCH(I386) || ARCH(X86_64))
#    error Unsupported Architecture
#endif

// FIXME: Some of these can be implemented using SSE, which might be faster on
//        SSE platforms, especially x86_64
namespace AK {

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

namespace Rounding {

#define ROUNDING_FUNCTION(name, direction)                                     \
    template<FloatingPoint T>                                                  \
    MATH_CONSTEXPR T name(T value)                                             \
    {                                                                          \
        CONSTEXPR_STATE(name, value);                                          \
        /* FIXME: Tell the compiler to optimize out repeated cw changes*/      \
        /*        We aren't using fenv for that now because we only want to */ \
        /*        touch the fpu, maybe that would change something*/           \
        u16 cw;                                                                \
        u16 cw2;                                                               \
        /* save controll word */                                               \
        asm("fnstcw %0"                                                        \
            : "=m"(cw));                                                       \
        /* set the correct rounding direction */                               \
        cw2 = cw & ~(3 << 10);                                                 \
        cw2 |= direction;                                                      \
                                                                               \
        asm("fldcw %1\n"                                                       \
            "frndint\n"                                                        \
            "fldcw %2"                                                         \
            : "+t"(value)                                                      \
            : "m"(cw2), "m"(cw));                                              \
                                                                               \
        return value;                                                          \
    }
#define ROUNDING_FUNCTION_TO_INT(name, direction)                                                                                      \
    template<Signed I = int, FloatingPoint T>                                                                                          \
    MATH_CONSTEXPR I name##_to_int(T value) requires(sizeof(I) == sizeof(i16) || sizeof(I) == sizeof(i32) || sizeof(I) == sizeof(i64)) \
    {                                                                                                                                  \
        /* FIXME: use int builtins if available (lround)?*/                                                                            \
        CONSTEXPR_STATE(name, value);                                                                                                  \
        /* FIXME: Tell the compiler to join repeated cw changes*/                                                                      \
        u16 cw;                                                                                                                        \
        u16 cw2;                                                                                                                       \
        /* save controll word */                                                                                                       \
        asm("fnstcw %0"                                                                                                                \
            : "=m"(cw));                                                                                                               \
        /* set the correct rounding direction */                                                                                       \
        cw2 = cw & ~(3 << 10);                                                                                                         \
        cw2 |= direction << 10;                                                                                                        \
        asm("fldcw %0" ::"m"(cw2));                                                                                                    \
        /* do the actual rounding */                                                                                                   \
        I res;                                                                                                                         \
        if constexpr (sizeof(I) == sizeof(i64)) {                                                                                      \
            asm("fistpq %0"                                                                                                            \
                : "=m"(res)                                                                                                            \
                : "t"(value)                                                                                                           \
                : "st");                                                                                                               \
        }                                                                                                                              \
        if constexpr (sizeof(I) == sizeof(i32)) {                                                                                      \
            asm("fistpl %0"                                                                                                            \
                : "=m"(res)                                                                                                            \
                : "t"(value)                                                                                                           \
                : "st");                                                                                                               \
        }                                                                                                                              \
        if constexpr (sizeof(I) == sizeof(i16)) {                                                                                      \
            asm("fistps %0"                                                                                                            \
                : "=m"(res)                                                                                                            \
                : "t"(value)                                                                                                           \
                : "st");                                                                                                               \
        }                                                                                                                              \
        /* restore the controll word*/                                                                                                 \
        asm("fldcw %0" ::"m"(cw));                                                                                                     \
        return res;                                                                                                                    \
    }

ROUNDING_FUNCTION(floor, FE_DOWNWARD);
ROUNDING_FUNCTION_TO_INT(floor, FE_DOWNWARD);
ROUNDING_FUNCTION(round, FE_TONEAREST);
ROUNDING_FUNCTION_TO_INT(round, FE_TONEAREST);
ROUNDING_FUNCTION(ceil, FE_UPWARD);
ROUNDING_FUNCTION_TO_INT(ceil, FE_UPWARD);
ROUNDING_FUNCTION(trunc, FE_TOWARDZERO);

template<Signed I = int, FloatingPoint T>
MATH_CONSTEXPR I trunc_to_int(T value) requires(sizeof(I) == sizeof(i16) || sizeof(I) == sizeof(i32) || sizeof(I) == sizeof(i64))
{
    CONSTEXPR_STATE(trunc, value);
    I res;
    if constexpr (sizeof(I) == sizeof(i64)) {
        asm("fisttpq %0"
            : "=m"(res)
            : "t"(value)
            : "st");
    }
    if constexpr (sizeof(I) == sizeof(i32)) {
        asm("fisttpl %0"
            : "=m"(res)
            : "t"(value)
            : "st");
    }
    if constexpr (sizeof(I) == sizeof(i16)) {
        asm("fisttps %0"
            : "=m"(res)
            : "t"(value)
            : "st");
    }
    return res;
}

template<FloatingPoint T>
MATH_CONSTEXPR T fast_round(T value)
{
    asm("frndint"
        : "+t"(value));
    return value;
}

template<Signed I = int, FloatingPoint T>
MATH_CONSTEXPR I fast_round_to_int(T value) requires(sizeof(I) == sizeof(i16) || sizeof(I) == sizeof(i32) || sizeof(I) == sizeof(i64))
{
    I res;
    if constexpr (sizeof(I) == sizeof(i64)) {
        asm("fistpq %0"
            : "=m"(res)
            : "t"(value)
            : "st");
    }
    if constexpr (sizeof(I) == sizeof(i32)) {
        asm("fistpl %0"
            : "=m"(res)
            : "t"(value)
            : "st");
    }
    if constexpr (sizeof(I) == sizeof(i16)) {
        asm("fistps %0"
            : "=m"(res)
            : "t"(value)
            : "st");
    }
    return res;
}

#undef ROUNDING_FUNCTION
#undef ROUNDING_FUNCTION_TO_INT
}

using Rounding::ceil;
using Rounding::ceil_to_int;
using Rounding::fast_round;
using Rounding::fast_round_to_int;
using Rounding::floor;
using Rounding::floor_to_int;
using Rounding::round;
using Rounding::round_to_int;
using Rounding::trunc;
using Rounding::trunc_to_int;

namespace Division {
template<FloatingPoint T>
MATH_CONSTEXPR T fmod(T x, T y)
{
    CONSTEXPR_STATE(fmod, x, y);
    T res;
    asm(
        "fprem"
        : "=t"(res)
        : "0"(x), "u"(y));
    return res;
}
template<FloatingPoint T>
MATH_CONSTEXPR T modf(T val, T& int_part)
{
    // FIXME: CONSTEXPR_STATE
    int_part = trunc(val);
    T fraction = val - int_part;
    // the sign should already be the same
    // trunc rounds towards 0
    // if i is less than 0 than {i <= trunc(i)}
    //                      and {trunc(i) <= 0}
    // therefore sign(i-trunc(i)) = sign(i)
    return fraction;
}

template<FloatingPoint T>
MATH_CONSTEXPR T remainder(T x, T y)
{
    CONSTEXPR_STATE(remainder, x, y);
    T res;
    asm(
        "fprem1"
        : "=t"(res)
        : "0"(x), "u"(y));
    return res;
}
}

using Division::fmod;
using Division::modf;
using Division::remainder;

template<FloatingPoint T>
MATH_CONSTEXPR T sqrt(T x)
{
    CONSTEXPR_STATE(sqrt, x);
    T res;
    asm("fsqrt"
        : "=t"(res)
        : "0"(x));
    return res;
}

template<FloatingPoint T>
MATH_CONSTEXPR T cbrt(T x)
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
    if (is_constant_evaluated())
        return x < 0 ? -x : x;
    asm(
        "fabs"
        : "+t"(x));
    return x;
}

namespace Trigonometry {

template<FloatingPoint T>
MATH_CONSTEXPR T hypot(T x, T y)
{
    return sqrt(x * x + y * y);
}

template<FloatingPoint T>
MATH_CONSTEXPR T sin(T angle)
{
    CONSTEXPR_STATE(sin, angle);
    T ret;
    asm(
        "fsin"
        : "=t"(ret)
        : "0"(angle));
    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T cos(T angle)
{
    CONSTEXPR_STATE(cos, angle);
    T ret;
    asm(
        "fcos"
        : "=t"(ret)
        : "0"(angle));
    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T tan(T angle)
{
    CONSTEXPR_STATE(tan, angle);
    double ret, one;
    asm(
        "fptan"
        : "=t"(one), "=u"(ret)
        : "0"(angle));

    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T atan(T value)
{
    CONSTEXPR_STATE(atan, value);

    T ret;
    asm(
        "fld1\n"
        "fpatan\n"
        : "=t"(ret)
        : "0"(value));
    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T asin(T x)
{
    CONSTEXPR_STATE(asin, x);
    if (x > 1 || x < -1)
        return NAN;
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
MATH_CONSTEXPR T acos(T value)
{
    CONSTEXPR_STATE(acos, value);

    // FIXME: I am naive
    return (T)M_PIl + asin(value);
}

template<FloatingPoint T>
MATH_CONSTEXPR T atan2(T y, T x)
{
    CONSTEXPR_STATE(atan2, y, x);

    T ret;
    asm("fpatan"
        : "=t"(ret)
        : "0"(x), "u"(y)
        : "st(1)");
    return ret;
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
MATH_CONSTEXPR T log(T x)
{
    CONSTEXPR_STATE(log, x);

    T ret;
    asm(
        "fldln2\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T log2(T x)
{
    CONSTEXPR_STATE(log2, x);

    T ret;
    asm(
        "fld1\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T log10(T x)
{
    CONSTEXPR_STATE(log10, x);

    T ret;
    asm(
        "fldlg2\n"
        "fxch %%st(1)\n"
        "fyl2x\n"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

template<FloatingPoint T>
MATH_CONSTEXPR T exp(T exponent)
{
    CONSTEXPR_STATE(exp, exponent);

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
}

template<FloatingPoint T>
MATH_CONSTEXPR T exp2(T exponent)
{
    CONSTEXPR_STATE(exp2, exponent);

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
}

template<FloatingPoint T>
MATH_CONSTEXPR T expm1(T value)
{
    // FIXME: I am less accurate than possible
    CONSTEXPR_STATE(expm1, value);
    return exp(value) - 1;
}

template<FloatingPoint T>
MATH_CONSTEXPR T log1p(T value)
{
    // FIXME: I am less accurate than possible
    CONSTEXPR_STATE(log1p, value);
    return log(1 + value);
}
}

using Exponentials::exp;
using Exponentials::exp2;
using Exponentials::expm1;
using Exponentials::log;
using Exponentials::log10;
using Exponentials::log1p;
using Exponentials::log2;

namespace Hyperbolic {

template<FloatingPoint T>
MATH_CONSTEXPR T sinh(T x)
{
    T exponentiated = exp<T>(x);
    if (x > 0)
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    return (exponentiated - 1 / exponentiated) / 2;
}

template<FloatingPoint T>
MATH_CONSTEXPR T cosh(T x)
{
    CONSTEXPR_STATE(cosh, x);

    T exponentiated = exp(-x);
    if (x < 0)
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    return (1 / exponentiated + exponentiated) / 2;
}

template<FloatingPoint T>
MATH_CONSTEXPR T tanh(T x)
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
MATH_CONSTEXPR T asinh(T x)
{
    return log<T>(x + sqrt<T>(x * x + 1));
}

template<FloatingPoint T>
MATH_CONSTEXPR T acosh(T x)
{
    return log<T>(x + sqrt<T>(x * x - 1));
}

template<FloatingPoint T>
MATH_CONSTEXPR T atanh(T x)
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
MATH_CONSTEXPR T pow(T x, T y)
{
    CONSTEXPR_STATE(pow, x, y);
    // fixme I am naive
    if (isnan(y))
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

template<FloatingPoint T>
MATH_CONSTEXPR T copysign(T x, T y)
{
    if ((y < 0 && x < 0) || (y >= 0 && x >= 0))
        return x;

    return -x;
}

template<Integral I>
MATH_CONSTEXPR I copysign(I x, I y) requires(IsSigned<I>)
{
    return (x & ((1 << (sizeof(I) * 8 - 1)) - 1)) | (y & (1 << (sizeof(I) * 8 - 1)));
}

}

#undef MATH_CONSTEXPR
#undef CONSTEXPR_STATE
