/*
 * Copyright (c) 2020, SerenityOS Developers
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

#include "BufferStream.h"
#include <AK/ByteBuffer.h>

#define MIN(a, b) (a < b ? a : b)

namespace AK {

u8 constexpr bitmask[9] = { 0xFF, 0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF };

// This implementation is not compatible with big-endian devices.
class BinaryStream {
public:
    BinaryStream(ByteBuffer& buffer)
        : m_buffer(buffer)
        , m_stream(BufferStream(buffer))
    {
    }

    ~BinaryStream()
    {
        ASSERT(!m_failed);
    }

    bool ensure_bytes(size_t bytes)
    {
        return (m_stream.offset() + bytes) <= m_buffer.size();
    }

    bool ensure_bits(size_t bits)
    {
        size_t full_bytes = m_buffer.size() - (m_stream.offset() + 1);
        u8 in_this_byte = (u8)8 - m_bit_offset;
        return (full_bytes * 8) + in_this_byte >= bits;
    }

    inline void byte_align_forward()
    {
        if (m_bit_offset > 0) {
            m_bit_offset = 0;
            m_stream >> m_current_byte;
        }
    }

    inline void byte_align_backward()
    {
        m_bit_offset = 0;
    }

    ALWAYS_INLINE size_t read_network_order_bits(size_t bit_count)
    {
        if (bit_count > (sizeof(size_t) * 8) || !ensure_bits(bit_count)) {
            set_failed();
            return 0;
        }

        size_t number = 0;
        while (bit_count) {
            if (m_bit_offset > 7) {
                m_bit_offset = 0;
                m_stream >> m_current_byte;
            }
            // Total bits readable from this byte given the bit offset.
            u8 read_count = (u8)MIN(bit_count, 8 - m_bit_offset);
            u8 shift_width = (u8)8 - (m_bit_offset + read_count);
            u8 this_byte = (m_current_byte & (bitmask[read_count] << shift_width)) >> shift_width;
            number = (number << read_count) | this_byte;
            bit_count -= read_count;
            m_bit_offset += read_count;
        }

        return number;
    }

    inline u8 read_u8()
    {
        return (u8)read_network_order_bits(8);
    }

    inline u16 read_network_order_u16()
    {
        return (u16)read_network_order_bits(16);
    }

    inline u32 read_network_order_u32()
    {
        return (u32)read_network_order_bits(32);
    }

    inline u64 read_network_order_u64()
    {
        return (u64)read_network_order_bits(64);
    }

    inline bool handle_read_failure()
    {
        bool old = m_failed;
        m_failed = false;
        return old || m_stream.handle_read_failure();
    }

    BinaryStream& operator>>(u8& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(u16& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(u32& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(u64& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(i8& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(i16& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(i32& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(i64& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    BinaryStream& operator>>(char& value)
    {
        m_stream >> value;
        invalidate_bit_offset();
        return *this;
    }

    // Returns byte offset.
    size_t offset()
    {
        return (size_t)m_stream.offset();
    }

    // Peeks a byte.
    u8 peek()
    {
        return m_stream.peek();
    }

    void reset()
    {
        m_stream.reset();
        invalidate_bit_offset();
    }

    void skip_bits(size_t amount)
    {
        read_network_order_bits(amount);
    }

    BinaryStream& skip_bytes(size_t amount)
    {
        m_stream.advance(amount);
        invalidate_bit_offset();
        return *this;
    }

private:
    inline void invalidate_bit_offset()
    {
        m_current_byte = 0;
        m_bit_offset = 8;
    }

    inline void set_failed()
    {
        m_failed = true;
    }

    ByteBuffer& m_buffer;
    BufferStream m_stream;
    u8 m_current_byte { 0 };
    u8 m_bit_offset { 8 };
    bool m_failed { false };
};

}

using AK::BinaryStream;
