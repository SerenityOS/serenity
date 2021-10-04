/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Endian.h>
#include <AK/Types.h>
#include <string.h>

namespace Gfx {

class Streamer {
public:
    constexpr Streamer(const u8* data, size_t size)
        : m_data_ptr(data)
        , m_size_remaining(size)
    {
    }

    template<typename T>
    constexpr bool read(T& value)
    {
        Array<u8, sizeof(T)> network_buffer {};
        auto network_value = new (network_buffer.data()) AK::NetworkOrdered<T> {};
        auto res = read_bytes(network_buffer.data(), sizeof(T));
        value = T(*network_value);
        return res;
    }

    constexpr bool read_bytes(u8* buffer, size_t count)
    {
        if (m_size_remaining < count) {
            return false;
        }
        memcpy(buffer, m_data_ptr, count);
        m_data_ptr += count;
        m_size_remaining -= count;
        return true;
    }

    constexpr bool at_end() const { return !m_size_remaining; }

    constexpr void step_back()
    {
        m_data_ptr -= 1;
        m_size_remaining += 1;
    }

private:
    const u8* m_data_ptr { nullptr };
    size_t m_size_remaining { 0 };
};

}
