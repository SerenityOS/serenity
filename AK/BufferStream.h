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

    void operator<<(byte value)
    {
        m_buffer[m_offset++] = value & 0xffu;
    }

    void operator<<(char value)
    {
        m_buffer[m_offset++] = (byte)value;
    }

    void operator<<(word value)
    {
        m_buffer[m_offset++] = value & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 8) & 0xffu;
    }

    void operator<<(dword value)
    {
        m_buffer[m_offset++] = value & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 8) & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 16) & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 24) & 0xffu;
    }

    void operator<<(const char* str)
    {
        ssize_t len = strlen(str);
        ASSERT(len >= 0);
        for (ssize_t i = 0; i < len; ++i)
            m_buffer[m_offset++] = str[i];
    }

    void operator<<(const String& value)
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

    void fill_to_end(byte ch)
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
