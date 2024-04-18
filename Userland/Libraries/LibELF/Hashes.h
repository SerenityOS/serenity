/*
 * Copyright (c) 2019-2020, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace ELF {

constexpr u32 compute_sysv_hash(StringView name)
{
    // SYSV ELF hash algorithm
    // Note that the GNU HASH algorithm has less collisions

    u32 hash = 0;

    for (auto ch : name) {
        hash = hash << 4;
        hash += ch;

        u32 const top_nibble_of_hash = hash & 0xf0000000u;
        hash ^= top_nibble_of_hash >> 24;
        hash &= ~top_nibble_of_hash;
    }

    return hash;
}

constexpr u32 compute_gnu_hash(StringView name)
{
    // GNU ELF hash algorithm
    u32 hash = 5381;

    for (auto ch : name)
        hash = hash * 33 + ch;

    return hash;
}

}
