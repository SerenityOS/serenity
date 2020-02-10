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
#include <AK/String.h>
#include <AK/Types.h>
#include <LibBareMetal/StdLib.h>

class [[gnu::packed]] MACAddress
{
public:
    MACAddress() {}
    MACAddress(const u8 data[6])
    {
        memcpy(m_data, data, 6);
    }
    MACAddress(u8 a, u8 b, u8 c, u8 d, u8 e, u8 f)
    {
        m_data[0] = a;
        m_data[1] = b;
        m_data[2] = c;
        m_data[3] = d;
        m_data[4] = e;
        m_data[5] = f;
    }
    ~MACAddress() {}

    u8 operator[](int i) const
    {
        ASSERT(i >= 0 && i < 6);
        return m_data[i];
    }

    bool operator==(const MACAddress& other) const
    {
        return !memcmp(m_data, other.m_data, sizeof(m_data));
    }

    String to_string() const
    {
        return String::format("%02x:%02x:%02x:%02x:%02x:%02x", m_data[0], m_data[1], m_data[2], m_data[3], m_data[4], m_data[5]);
    }

    bool is_zero() const
    {
        return m_data[0] == 0 && m_data[1] == 0 && m_data[2] == 0 && m_data[3] == 0 && m_data[4] == 0 && m_data[5] == 0;
    }

private:
    u8 m_data[6];
};

static_assert(sizeof(MACAddress) == 6);

namespace AK {

template<>
struct Traits<MACAddress> : public GenericTraits<MACAddress> {
    static unsigned hash(const MACAddress& address) { return string_hash((const char*)&address, sizeof(address)); }
};

}
