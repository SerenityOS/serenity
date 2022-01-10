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

constexpr u32 case_insensitive_string_hash(char const* characters, size_t length, u32 seed = 0)
{
    // AK/CharacterTypes.h cannot be included from here.
    auto to_lowercase = [](char ch) -> u32 {
        if (ch >= 'A' && ch <= 'Z')
            return static_cast<u32>(ch) + 0x20;
        return static_cast<u32>(ch);
    };

    u32 hash = seed;
    for (size_t i = 0; i < length; ++i) {
        hash += to_lowercase(characters[i]);
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
