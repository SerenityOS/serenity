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

#include <AK/Endian.h>
#include <AK/LogStream.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

typedef u32 in_addr_t;

namespace AK {

class [[gnu::packed]] IPv4Address
{
public:
    IPv4Address() { }
    IPv4Address(const u8 data[4])
    {
        m_data[0] = data[0];
        m_data[1] = data[1];
        m_data[2] = data[2];
        m_data[3] = data[3];
    }
    IPv4Address(u8 a, u8 b, u8 c, u8 d)
    {
        m_data[0] = a;
        m_data[1] = b;
        m_data[2] = c;
        m_data[3] = d;
    }
    IPv4Address(NetworkOrdered<u32> address)
        : m_data_as_u32(address)
    {
    }

    u8 operator[](int i) const
    {
        ASSERT(i >= 0 && i < 4);
        return m_data[i];
    }

    String to_string() const
    {
        return String::formatted("{}.{}.{}.{}", m_data[0], m_data[1], m_data[2], m_data[3]);
    }

    static Optional<IPv4Address> from_string(const StringView& string)
    {
        if (string.is_null())
            return {};
        auto parts = string.split_view('.');

        u32 a;
        u32 b;
        u32 c;
        u32 d;

        if (parts.size() == 1) {
            a = 0;
            b = 0;
            c = 0;
            d = parts[0].to_uint().value_or(256);
        } else if (parts.size() == 2) {
            a = parts[0].to_uint().value_or(256);
            b = 0;
            c = 0;
            d = parts[1].to_uint().value_or(256);
        } else if (parts.size() == 3) {
            a = parts[0].to_uint().value_or(256);
            b = parts[1].to_uint().value_or(256);
            c = 0;
            d = parts[2].to_uint().value_or(256);
        } else if (parts.size() == 4) {
            a = parts[0].to_uint().value_or(256);
            b = parts[1].to_uint().value_or(256);
            c = parts[2].to_uint().value_or(256);
            d = parts[3].to_uint().value_or(256);
        } else {
            return {};
        }

        if (a > 255 || b > 255 || c > 255 || d > 255)
            return {};
        return IPv4Address((u8)a, (u8)b, (u8)c, (u8)d);
    }

    in_addr_t to_in_addr_t() const { return m_data_as_u32; }
    u32 to_u32() const { return m_data_as_u32; }

    bool operator==(const IPv4Address& other) const { return m_data_as_u32 == other.m_data_as_u32; }
    bool operator!=(const IPv4Address& other) const { return m_data_as_u32 != other.m_data_as_u32; }

    bool is_zero() const
    {
        return m_data_as_u32 == 0;
    }

private:
    union {
        u8 m_data[4];
        u32 m_data_as_u32 { 0 };
    };
};

static_assert(sizeof(IPv4Address) == 4);

template<>
struct Traits<IPv4Address> : public GenericTraits<IPv4Address> {
    static unsigned hash(const IPv4Address& address) { return string_hash((const char*)&address, sizeof(address)); }
};

inline const LogStream& operator<<(const LogStream& stream, const IPv4Address& value)
{
    return stream << value.to_string();
}

}

using AK::IPv4Address;
