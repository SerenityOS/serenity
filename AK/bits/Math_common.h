/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>

#ifdef __clang__
#    define MATH_CONSTEXPR ALWAYS_INLINE
#    define CONSTEXPR_STATE(function, args...)
#else
#    define MATH_CONSTEXPR constexpr
#    define CONSTEXPR_STATE(function, args...)        \
        if (is_constant_evaluated()) {                \
            if (IsSame<RemoveCV<T>, long double>)     \
                return __builtin_##function##l(args); \
            if (IsSame<RemoveCV<T>, double>)          \
                return __builtin_##function(args);    \
            if (IsSame<RemoveCV<T>, float>)           \
                return __builtin_##function##f(args); \
        }
#endif

namespace AK {
template<Integral T>
MATH_CONSTEXPR T clz(T x)
{
    if (!x)
        return sizeof(T) * 8;
    if constexpr (sizeof(T) == sizeof(long long))
        return __builtin_clzll(x);
    if constexpr (sizeof(T) == sizeof(long))
        return __builtin_clzl(x);
    return __builtin_clz(x);
}

template<Integral T>
MATH_CONSTEXPR T ctz(T x)
{
    if (!x)
        return sizeof(T) * 8;
    if constexpr (sizeof(T) == sizeof(long long))
        return __builtin_ctzll(x);
    if constexpr (sizeof(T) == sizeof(long))
        return __builtin_ctzl(x);
    return __builtin_ctz(x);
}
template<Integral T>
MATH_CONSTEXPR T popcnt(T x)
{
    if constexpr (sizeof(T) == sizeof(long long))
        return __builtin_popcountll(x);
    if constexpr (sizeof(T) == sizeof(long))
        return __builtin_popcountl(x);
    return __builtin_popcount(x);
}

template<Integral T>
MATH_CONSTEXPR T exp2(T exponent)
{
    return 1u << exponent;
}

template<Integral T>
MATH_CONSTEXPR T log2(T x)
{
    return x ? 8 * sizeof(T) - clz(x) : 0;
}

}
