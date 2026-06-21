/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 * Copyright (c) 2024-2026, Nico Weber <thakis@chromium.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Math/Constants.h>
#include <AK/Math/Copysign.h>
#include <AK/Math/Sqrt.h>
#include <math.h>

#include <AK/Math/Macros.h>

namespace AK {

namespace Trigonometry {

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
    T sign = 1;
    if (angle < 0) {
        angle = -angle;
        sign = -1;
    }

    if (angle >= 2 * Pi<T>)
        angle = fmod(angle, 2 * Pi<T>);

    if (angle >= Pi<T>) {
        angle = angle - Pi<T>;
        sign = -sign;
    }

    if (angle > Pi<T> / 2)
        angle = Pi<T> - angle;

    // https://github.com/samhocevar/lolremez/wiki/Tutorial-4-of-5%3A-fixing-lower-order-parameters
    auto f = [](T x) {
        if constexpr (IsSame<T, f32>) {
            // lolremez --float --degree 3 --range "1e-50:pi*pi/4" "(sin(sqrt(x))-sqrt(x))/(x*sqrt(x))" "1/(x*sqrt(x))"
            // "Estimated max error: 4.618689e-9"
            f32 u = 2.6000548e-06f;
            u = u * x + -0.00019806615f;
            u = u * x + 0.0083330171f;
            return u * x + -0.16666657f;
        } else {
            // FIXME: Could do something custom for long double.
            // lolremez --degree 6 --range "1e-50:pi*pi/4" "(sin(sqrt(x))-sqrt(x))/(x*sqrt(x))" "1/(x*sqrt(x))"
            // "Estimated max error: 1.1015766629825144e-16"
            T u = -7.3646464502210486e-13;
            u = u * x + 1.6047301196685753e-10;
            u = u * x + -2.5051851497012596e-08;
            u = u * x + 2.7557316077007725e-06;
            u = u * x + -0.00019841269820094207;
            u = u * x + 0.0083333333332628792;
            return u * x + -0.16666666666665811;
        }
    };
    T angle_squared = angle * angle;
    return sign * (angle + angle * angle_squared * f(angle_squared));
#    else
    CALL_BUILTIN(sin, angle);
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
        angle = -angle;

    if (angle >= 2 * Pi<T>)
        angle = fmod(angle, 2 * Pi<T>);

    T sign = 1;
    if (angle >= Pi<T>) {
        angle = angle - Pi<T>;
        sign = -1;
    }

    if (angle > Pi<T> / 2) {
        angle = Pi<T> - angle;
        sign = -sign;
    }

    // https://github.com/samhocevar/lolremez/wiki/Tutorial-3-of-5:-changing-variables-for-simpler-polynomials
    // for cos(x): cos(x) - 1 is a function of x^2 terms, so we do the substitution on that page like
    // max | cos(x) - 1 - x^2 Q(x^2) | and then set y = x^2. That yields:
    // max | (cos(sqrt(y)) - 1) - y Q(y) |. Dividing through with y (instead of sqrt(y) as in the sin() case) gives us:
    auto f = [](T x) {
        if constexpr (IsSame<T, f32>) {
            // lolremez --float --degree 3 --range "1e-50:pi*pi/4" "(cos(sqrt(x)) - 1)/x"  "1/x"
            // "Estimated max error: 5.2720126e-8"
            f32 u = 2.3194387e-05f;
            u = u * x + -0.0013855927f;
            u = u * x + 0.041663989f;
            return u * x + -0.49999931f;
        } else {
            // lolremez --degree 6 --range "1e-50:pi*pi/4" "(cos(sqrt(x)) - 1)/x"  "1/x"
            // "Estimated max error: 2.0880269759116624e-15"
            T u = -1.101249182846601e-11;
            u = u * x + 2.0858735176345955e-09;
            u = u * x + -2.7556950755056579e-07;
            u = u * x + 2.4801583156341611e-05;
            u = u * x + -0.001388888886393419;
            u = u * x + 0.041666666665954213;
            return u * x + -0.49999999999993117;
        }
    };
    T angle_squared = angle * angle;
    return sign * (1 + angle_squared * f(angle_squared));
#    else
    CALL_BUILTIN(cos, angle);
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
    CALL_BUILTIN(tan, angle);
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
    if (value < 0)
        return -atan(-value);

    if (value > 1)
        return Pi<T> / 2 - atan(1 / value);

    // atan is an odd function a leading factor of 1, so this uses the same substitutions as sin().
    // See there for a description.
    auto f = [](T x) {
        if constexpr (IsSame<T, f32>) {
            // lolremez --float --degree 7 --range "1e-50:1" "(atan(sqrt(x))-sqrt(x))/(x*sqrt(x))" "1/(x*sqrt(x))"
            // "Estimated max error: 7.351572e-9"
            float u = 0.0026222446f;
            u = u * x + -0.015132537f;
            u = u * x + 0.041121863f;
            u = u * x + -0.073667064f;
            u = u * x + 0.10573932f;
            u = u * x + -0.14185975f;
            u = u * x + 0.19990396f;
            return u * x + -0.33332986f;
        } else {
            // FIXME: Could do something custom for long double.
            // lolremez --degree 18 --range "1e-50:1" "(atan(sqrt(x))-sqrt(x))/(x*sqrt(x))" "1/(x*sqrt(x))"
            // "Estimated max error: 1.1404872427303659e-17"
            T u = -1.7655716820720429e-05;
            u = u * x + 0.00019779001389463795;
            u = u * x + -0.001051241688667142;
            u = u * x + 0.0035473387064041432;
            u = u * x + -0.008607083731273335;
            u = u * x + 0.016195140256854403;
            u = u * x + -0.025032045474239515;
            u = u * x + 0.033452143515758453;
            u = u * x + -0.040558834967395346;
            u = u * x + 0.046569903058368577;
            u = u * x + -0.052332028769674181;
            u = u * x + 0.05875670126323182;
            u = u * x + -0.066655258619483626;
            u = u * x + 0.076921627408511856;
            u = u * x + -0.090908958935242423;
            u = u * x + 0.11111110296047642;
            u = u * x + -0.14285714254349127;
            u = u * x + 0.1999999999935175;
            return u * x + -0.33333333333328197;
        }
    };
    T squared = value * value;
    return value + value * squared * f(squared);
#    endif
    CALL_BUILTIN(atan, value);
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
    CALL_BUILTIN(atan2, y, x);
#    endif
#endif
}

}

#include <AK/Math/UndefMacros.h>

using Trigonometry::acos;
using Trigonometry::asin;
using Trigonometry::atan;
using Trigonometry::atan2;
using Trigonometry::cos;
using Trigonometry::hypot;
using Trigonometry::sin;
using Trigonometry::sincos;
using Trigonometry::tan;

}
