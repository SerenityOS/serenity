/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/ByteReader.h>
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
        // FIXME: Handle possible OOM situation.
        m_packet_data = ByteBuffer::create_uninitialized(size_hint + 16).release_value();
        m_current_length = 5;
        m_packet_data[0] = (u8)type;
        ByteReader::store(m_packet_data.offset_pointer(1), AK::convert_between_host_and_network_endian((u16)version));
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
    inline void append(ReadonlyBytes data)
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
            m_packet_data.resize(m_current_length);
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
        VERIFY(offset < m_current_length);
        m_packet_data[offset] = value;
    }
    size_t length() const { return m_current_length; }

private:
    ByteBuffer m_packet_data;
    size_t m_current_length;
};

}
