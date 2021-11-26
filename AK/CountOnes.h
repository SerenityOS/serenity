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
ALWAYS_INLINE constexpr size_t count_ones(T val)
{
#if defined(__GNUC__) || defined(__clang__)
    if constexpr (sizeof(T) == sizeof(int))
        return __builtin_popcount(val);
    if constexpr (sizeof(T) == sizeof(long))
        return __builtin_popcountl(val);
    if constexpr (sizeof(T) == sizeof(long long))
        return __builtin_popcountll(val);
#endif

    size_t count = 0;
    do {
        if (val & 1) {
            count++;
        }
    } while (val >>= 1);
    return count;
}

}

using AK::count_ones;
