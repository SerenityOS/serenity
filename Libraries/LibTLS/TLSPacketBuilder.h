/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
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

#include <AK/ByteBuffer.h>
#include <AK/Endian.h>
#include <AK/Types.h>

namespace TLS {

enum class MessageType : u8 {
    ChangeCipher = 0x14,
    Alert = 0x15,
    Handshake = 0x16,
    ApplicationData = 0x17,
};

enum class Version : u16 {
    V10 = 0x0301,
    V11 = 0x0302,
    V12 = 0x0303,
    V13 = 0x0304
};

class PacketBuilder {
public:
    PacketBuilder(MessageType type, u16 version, size_t size_hint = 0xfdf)
        : PacketBuilder(type, (Version)version, size_hint)
    {
    }

    PacketBuilder(MessageType type, Version version, size_t size_hint = 0xfdf)
    {
        m_packet_data = ByteBuffer::create_uninitialized(size_hint + 16);
        m_current_length = 5;
        m_packet_data[0] = (u8)type;
        *(u16*)m_packet_data.offset_pointer(1) = AK::convert_between_host_and_network_endian((u16)version);
    }

    inline void append(u16 value)
    {
        value = AK::convert_between_host_and_network_endian(value);
        append((const u8*)&value, sizeof(value));
    }
    inline void append(u8 value)
    {
        append((const u8*)&value, sizeof(value));
    }
    inline void append(const ByteBuffer& data)
    {
        append(data.data(), data.size());
    }
    inline void append_u24(u32 value)
    {
        u8 buf[3];
        buf[0] = value / 0x10000;
        value %= 0x10000;
        buf[1] = value / 0x100;
        value %= 0x100;
        buf[2] = value;

        append(buf, 3);
    }
    inline void append(const u8* data, size_t bytes)
    {
        if (bytes == 0)
            return;

        auto old_length = m_current_length;
        m_current_length += bytes;

        if (m_packet_data.size() < m_current_length) {
            m_packet_data.grow(m_current_length);
        }

        m_packet_data.overwrite(old_length, data, bytes);
    }
    inline ByteBuffer build()
    {
        auto length = m_current_length;
        m_current_length = 0;
        return m_packet_data.slice(0, length);
    }
    inline void set(size_t offset, u8 value)
    {
        ASSERT(offset < m_current_length);
        m_packet_data[offset] = value;
    }
    size_t length() const { return m_current_length; }

private:
    ByteBuffer m_packet_data;
    size_t m_current_length;
};

}
