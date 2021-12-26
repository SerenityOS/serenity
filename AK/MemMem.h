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

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/Vector.h>

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

template<typename HaystackIterT>
static inline Optional<size_t> memmem(const HaystackIterT& haystack_begin, const HaystackIterT& haystack_end, Span<const u8> needle) requires(requires { (*haystack_begin).data(); (*haystack_begin).size(); })
{
    auto prepare_kmp_partial_table = [&] {
        Vector<int, 64> table;
        table.resize(needle.size());

        size_t position = 1;
        int candidate = 0;

        table[0] = -1;
        while (position < needle.size()) {
            if (needle[position] == needle[candidate]) {
                table[position] = table[candidate];
            } else {
                table[position] = candidate;
                do {
                    candidate = table[candidate];
                } while (candidate >= 0 && needle[candidate] != needle[position]);
            }
            ++position;
            ++candidate;
        }
        return table;
    };

    auto table = prepare_kmp_partial_table();
    size_t total_haystack_index = 0;
    size_t current_haystack_index = 0;
    int needle_index = 0;
    auto haystack_it = haystack_begin;

    while (haystack_it != haystack_end) {
        auto&& chunk = *haystack_it;
        if (current_haystack_index >= chunk.size()) {
            current_haystack_index = 0;
            ++haystack_it;
            continue;
        }
        if (needle[needle_index] == chunk[current_haystack_index]) {
            ++needle_index;
            ++current_haystack_index;
            ++total_haystack_index;
            if ((size_t)needle_index == needle.size())
                return total_haystack_index - needle_index;
            continue;
        }
        needle_index = table[needle_index];
        if (needle_index < 0) {
            ++needle_index;
            ++current_haystack_index;
            ++total_haystack_index;
        }
    }

    return {};
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

    // Fallback to KMP.
    Array<Span<const u8>, 1> spans { Span<const u8> { (const u8*)haystack, haystack_length } };
    auto result = memmem(spans.begin(), spans.end(), { (const u8*)needle, needle_length });

    if (result.has_value())
        return (const u8*)haystack + result.value();

    return {};
}

}
