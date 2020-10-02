/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/Noncopyable.h>
#include <AK/Optional.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

class Bitmap {
    AK_MAKE_NONCOPYABLE(Bitmap);

public:
    // NOTE: A wrapping Bitmap won't try to free the wrapped data.
    static Bitmap wrap(u8* data, size_t size)
    {
        return Bitmap(data, size);
    }

    static Bitmap create(size_t size, bool default_value = 0)
    {
        return Bitmap(size, default_value);
    }

    static Bitmap create()
    {
        return Bitmap();
    }

    Bitmap(Bitmap&& other)
    {
        m_owned = exchange(other.m_owned, false);
        m_data = exchange(other.m_data, nullptr);
        m_size = exchange(other.m_size, 0);
    }

    Bitmap& operator=(Bitmap&& other)
    {
        if (this != &other) {
            if (m_owned)
                kfree(m_data);
            m_owned = exchange(other.m_owned, false);
            m_data = exchange(other.m_data, nullptr);
            m_size = exchange(other.m_size, 0);
        }
        return *this;
    }

    ~Bitmap()
    {
        if (m_owned)
            kfree(m_data);
        m_data = nullptr;
    }

    size_t size() const { return m_size; }
    bool get(size_t index) const
    {
        ASSERT(index < m_size);
        return 0 != (m_data[index / 8] & (1u << (index % 8)));
    }
    void set(size_t index, bool value) const
    {
        ASSERT(index < m_size);
        if (value)
            m_data[index / 8] |= static_cast<u8>((1u << (index % 8)));
        else
            m_data[index / 8] &= static_cast<u8>(~(1u << (index % 8)));
    }

    size_t count_slow(bool value) const
    {
        return count_in_range(0, m_size, value);
    }

    size_t count_in_range(size_t start, size_t len, bool value) const
    {
        ASSERT(start < m_size);
        ASSERT(start + len <= m_size);
        if (len == 0)
            return 0;

        static const u8 bitmask_first_byte[8] = { 0xFF, 0xFE, 0xFC, 0xF8, 0xF0, 0xE0, 0xC0, 0x80 };
        static const u8 bitmask_last_byte[8] = { 0x0, 0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F };

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
            byte = *last;
            byte &= bitmask_last_byte[(start + len) % 8];
            count += __builtin_popcount(byte);
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

    u8* data() { return m_data; }
    const u8* data() const { return m_data; }

    void grow(size_t size, bool default_value)
    {
        ASSERT(m_owned);
        ASSERT(size > m_size);

        auto previous_size_bytes = size_in_bytes();
        auto previous_size = m_size;
        auto previous_data = m_data;

        m_size = size;
        m_data = reinterpret_cast<u8*>(kmalloc(size_in_bytes()));

        fill(default_value);

        if (previous_data != nullptr) {
            __builtin_memcpy(m_data, previous_data, previous_size_bytes);
            if (previous_size % 8)
                set_range(previous_size, 8 - previous_size % 8, default_value);
            kfree(previous_data);
        }
    }

    template<bool VALUE>
    void set_range(size_t start, size_t len)
    {
        ASSERT(start < m_size);
        ASSERT(start + len <= m_size);
        if (len == 0)
            return;

        static const u8 bitmask_first_byte[8] = { 0xFF, 0xFE, 0xFC, 0xF8, 0xF0, 0xE0, 0xC0, 0x80 };
        static const u8 bitmask_last_byte[8] = { 0x0, 0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F };

        u8* first = &m_data[start / 8];
        u8* last = &m_data[(start + len) / 8];
        u8 byte_mask = bitmask_first_byte[start % 8];
        if (first == last) {
            byte_mask &= bitmask_last_byte[(start + len) % 8];
            if constexpr (VALUE)
                *first |= byte_mask;
            else
                *first &= ~byte_mask;
        } else {
            if constexpr (VALUE)
                *first |= byte_mask;
            else
                *first &= ~byte_mask;
            byte_mask = bitmask_last_byte[(start + len) % 8];
            if constexpr (VALUE)
                *last |= byte_mask;
            else
                *last &= ~byte_mask;
            if (++first < last) {
                if constexpr (VALUE)
                    __builtin_memset(first, 0xFF, last - first);
                else
                    __builtin_memset(first, 0x0, last - first);
            }
        }
    }

    void set_range(size_t start, size_t len, bool value)
    {
        if (value)
            set_range<true>(start, len);
        else
            set_range<false>(start, len);
    }

    void fill(bool value)
    {
        __builtin_memset(m_data, value ? 0xff : 0x00, size_in_bytes());
    }

    template<bool VALUE>
    Optional<size_t> find_one_anywhere(size_t hint = 0) const
    {
        ASSERT(hint < m_size);
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
                    ASSERT(byte != 0);
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
                ASSERT(i <= byte_count);
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
                ASSERT(byte != 0);
                return i * 8 + __builtin_ffs(byte) - 1;
            }

            // NOTE: We don't really care about byte ordering. We found *one*
            // free bit, just calculate the position and return it
            val32 = *ptr32;
            if constexpr (!VALUE)
                val32 = ~val32;
            ASSERT(val32 != 0);
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
        ASSERT(byte != 0);
        return i * 8 + __builtin_ffs(byte) - 1;
    }

    Optional<size_t> find_first_set() const
    {
        return find_first<true>();
    }
    Optional<size_t> find_first_unset() const
    {
        return find_first<false>();
    }

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
                    if (!free_chunks)
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
        if (max_region_size) {
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

    Bitmap()
        : m_size(0)
        , m_owned(true)
    {
        m_data = nullptr;
    }

    Bitmap(size_t size, bool default_value)
        : m_size(size)
        , m_owned(true)
    {
        ASSERT(m_size != 0);
        m_data = reinterpret_cast<u8*>(kmalloc(size_in_bytes()));
        fill(default_value);
    }

    Bitmap(u8* data, size_t size)
        : m_data(data)
        , m_size(size)
        , m_owned(false)
    {
    }

    static constexpr u32 max_size = 0xffffffff;

private:
    size_t size_in_bytes() const { return ceil_div(m_size, static_cast<size_t>(8)); }

    u8* m_data { nullptr };
    size_t m_size { 0 };
    bool m_owned { false };
};

}

using AK::Bitmap;
