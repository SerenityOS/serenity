/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/Random.h>
#include <AK/Types.h>

namespace FTP {

class ByteReader {
public:
    ByteReader(AK::InputMemoryStream stream)
        : m_stream(stream)
    {
    }

    u8 read_1_bytes()
    {
        u8 byte1 = {};

        m_stream >> byte1;

        return byte1;
    }

    u16 read_2_bytes()
    {
        u8 byte1 = {};
        u8 byte2 = {};

        m_stream >> byte1;
        m_stream >> byte2;

        return (byte2 | byte1 << 8);
    }

    u32 read_3_bytes()
    {
        u8 byte1 = {};
        u8 byte2 = {};
        u8 byte3 = {};

        m_stream >> byte1;
        m_stream >> byte2;
        m_stream >> byte3;

        return (byte3 | byte2 << 8 | byte1 << 16);
    }

    ByteBuffer read_bytes(size_t count)
    {
        u32 read {};
        u8 byte;

        ByteBuffer output = ByteBuffer::create_zeroed(count);

        while (!m_stream.eof() && read < count) {
            m_stream >> byte;
            output.overwrite(read, (const u8*)&byte, 1);
            read++;
        }

        return output;
    }

    size_t remaining()
    {
        return m_stream.remaining();
    }

private:
    AK::InputMemoryStream m_stream;
};
}
