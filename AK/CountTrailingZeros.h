/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Concepts.h>
#include <AK/Platform.h>
#include <AK/Types.h>

namespace AK {

template<Integral T>
ALWAYS_INLINE constexpr size_t count_trailing_zeros(T val)
{
    constexpr size_t char_bit = 8;
    constexpr size_t T_bit = sizeof(T) * char_bit;

    if (val == 0)
        return T_bit;

#if defined(__GNUC__) || defined(__clang__)
    if constexpr (sizeof(T) == sizeof(int))
        return __builtin_ctz(val);
    if constexpr (sizeof(T) == sizeof(long))
        return __builtin_ctzl(val);
    if constexpr (sizeof(T) == sizeof(long long))
        return __builtin_ctzll(val);
#endif

    for (size_t count = 0; count < T_bit; ++count) {
        if ((val >> count) & 1) {
            return count;
        }
    }
    VERIFY_NOT_REACHED();
    return T_bit;
}

}

using AK::count_trailing_zeros;
