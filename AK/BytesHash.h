/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK {

constexpr u32 bytes_hash(u8 const* bytes, size_t size)
{
    u32 hash = 0;
    for (size_t i = 0; i < size; ++i) {
        hash += (u32)bytes[i];
        hash += (hash << 10);
        hash ^= (hash >> 6);
    }
    hash += hash << 3;
    hash ^= hash >> 11;
    hash += hash << 15;
    return hash;
}

}

using AK::bytes_hash;
