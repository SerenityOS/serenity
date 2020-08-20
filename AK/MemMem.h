/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>

namespace AK {

namespace {
const static void* bitap_bitwise(const void* haystack, size_t haystack_length, const void* needle, size_t needle_length)
{
    ASSERT(needle_length < 32);

    u64 lookup = 0xfffffffe;

    constexpr size_t mask_length = (size_t)((u8)-1) + 1;
    u64 needle_mask[mask_length];

    for (size_t i = 0; i < mask_length; ++i)
        needle_mask[i] = 0xffffffff;

    for (size_t i = 0; i < needle_length; ++i)
        needle_mask[((const u8*)needle)[i]] &= ~(0x00000001 << i);

    for (size_t i = 0; i < haystack_length; ++i) {
        lookup |= needle_mask[((const u8*)haystack)[i]];
        lookup <<= 1;

        if (!(lookup & (0x00000001 << needle_length)))
            return ((const u8*)haystack) + i - needle_length + 1;
    }

    return nullptr;
}
}

static inline const void* memmem(const void* haystack, size_t haystack_length, const void* needle, size_t needle_length)
{
    if (needle_length == 0)
        return haystack;

    if (haystack_length < needle_length)
        return nullptr;

    if (haystack_length == needle_length)
        return __builtin_memcmp(haystack, needle, haystack_length) == 0 ? haystack : nullptr;

    if (needle_length < 32)
        return bitap_bitwise(haystack, haystack_length, needle, needle_length);

    // Fallback to a slower search.
    auto length_diff = haystack_length - needle_length;
    for (size_t i = 0; i < length_diff; ++i) {
        const auto* start = ((const u8*)haystack) + i;
        if (__builtin_memcmp(start, needle, needle_length) == 0)
            return start;
    }

    return nullptr;
}

}
