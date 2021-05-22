/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/ByteReader.h>
#include <AK/Endian.h>
#include <AK/Types.h>

namespace FTP {

class ByteWriter {
public:
    ByteWriter()
    {
        m_data = ByteBuffer::create_uninitialized(256);
    }

    inline void write_bytes(ReadonlyBytes data)
    {
        write_bytes(data.data(), data.size());
    }

    inline void write_1_bytes(u8 value)
    {
        write_bytes((const u8*)&value, 1);
    }

    inline void write_2_bytes(u16 value)
    {
        u8 buf[2];
        buf[0] = (value & 0xFF00) >> 8;
        buf[1] = value & 0xFF;

        write_bytes(buf, 2);
    }

    inline void write_3_bytes(u32 value)
    {
        u8 buf[3];
        buf[0] = (value & 0xFF0000) >> 16;
        buf[1] = (value & 0xFF00) >> 8;
        buf[2] = value & 0xFF;

        write_bytes(buf, 3);
    }

    inline void write_bytes(const u8* data, size_t bytes)
    {
        if (bytes == 0) {
            return;
        }

        auto old_length = m_current_length;
        m_current_length += bytes;

        if (m_data.size() < m_current_length) {
            m_data.resize(m_current_length);
        }

        m_data.overwrite(old_length, data, bytes);
    }

    inline ByteBuffer build()
    {
        return m_data.slice(0, m_current_length);
    }

    inline void set_1_bytes(size_t offset, u8 value)
    {
        m_data[offset] = value;
    }

    inline void set_2_bytes(size_t offset, u16 value)
    {
        m_data[offset] = (value & 0xFF00) >> 8;
        m_data[offset + 1] = value & 0xFF;
    }

    inline void set_3_bytes(size_t offset, u32 value)
    {
        m_data[offset] = (value & 0xFF0000) >> 16;
        m_data[offset + 1] = (value & 0xFF00) >> 8;
        m_data[offset + 2] = value & 0xFF;
    }

    size_t length() const { return m_current_length; }

private:
    ByteBuffer m_data;
    size_t m_current_length {};
};
}
