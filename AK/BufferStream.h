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

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

class BufferStream {
public:
    explicit BufferStream(ByteBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    ~BufferStream()
    {
        ASSERT(!m_read_failure);
    }

    BufferStream& operator<<(i8 value)
    {
        m_buffer[m_offset++] = value;
        return *this;
    }
    BufferStream& operator>>(i8& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        value = m_buffer[m_offset++];
        return *this;
    }

    BufferStream& operator<<(u8 value)
    {
        m_buffer[m_offset++] = value;
        return *this;
    }
    BufferStream& operator>>(u8& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        value = m_buffer[m_offset++];
        return *this;
    }

    BufferStream& operator<<(bool value)
    {
        m_buffer[m_offset++] = value;
        return *this;
    }
    BufferStream& operator>>(bool& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        value = m_buffer[m_offset++];
        return *this;
    }

    BufferStream& operator<<(float value)
    {
        union bits {
            float as_float;
            u32 as_u32;
        } u;
        u.as_float = value;
        return *this << u.as_u32;
    }

    BufferStream& operator>>(float& value)
    {
        union bits {
            float as_float;
            u32 as_u32;
        } u;
        *this >> u.as_u32;
        if (m_read_failure)
            return *this;
        value = u.as_float;
        return *this;
    }

    BufferStream& operator<<(char value)
    {
        m_buffer[m_offset++] = (u8)value;
        return *this;
    }
    BufferStream& operator>>(char& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        value = (u8)m_buffer[m_offset++];
        return *this;
    }

    BufferStream& operator<<(u16 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        return *this;
    }
    BufferStream& operator>>(u16& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        value = 0;
        u8 b0 = m_buffer[m_offset++];
        u8 b1 = m_buffer[m_offset++];
        value |= (u8(b1) << 8);
        value |= (u8(b0));
        return *this;
    }

    BufferStream& operator<<(i16 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        return *this;
    }
    BufferStream& operator>>(i16& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        value = 0;
        u8 b0 = m_buffer[m_offset++];
        u8 b1 = m_buffer[m_offset++];
        value |= (u8(b1) << 8);
        value |= (u8(b0));
        return *this;
    }

    BufferStream& operator<<(u32 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        m_buffer[m_offset++] = (u8)(value >> 16);
        m_buffer[m_offset++] = (u8)(value >> 24);
        return *this;
    }
    BufferStream& operator>>(u32& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        u8 b0 = m_buffer[m_offset++];
        u8 b1 = m_buffer[m_offset++];
        u8 b2 = m_buffer[m_offset++];
        u8 b3 = m_buffer[m_offset++];

        value = 0;
        value |= (u8(b3) << 24);
        value |= (u8(b2) << 16);
        value |= (u8(b1) << 8);
        value |= (u8(b0));
        return *this;
    }

    BufferStream& operator<<(i32 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        m_buffer[m_offset++] = (u8)(value >> 16);
        m_buffer[m_offset++] = (u8)(value >> 24);
        return *this;
    }
    BufferStream& operator>>(i32& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        u8 b0 = m_buffer[m_offset++];
        u8 b1 = m_buffer[m_offset++];
        u8 b2 = m_buffer[m_offset++];
        u8 b3 = m_buffer[m_offset++];

        value = 0;
        value |= (u8(b3) << 24);
        value |= (u8(b2) << 16);
        value |= (u8(b1) << 8);
        value |= (u8(b0));
        return *this;
    }

    BufferStream& operator<<(u64 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        m_buffer[m_offset++] = (u8)(value >> 16);
        m_buffer[m_offset++] = (u8)(value >> 24);
        m_buffer[m_offset++] = (u8)(value >> 32);
        m_buffer[m_offset++] = (u8)(value >> 40);
        m_buffer[m_offset++] = (u8)(value >> 48);
        m_buffer[m_offset++] = (u8)(value >> 56);
        return *this;
    }
    BufferStream& operator>>(u64& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        u8 b0 = m_buffer[m_offset++];
        u8 b1 = m_buffer[m_offset++];
        u8 b2 = m_buffer[m_offset++];
        u8 b3 = m_buffer[m_offset++];
        u8 b4 = m_buffer[m_offset++];
        u8 b5 = m_buffer[m_offset++];
        u8 b6 = m_buffer[m_offset++];
        u8 b7 = m_buffer[m_offset++];

        value = 0;
        value |= ((long long)b7 << 56);
        value |= ((long long)b6 << 48);
        value |= ((long long)b5 << 40);
        value |= ((long long)b4 << 32);
        value |= ((long long)b3 << 24);
        value |= ((long long)b2 << 16);
        value |= ((long long)b1 << 8);
        value |= ((long long)b0);
        return *this;
    }
    BufferStream& operator<<(i64 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        m_buffer[m_offset++] = (u8)(value >> 16);
        m_buffer[m_offset++] = (u8)(value >> 24);
        m_buffer[m_offset++] = (u8)(value >> 32);
        m_buffer[m_offset++] = (u8)(value >> 40);
        m_buffer[m_offset++] = (u8)(value >> 48);
        m_buffer[m_offset++] = (u8)(value >> 56);
        return *this;
    }
    BufferStream& operator>>(i64& value)
    {
        if (m_offset + sizeof(value) > unsigned(m_buffer.size())) {
            m_read_failure = true;
            return *this;
        }
        u8 b0 = m_buffer[m_offset++];
        u8 b1 = m_buffer[m_offset++];
        u8 b2 = m_buffer[m_offset++];
        u8 b3 = m_buffer[m_offset++];
        u8 b4 = m_buffer[m_offset++];
        u8 b5 = m_buffer[m_offset++];
        u8 b6 = m_buffer[m_offset++];
        u8 b7 = m_buffer[m_offset++];

        value = 0;
        value |= ((long long)b7 << 56);
        value |= ((long long)b6 << 48);
        value |= ((long long)b5 << 40);
        value |= ((long long)b4 << 32);
        value |= ((long long)b3 << 24);
        value |= ((long long)b2 << 16);
        value |= ((long long)b1 << 8);
        value |= ((long long)b0);
        return *this;
    }

    BufferStream& operator<<(const char* value)
    {
        return *this << StringView(value);
    }

    BufferStream& operator<<(const StringView& value)
    {
        for (size_t i = 0; i < value.length(); ++i)
            m_buffer[m_offset++] = value[i];
        return *this;
    }

    BufferStream& operator<<(const ByteBuffer& value)
    {
        for (size_t i = 0; i < value.size(); ++i)
            m_buffer[m_offset++] = value[i];
        return *this;
    }

    BufferStream& read_raw(u8* raw_data, size_t size)
    {
        if (m_offset + size > m_buffer.size()) {
            m_read_failure = true;
            return *this;
        }
        __builtin_memcpy(raw_data, m_buffer.data() + m_offset, size);
        m_offset += size;
        return *this;
    };

    u8 peek()
    {
        if (m_offset >= m_buffer.size()) {
            m_read_failure = true;
            return 0;
        }
        return m_buffer[m_offset];
    }

    BufferStream& operator>>(String& str)
    {
        if (m_offset >= m_buffer.size()) {
            m_read_failure = true;
            return *this;
        }
        size_t string_size = 0;
        while (m_offset + string_size < m_buffer.size() && m_buffer[m_offset + string_size]) {
            ++string_size;
        }
        str = String(reinterpret_cast<const char*>(&m_buffer[m_offset]), string_size);
        m_offset += string_size + 1;
        return *this;
    }

    // LEB128 is a variable-length encoding for integers
    BufferStream& read_LEB128_unsigned(size_t& result)
    {
        result = 0;
        size_t num_bytes = 0;
        while (true) {
            if (m_offset > m_buffer.size()) {
                m_read_failure = true;
                break;
            }
            const u8 byte = m_buffer[m_offset];
            result = (result) | (static_cast<size_t>(byte & ~(1 << 7)) << (num_bytes * 7));
            ++m_offset;
            if (!(byte & (1 << 7)))
                break;
            ++num_bytes;
        }

        return *this;
    }

    // LEB128 is a variable-length encoding for integers
    BufferStream& read_LEB128_signed(ssize_t& result)
    {
        result = 0;
        size_t num_bytes = 0;
        u8 byte = 0;
        do {
            if (m_offset > m_buffer.size()) {
                m_read_failure = true;
                break;
            }
            byte = m_buffer[m_offset];
            result = (result) | (static_cast<size_t>(byte & ~(1 << 7)) << (num_bytes * 7));
            ++m_offset;
            ++num_bytes;
        } while (byte & (1 << 7));
        if (num_bytes * 7 < sizeof(size_t) * 4 && (byte & 0x40)) {
            // sign extend
            result |= ((size_t)(-1) << (num_bytes * 7));
        }
        return *this;
    }

    BufferStream& advance(size_t amount)
    {
        if (m_offset + amount > m_buffer.size()) {
            m_read_failure = true;
        } else {
            m_offset += amount;
        }
        return *this;
    }

    bool at_end() const
    {
        return m_offset == m_buffer.size();
    }

    void fill_to_end(u8 ch)
    {
        while (!at_end())
            m_buffer[m_offset++] = ch;
    }

    ssize_t offset() const { return m_offset; }

    void snip()
    {
        m_buffer.trim(m_offset);
    }

    void reset()
    {
        m_offset = 0;
        m_read_failure = false;
    }

    bool handle_read_failure()
    {
        bool old = m_read_failure;
        m_read_failure = false;
        return old;
    }

private:
    ByteBuffer& m_buffer;
    size_t m_offset { 0 };
    bool m_read_failure { false };
};

}

using AK::BufferStream;
