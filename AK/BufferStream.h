#pragma once

#include <AK/ByteBuffer.h>
#include <AK/String.h>

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

    BufferStream& operator<<(size_t value)
    {
        return *this << (u32)value;
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
        for (ssize_t i = 0; i < value.size(); ++i)
            m_buffer[m_offset++] = value[i];
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
    ssize_t m_offset { 0 };
    bool m_read_failure { false };
};

}

using AK::BufferStream;
