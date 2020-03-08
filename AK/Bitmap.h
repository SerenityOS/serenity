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
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

class Bitmap {
    AK_MAKE_NONCOPYABLE(Bitmap)
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

    u8* data() { return m_data; }
    const u8* data() const { return m_data; }

    void grow(size_t size, bool default_value)
    {
        ASSERT(size > m_size);

        auto previous_size_bytes = size_in_bytes();
        auto previous_size = m_size;
        auto previous_data = m_data;

        m_size = size;
        m_data = reinterpret_cast<u8*>(kmalloc(size_in_bytes()));

        fill(default_value);

        if (previous_data != nullptr) {
            __builtin_memcpy(m_data, previous_data, previous_size_bytes);

            if ((previous_size % 8) != 0) {
                if (default_value)
                    m_data[previous_size_bytes - 1] |= (0xff >> (previous_size % 8));
                else
                    m_data[previous_size_bytes - 1] &= ~(0xff >> (previous_size % 8));
            }

            kfree(previous_data);
        }
    }

    void fill(bool value)
    {
        __builtin_memset(m_data, value ? 0xff : 0x00, size_in_bytes());
    }

    Optional<size_t> find_first_set() const
    {
        size_t i = 0;
        while (i < m_size / 8 && m_data[i] == 0x00)
            i++;

        size_t j = 0;
        for (j = i * 8; j < m_size; j++) {
            if (get(j))
                return j;
        }

        return {};
    }

    Optional<size_t> find_first_unset() const
    {
        size_t i = 0;
        while (i < m_size / 8 && m_data[i] == 0xff)
            i++;

        size_t j = 0;
        for (j = i * 8; j < m_size; j++)
            if (!get(j))
                return j;

        return {};
    }

    Optional<size_t> find_longest_range_of_unset_bits(size_t max_length, size_t& found_range_size) const
    {
        auto first_index = find_first_unset();
        if (!first_index.has_value())
            return {};

        size_t free_region_start = first_index.value();
        size_t free_region_size = 1;

        size_t max_region_start = free_region_start;
        size_t max_region_size = free_region_size;

        // Let's try and find the best fit possible
        for (size_t j = first_index.value() + 1; j < m_size && free_region_size < max_length; j++) {
            if (!get(j)) {
                if (free_region_size == 0)
                    free_region_start = j;
                free_region_size++;
            } else {
                if (max_region_size < free_region_size) {
                    max_region_size = free_region_size;
                    max_region_start = free_region_start;
                }
                free_region_start = 0;
                free_region_size = 0;
            }
        }

        if (max_region_size < free_region_size) {
            max_region_size = free_region_size;
            max_region_start = free_region_start;
        }

        found_range_size = max_region_size;
        if (max_region_size > 1)
            return max_region_start;
        // if the max free region size is one, then return the earliest one found
        return first_index;
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

private:
    size_t size_in_bytes() const { return ceil_div(m_size, 8); }

    u8* m_data { nullptr };
    size_t m_size { 0 };
    bool m_owned { false };
};

}

using AK::Bitmap;
