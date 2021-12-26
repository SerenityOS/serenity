/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    Bitmap(u8* data, size_t size, bool is_owning = false)
        : m_data(data)
        , m_size(size)
        , m_is_owning(is_owning)
    {
    }

    [[nodiscard]] BitmapView view() { return { m_data, m_size }; }
    [[nodiscard]] BitmapView const view() const { return { m_data, m_size }; }

    Bitmap(Bitmap&& other)
        : m_data(exchange(other.m_data, nullptr))
        , m_size(exchange(other.m_size, 0))
    {
    }

    Bitmap& operator=(Bitmap&& other)
    {
        if (this != &other) {
            kfree_sized(m_data, size_in_bytes());
            m_data = exchange(other.m_data, nullptr);
            m_size = exchange(other.m_size, 0);
        }
        return *this;
    }

    ~Bitmap()
    {
        if (m_is_owning) {
            kfree_sized(m_data, size_in_bytes());
        }
        m_data = nullptr;
    }

    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] size_t size_in_bytes() const { return ceil_div(m_size, static_cast<size_t>(8)); }

    [[nodiscard]] bool get(size_t index) const
    {
        VERIFY(index < m_size);
        return 0 != (m_data[index / 8] & (1u << (index % 8)));
    }

    void set(size_t index, bool value)
    {
        VERIFY(index < m_size);
        if (value)
            m_data[index / 8] |= static_cast<u8>((1u << (index % 8)));
        else
            m_data[index / 8] &= static_cast<u8>(~(1u << (index % 8)));
    }

    [[nodiscard]] size_t count_slow(bool value) const { return count_in_range(0, m_size, value); }
    [[nodiscard]] size_t count_in_range(size_t start, size_t len, bool value) const { return view().count_in_range(start, len, value); }

    [[nodiscard]] bool is_null() const { return !m_data; }

    [[nodiscard]] u8* data() { return m_data; }
    [[nodiscard]] u8 const* data() const { return m_data; }

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
            kfree_sized(previous_data, previous_size_bytes);
        }
    }

    template<bool VALUE, bool verify_that_all_bits_flip = false>
    void set_range(size_t start, size_t len)
    {
        VERIFY(start < m_size);
        VERIFY(start + len <= m_size);
        if (len == 0)
            return;

        u8* first = &m_data[start / 8];
        u8* last = &m_data[(start + len) / 8];
        u8 byte_mask = bitmask_first_byte[start % 8];
        if (first == last) {
            byte_mask &= bitmask_last_byte[(start + len) % 8];
            if constexpr (verify_that_all_bits_flip) {
                if constexpr (VALUE) {
                    VERIFY((*first & byte_mask) == 0);
                } else {
                    VERIFY((*first & byte_mask) == byte_mask);
                }
            }
            if constexpr (VALUE)
                *first |= byte_mask;
            else
                *first &= ~byte_mask;
        } else {
            if constexpr (verify_that_all_bits_flip) {
                if constexpr (VALUE) {
                    VERIFY((*first & byte_mask) == 0);
                } else {
                    VERIFY((*first & byte_mask) == byte_mask);
                }
            }
            if constexpr (VALUE)
                *first |= byte_mask;
            else
                *first &= ~byte_mask;
            byte_mask = bitmask_last_byte[(start + len) % 8];
            if constexpr (verify_that_all_bits_flip) {
                if constexpr (VALUE) {
                    VERIFY((*last & byte_mask) == 0);
                } else {
                    VERIFY((*last & byte_mask) == byte_mask);
                }
            }
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
            set_range<true, false>(start, len);
        else
            set_range<false, false>(start, len);
    }

    void set_range_and_verify_that_all_bits_flip(size_t start, size_t len, bool value)
    {
        if (value)
            set_range<true, true>(start, len);
        else
            set_range<false, true>(start, len);
    }

    void fill(bool value)
    {
        __builtin_memset(m_data, value ? 0xff : 0x00, size_in_bytes());
    }

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
    bool m_is_owning { true };
};

}

using AK::Bitmap;
