/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// FIXME: This is just for the Lagom build on M1 Macs, if we ever want to build
//        for aarch64/Arm64, we should implement these properly, I tried to
//        implement rounding, but I was not able to get clang happy with it
//        although gcc was
#include <AK/Concepts.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>
#include <AK/bits/Math_common.h>
#include <math.h>

#ifdef __serenity__
#    error You are using the Lagom-only version of AK/Math.h during the main build\
falling back to LibM is not supported during non Lagom builds
#endif

#define MAKE_MATH_BACKED1(name)               \
    template<FloatingPoint T>                 \
    MATH_CONSTEXPR T name(T arg)              \
    {                                         \
        if (IsSame<RemoveCV<T>, long double>) \
            return ::name##l(arg);            \
        if (IsSame<RemoveCV<T>, double>)      \
            return ::name(arg);               \
        if (IsSame<RemoveCV<T>, float>)       \
            return ::name##f(arg);            \
    }
#define MAKE_MATH_BACKED2(name)               \
    template<FloatingPoint T>                 \
    MATH_CONSTEXPR T name(T arg1, T arg2)     \
    {                                         \
        if (IsSame<RemoveCV<T>, long double>) \
            return ::name##l(arg1, arg2);     \
        if (IsSame<RemoveCV<T>, double>)      \
            return ::name(arg1, arg2);        \
        if (IsSame<RemoveCV<T>, float>)       \
            return ::name##f(arg1, arg2);     \
    }

namespace AK {
MAKE_MATH_BACKED1(ceil);
MAKE_MATH_BACKED1(floor);
MAKE_MATH_BACKED1(round);
MAKE_MATH_BACKED1(trunc);
MAKE_MATH_BACKED1(sqrt);
MAKE_MATH_BACKED1(cbrt);
template<FloatingPoint T>
MATH_CONSTEXPR T fabs(T arg) { return ::abs(arg); }
MAKE_MATH_BACKED1(exp);
MAKE_MATH_BACKED1(exp2);
MAKE_MATH_BACKED1(expm1);
MAKE_MATH_BACKED1(log);
MAKE_MATH_BACKED1(log10);
MAKE_MATH_BACKED1(log2);
MAKE_MATH_BACKED1(log1p);
MAKE_MATH_BACKED1(acos);
MAKE_MATH_BACKED1(asin);
MAKE_MATH_BACKED1(atan);
MAKE_MATH_BACKED1(cos);
MAKE_MATH_BACKED1(sin);
MAKE_MATH_BACKED1(tan);
MAKE_MATH_BACKED1(asinh);
MAKE_MATH_BACKED1(acosh);
MAKE_MATH_BACKED1(atanh);
MAKE_MATH_BACKED1(sinh);
MAKE_MATH_BACKED1(cosh);
MAKE_MATH_BACKED1(tanh);

MAKE_MATH_BACKED2(atan2);
MAKE_MATH_BACKED2(hypot);
MAKE_MATH_BACKED2(fmod);
template<FloatingPoint T>
MATH_CONSTEXPR T modf(T arg1, T& arg2) { return ::modf(arg1, &arg2); }
MAKE_MATH_BACKED2(remainder);
MAKE_MATH_BACKED2(pow);
MAKE_MATH_BACKED2(copysign);

#define MAKE_MATH_BACKED_TO_INT(name)         \
    template<Signed I = int, FloatingPoint T> \
    MATH_CONSTEXPR I name##_to_int(T arg)     \
    {                                         \
        if (IsSame<RemoveCV<T>, long double>) \
            return (I)::name##l(arg);         \
        if (IsSame<RemoveCV<T>, double>)      \
            return (I)::name(arg);            \
        if (IsSame<RemoveCV<T>, float>)       \
            return (I)::name##f(arg);         \
    }

MAKE_MATH_BACKED_TO_INT(floor)
// FIXME: llround()
MAKE_MATH_BACKED_TO_INT(round)
MAKE_MATH_BACKED_TO_INT(ceil)
MAKE_MATH_BACKED_TO_INT(trunc)

template<FloatingPoint T>
MATH_CONSTEXPR T fast_round(T value)
{
    return rint(value);
}
template<Signed I = int, FloatingPoint T>
MATH_CONSTEXPR I fast_round_to_int(T value)
{
    return (I)rint(value);
}

}

#undef MAKE_MATH_BACKED1
#undef MAKE_MATH_BACKED2
#undef INTEGER_BUILTIN
#undef MAKE_MATH_BACKED_TO_INT
#undef MATH_CONSTEXPR
#undef CONSTEXPR_STATE
