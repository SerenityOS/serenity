#pragma once

#include <AK/AKString.h>
#include <AK/ByteBuffer.h>

namespace AK {

class BufferStream {
public:
    explicit BufferStream(ByteBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    void operator<<(u8 value)
    {
        m_buffer[m_offset++] = value;
    }

    void operator<<(char value)
    {
        m_buffer[m_offset++] = (u8)value;
    }

    void operator<<(u16 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
    }

    void operator<<(u32 value)
    {
        m_buffer[m_offset++] = value;
        m_buffer[m_offset++] = (u8)(value >> 8);
        m_buffer[m_offset++] = (u8)(value >> 16);
        m_buffer[m_offset++] = (u8)(value >> 24);
    }

    void operator<<(const StringView& value)
    {
        for (ssize_t i = 0; i < value.length(); ++i)
            m_buffer[m_offset++] = value[i];
    }

    void operator<<(const ByteBuffer& value)
    {
        for (ssize_t i = 0; i < value.size(); ++i)
            m_buffer[m_offset++] = value[i];
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

private:
    ByteBuffer& m_buffer;
    ssize_t m_offset { 0 };
};

}

using AK::BufferStream;
