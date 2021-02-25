/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>, the SerenityOS developers
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
