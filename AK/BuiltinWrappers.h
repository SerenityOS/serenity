/*
 * Copyright (c) 2021, Nick Johnson <sylvyrfysh@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Concepts.h"

template<Unsigned IntType>
inline constexpr size_t popcount(IntType value)
{
#if defined(__GNUC__) || defined(__clang__)
    static_assert(sizeof(IntType) <= sizeof(unsigned long long));
    if constexpr (sizeof(IntType) <= sizeof(unsigned int))
        return __builtin_popcount(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long))
        return __builtin_popcountl(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long long))
        return __builtin_popcountll(value);
    VERIFY_NOT_REACHED();
#else
    size_t ones = 0;
    for (size_t i = 0; i < bit_sizeof(IntType); ++i) {
        if ((val >> i) & 1) {
            ++ones;
        }
    }
    return ones;
#endif
}

template<Unsigned IntType>
inline constexpr size_t count_ones(IntType value)
{
    return popcount(value);
}

template<Unsigned IntType>
inline constexpr size_t count_zeroes(IntType value)
{
    return popcount(static_cast<IntType>(~value));
}

// The function will return the number of trailing zeroes in the type. If
// the given number is zero, this function will return the number of bits
// bits in the IntType.
template<Unsigned IntType>
inline constexpr size_t count_trailing_zeroes(IntType value)
{
    if (value == 0) [[unlikely]]
        return bit_sizeof(IntType);
#if defined(__GNUC__) || defined(__clang__)
    static_assert(sizeof(IntType) <= sizeof(unsigned long long));
    if constexpr (sizeof(IntType) <= sizeof(unsigned int))
        return __builtin_ctz(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long))
        return __builtin_ctzl(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long long))
        return __builtin_ctzll(value);
    VERIFY_NOT_REACHED();
#else
    for (size_t i = 0; i < bit_sizeof(IntType); ++i) {
        if ((val >> i) & 1) {
            return i;
        }
    }
    return bit_sizeof(IntType);
#endif
}

template<Unsigned IntType>
inline constexpr size_t count_trailing_ones(IntType value)
{
    return count_trailing_zeroes(static_cast<IntType>(~value));
}

// The function will return the number of leading zeroes in the type. If
// the given number is zero, this function will return the number of bits
// in the IntType.
template<Unsigned IntType>
inline constexpr size_t count_leading_zeroes(IntType value)
{
    if (value == 0) [[unlikely]]
        return bit_sizeof(IntType);
#if defined(__GNUC__) || defined(__clang__)
    static_assert(sizeof(IntType) <= sizeof(unsigned long long));
    if constexpr (sizeof(IntType) <= sizeof(unsigned int))
        return __builtin_clz(value) - (bit_sizeof(unsigned int) - bit_sizeof(IntType));
    if constexpr (sizeof(IntType) == sizeof(unsigned long))
        return __builtin_clzl(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long long))
        return __builtin_clzll(value);
    VERIFY_NOT_REACHED();
#else
    // Wrap around, catch going past zero by noticing that i is greater than the number of bits in the number
    for (size_t i = (bit_sizeof(IntType)) - 1; i < bit_sizeof(IntType); --i) {
        if ((val >> i) & 1) {
            return i;
        }
    }
    return bit_sizeof(IntType);
#endif
}

template<Unsigned IntType>
inline constexpr size_t count_leading_ones(IntType value)
{
    return count_leading_zeroes(static_cast<IntType>(~value));
}

// The function will return the index of leading one bit in the type. If
// the given number is zero, this function will return zero.
template<Integral IntType>
inline constexpr size_t bit_scan_forward(IntType value)
{
#if defined(__GNUC__) || defined(__clang__)
    static_assert(sizeof(IntType) <= sizeof(unsigned long long));
    if constexpr (sizeof(IntType) <= sizeof(unsigned int))
        return __builtin_ffs(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long))
        return __builtin_ffsl(value);
    if constexpr (sizeof(IntType) == sizeof(unsigned long long))
        return __builtin_ffsll(value);
    VERIFY_NOT_REACHED();
#else
    if (value == 0)
        return 0;
    return 1 + count_trailing_zeroes(static_cast<MakeUnsigned<IntType>>(value));
#endif
}
