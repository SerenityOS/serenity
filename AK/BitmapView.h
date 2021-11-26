/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Optional.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

static constexpr Array bitmask_first_byte = { 0xFF, 0xFE, 0xFC, 0xF8, 0xF0, 0xE0, 0xC0, 0x80 };
static constexpr Array bitmask_last_byte = { 0x00, 0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F };

class BitmapView {
public:
    BitmapView() = default;

    BitmapView(u8* data, size_t size)
        : m_data(data)
        , m_size(size)
    {
    }

    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] size_t size_in_bytes() const { return ceil_div(m_size, static_cast<size_t>(8)); }
    [[nodiscard]] bool get(size_t index) const
    {
        VERIFY(index < m_size);
        return 0 != (m_data[index / 8] & (1u << (index % 8)));
    }

    [[nodiscard]] size_t count_slow(bool value) const
    {
        return count_in_range(0, m_size, value);
    }

    [[nodiscard]] size_t count_in_range(size_t start, size_t len, bool value) const
    {
        VERIFY(start < m_size);
        VERIFY(start + len <= m_size);
        if (len == 0)
            return 0;

        size_t count;
        const u8* first = &m_data[start / 8];
        const u8* last = &m_data[(start + len) / 8];
        u8 byte = *first;
        byte &= bitmask_first_byte[start % 8];
        if (first == last) {
            byte &= bitmask_last_byte[(start + len) % 8];
            count = __builtin_popcount(byte);
        } else {
            count = __builtin_popcount(byte);
            // Don't access *last if it's out of bounds
            if (last < &m_data[size_in_bytes()]) {
                byte = *last;
                byte &= bitmask_last_byte[(start + len) % 8];
                count += __builtin_popcount(byte);
            }
            if (++first < last) {
                const u32* ptr32 = (const u32*)(((FlatPtr)first + sizeof(u32) - 1) & ~(sizeof(u32) - 1));
                if ((const u8*)ptr32 > last)
                    ptr32 = (const u32*)last;
                while (first < (const u8*)ptr32) {
                    count += __builtin_popcount(*first);
                    first++;
                }
                const u32* last32 = (const u32*)((FlatPtr)last & ~(sizeof(u32) - 1));
                while (ptr32 < last32) {
                    count += __builtin_popcountl(*ptr32);
                    ptr32++;
                }
                for (first = (const u8*)ptr32; first < last; first++)
                    count += __builtin_popcount(*first);
            }
        }

        if (!value)
            count = len - count;
        return count;
    }

    [[nodiscard]] bool is_null() const { return m_data == nullptr; }

    [[nodiscard]] const u8* data() const { return m_data; }

    template<bool VALUE>
    Optional<size_t> find_one_anywhere(size_t hint = 0) const
    {
        VERIFY(hint < m_size);
        const u8* end = &m_data[m_size / 8];

        for (;;) {
            // We will use hint as what it is: a hint. Because we try to
            // scan over entire 32 bit words, we may start searching before
            // the hint!
            const u32* ptr32 = (const u32*)((FlatPtr)&m_data[hint / 8] & ~(sizeof(u32) - 1));
            if ((const u8*)ptr32 < &m_data[0]) {
                ptr32++;

                // m_data isn't aligned, check first bytes
                size_t start_ptr32 = (const u8*)ptr32 - &m_data[0];
                size_t i = 0;
                u8 byte = VALUE ? 0x00 : 0xff;
                while (i < start_ptr32 && m_data[i] == byte)
                    i++;
                if (i < start_ptr32) {
                    byte = m_data[i];
                    if constexpr (!VALUE)
                        byte = ~byte;
                    VERIFY(byte != 0);
                    return i * 8 + __builtin_ffs(byte) - 1;
                }
            }

            u32 val32 = VALUE ? 0x0 : 0xffffffff;
            const u32* end32 = (const u32*)((FlatPtr)end & ~(sizeof(u32) - 1));
            while (ptr32 < end32 && *ptr32 == val32)
                ptr32++;

            if (ptr32 == end32) {
                // We didn't find anything, check the remaining few bytes (if any)
                u8 byte = VALUE ? 0x00 : 0xff;
                size_t i = (const u8*)ptr32 - &m_data[0];
                size_t byte_count = m_size / 8;
                VERIFY(i <= byte_count);
                while (i < byte_count && m_data[i] == byte)
                    i++;
                if (i == byte_count) {
                    if (hint <= 8)
                        return {}; // We already checked from the beginning

                    // Try scanning before the hint
                    end = (const u8*)((FlatPtr)&m_data[hint / 8] & ~(sizeof(u32) - 1));
                    hint = 0;
                    continue;
                }
                byte = m_data[i];
                if constexpr (!VALUE)
                    byte = ~byte;
                VERIFY(byte != 0);
                return i * 8 + __builtin_ffs(byte) - 1;
            }

            // NOTE: We don't really care about byte ordering. We found *one*
            // free bit, just calculate the position and return it
            val32 = *ptr32;
            if constexpr (!VALUE)
                val32 = ~val32;
            VERIFY(val32 != 0);
            return ((const u8*)ptr32 - &m_data[0]) * 8 + __builtin_ffsl(val32) - 1;
        }
    }

    Optional<size_t> find_one_anywhere_set(size_t hint = 0) const
    {
        return find_one_anywhere<true>(hint);
    }

    Optional<size_t> find_one_anywhere_unset(size_t hint = 0) const
    {
        return find_one_anywhere<false>(hint);
    }

    template<bool VALUE>
    Optional<size_t> find_first() const
    {
        size_t byte_count = m_size / 8;
        size_t i = 0;

        u8 byte = VALUE ? 0x00 : 0xff;
        while (i < byte_count && m_data[i] == byte)
            i++;
        if (i == byte_count)
            return {};

        byte = m_data[i];
        if constexpr (!VALUE)
            byte = ~byte;
        VERIFY(byte != 0);
        return i * 8 + __builtin_ffs(byte) - 1;
    }

    Optional<size_t> find_first_set() const { return find_first<true>(); }
    Optional<size_t> find_first_unset() const { return find_first<false>(); }

    // The function will return the next range of unset bits starting from the
    // @from value.
    // @from: the position from which the search starts. The var will be
    //        changed and new value is the offset of the found block.
    // @min_length: minimum size of the range which will be returned.
    // @max_length: maximum size of the range which will be returned.
    //              This is used to increase performance, since the range of
    //              unset bits can be long, and we don't need the while range,
    //              so we can stop when we've reached @max_length.
    inline Optional<size_t> find_next_range_of_unset_bits(size_t& from, size_t min_length = 1, size_t max_length = max_size) const
    {
        if (min_length > max_length) {
            return {};
        }

        u32* bitmap32 = (u32*)m_data;

        // Calculating the start offset.
        size_t start_bucket_index = from / 32;
        size_t start_bucket_bit = from % 32;

        size_t* start_of_free_chunks = &from;
        size_t free_chunks = 0;

        for (size_t bucket_index = start_bucket_index; bucket_index < m_size / 32; ++bucket_index) {
            if (bitmap32[bucket_index] == 0xffffffff) {
                // Skip over completely full bucket of size 32.
                if (free_chunks >= min_length) {
                    return min(free_chunks, max_length);
                }
                free_chunks = 0;
                start_bucket_bit = 0;
                continue;
            }
            if (bitmap32[bucket_index] == 0x0) {
                // Skip over completely empty bucket of size 32.
                if (free_chunks == 0) {
                    *start_of_free_chunks = bucket_index * 32;
                }
                free_chunks += 32;
                if (free_chunks >= max_length) {
                    return max_length;
                }
                start_bucket_bit = 0;
                continue;
            }

            u32 bucket = bitmap32[bucket_index];
            u8 viewed_bits = start_bucket_bit;
            u32 trailing_zeroes = 0;

            bucket >>= viewed_bits;
            start_bucket_bit = 0;

            while (viewed_bits < 32) {
                if (bucket == 0) {
                    if (free_chunks == 0) {
                        *start_of_free_chunks = bucket_index * 32 + viewed_bits;
                    }
                    free_chunks += 32 - viewed_bits;
                    viewed_bits = 32;
                } else {
                    trailing_zeroes = count_trailing_zeroes_32(bucket);
                    bucket >>= trailing_zeroes;

                    if (free_chunks == 0) {
                        *start_of_free_chunks = bucket_index * 32 + viewed_bits;
                    }
                    free_chunks += trailing_zeroes;
                    viewed_bits += trailing_zeroes;

                    if (free_chunks >= min_length) {
                        return min(free_chunks, max_length);
                    }

                    // Deleting trailing ones.
                    u32 trailing_ones = count_trailing_zeroes_32(~bucket);
                    bucket >>= trailing_ones;
                    viewed_bits += trailing_ones;
                    free_chunks = 0;
                }
            }
        }

        if (free_chunks < min_length) {
            size_t first_trailing_bit = (m_size / 32) * 32;
            size_t trailing_bits = size() % 32;
            for (size_t i = 0; i < trailing_bits; ++i) {
                if (!get(first_trailing_bit + i)) {
                    if (free_chunks == 0)
                        *start_of_free_chunks = first_trailing_bit + i;
                    if (++free_chunks >= min_length)
                        return min(free_chunks, max_length);
                } else {
                    free_chunks = 0;
                }
            }
            return {};
        }

        return min(free_chunks, max_length);
    }

    Optional<size_t> find_longest_range_of_unset_bits(size_t max_length, size_t& found_range_size) const
    {
        size_t start = 0;
        size_t max_region_start = 0;
        size_t max_region_size = 0;

        while (true) {
            // Look for the next block which is bigger than currunt.
            auto length_of_found_range = find_next_range_of_unset_bits(start, max_region_size + 1, max_length);
            if (length_of_found_range.has_value()) {
                max_region_start = start;
                max_region_size = length_of_found_range.value();
                start += max_region_size;
            } else {
                // No ranges which are bigger than current were found.
                break;
            }
        }

        found_range_size = max_region_size;
        if (max_region_size != 0) {
            return max_region_start;
        }
        return {};
    }

    Optional<size_t> find_first_fit(size_t minimum_length) const
    {
        size_t start = 0;
        auto length_of_found_range = find_next_range_of_unset_bits(start, minimum_length, minimum_length);
        if (length_of_found_range.has_value()) {
            return start;
        }
        return {};
    }

    Optional<size_t> find_best_fit(size_t minimum_length) const
    {
        size_t start = 0;
        size_t best_region_start = 0;
        size_t best_region_size = max_size;
        bool found = false;

        while (true) {
            // Look for the next block which is bigger than requested length.
            auto length_of_found_range = find_next_range_of_unset_bits(start, minimum_length, best_region_size);
            if (length_of_found_range.has_value()) {
                if (best_region_size > length_of_found_range.value() || !found) {
                    best_region_start = start;
                    best_region_size = length_of_found_range.value();
                    found = true;
                }
                start += length_of_found_range.value();
            } else {
                // There are no ranges which can fit requested length.
                break;
            }
        }

        if (found) {
            return best_region_start;
        }
        return {};
    }

    static constexpr size_t max_size = 0xffffffff;

protected:
    u8* m_data { nullptr };
    size_t m_size { 0 };
};

}

using AK::BitmapView;
