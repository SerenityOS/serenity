/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace AK {

constexpr u32 string_hash(char const* characters, size_t length, u32 seed = 0)
{
    u32 hash = seed;
    for (size_t i = 0; i < length; ++i) {
        hash += (u32)characters[i];
        hash += (hash << 10);
        hash ^= (hash >> 6);
    }
    hash += hash << 3;
    hash ^= hash >> 11;
    hash += hash << 15;
    return hash;
}

}

using AK::string_hash;
