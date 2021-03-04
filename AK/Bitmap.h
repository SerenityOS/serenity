/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/BitmapView.h>
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
    Bitmap() = default;

    Bitmap(size_t size, bool default_value)
        : m_size(size)
    {
        VERIFY(m_size != 0);
        m_data = static_cast<u8*>(kmalloc(size_in_bytes()));
        fill(default_value);
    }

    Bitmap(u8* data, size_t size)
        : m_data(data)
        , m_size(size)
    {
    }

    BitmapView view() { return { m_data, m_size }; }
    const BitmapView view() const { return { m_data, m_size }; }

    Bitmap(Bitmap&& other)
        : m_data(exchange(other.m_data, nullptr))
        , m_size(exchange(other.m_size, 0))
    {
    }

    Bitmap& operator=(Bitmap&& other)
    {
        if (this != &other) {
            kfree(m_data);
            m_data = exchange(other.m_data, nullptr);
            m_size = exchange(other.m_size, 0);
        }
        return *this;
    }

    ~Bitmap()
    {
        kfree(m_data);
        m_data = nullptr;
    }

    size_t size() const { return m_size; }
    size_t size_in_bytes() const { return ceil_div(m_size, static_cast<size_t>(8)); }

    bool get(size_t index) const
    {
        VERIFY(index < m_size);
        return 0 != (m_data[index / 8] & (1u << (index % 8)));
    }

    void set(size_t index, bool value) const
    {
        VERIFY(index < m_size);
        if (value)
            m_data[index / 8] |= static_cast<u8>((1u << (index % 8)));
        else
            m_data[index / 8] &= static_cast<u8>(~(1u << (index % 8)));
    }

    size_t count_slow(bool value) const { return count_in_range(0, m_size, value); }
    size_t count_in_range(size_t start, size_t len, bool value) const { return view().count_in_range(start, len, value); }

    bool is_null() const { return !m_data; }

    u8* data() { return m_data; }
    const u8* data() const { return m_data; }

    void grow(size_t size, bool default_value)
    {
        VERIFY(size > m_size);

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
    void set_range(size_t start, size_t len) { return view().set_range<VALUE>(start, len); }
    void set_range(size_t start, size_t len, bool value) { return view().set_range(start, len, value); }

    void fill(bool value) { view().fill(value); }

    Optional<size_t> find_one_anywhere_set(size_t hint = 0) const { return view().find_one_anywhere<true>(hint); }
    Optional<size_t> find_one_anywhere_unset(size_t hint = 0) const { return view().find_one_anywhere<false>(hint); }

    Optional<size_t> find_first_set() const { return view().find_first<true>(); }
    Optional<size_t> find_first_unset() const { return view().find_first<false>(); }

    Optional<size_t> find_next_range_of_unset_bits(size_t& from, size_t min_length = 1, size_t max_length = max_size) const
    {
        return view().find_next_range_of_unset_bits(from, min_length, max_length);
    }

    Optional<size_t> find_longest_range_of_unset_bits(size_t max_length, size_t& found_range_size) const
    {
        return view().find_longest_range_of_unset_bits(max_length, found_range_size);
    }

    Optional<size_t> find_first_fit(size_t minimum_length) const { return view().find_first_fit(minimum_length); }
    Optional<size_t> find_best_fit(size_t minimum_length) const { return view().find_best_fit(minimum_length); }

    static constexpr size_t max_size = 0xffffffff;

private:
    u8* m_data { nullptr };
    size_t m_size { 0 };
};

}

using AK::Bitmap;
