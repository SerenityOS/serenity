/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Video::VP9 {

// FIXME: Once everything is working, replace this with plain clamp
// since parameter order is different
template<typename T>
T clip_3(T x, T y, T z)
{
    return clamp(z, x, y);
}

template<typename T>
u16 clip_1(u8 bit_depth, T x)
{
    if (x < 0) {
        return 0u;
    }
    const T max = (1u << bit_depth) - 1u;
    if (x > max)
        return max;
    return x;
}

template<typename T, typename C>
inline T brev(C bit_count, T value)
{
    T result = 0;
    for (C i = 0; i < bit_count; i++) {
        auto bit = (value >> i) & 1;
        result |= bit << (bit_count - 1 - i);
    }
    return result;
}

}
