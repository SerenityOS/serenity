/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibGfx/Size.h>

#include "LookupTables.h"

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

inline Gfx::Size<u8> block_size_to_sub_blocks(BlockSubsize size)
{
    return Gfx::Size<u8>(num_4x4_blocks_wide_lookup[size], num_4x4_blocks_high_lookup[size]);
}

template<Integral T>
inline T blocks_to_sub_blocks(T blocks)
{
    return blocks << 1;
}

template<Integral T>
inline T sub_blocks_to_blocks(T sub_blocks)
{
    return sub_blocks >> 1;
}

template<Integral T>
inline T sub_blocks_to_pixels(T sub_blocks)
{
    return sub_blocks << 2;
}

template<Integral T>
inline T pixels_to_sub_blocks(T pixels)
{
    return pixels >> 2;
}

template<Integral T>
inline T blocks_to_pixels(T blocks)
{
    return sub_blocks_to_pixels(blocks_to_sub_blocks(blocks));
}

template<Integral T>
inline T pixels_to_blocks(T pixels)
{
    return sub_blocks_to_blocks(pixels_to_sub_blocks(pixels));
}

inline u8 transform_size_to_sub_blocks(TransformSize transform_size)
{
    return 1 << transform_size;
}

}
