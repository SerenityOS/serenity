#pragma once

#include "ByteBuffer.h"

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

    void operator<<(const String& value)
    {
        for (unsigned i = 0; i < value.length(); ++i)
            m_buffer[m_offset++] = value[i];
    }

    void fillToEnd(byte ch)
    {
        while (m_offset < m_buffer.size())
            m_buffer[m_offset++] = ch;
    }

    Unix::size_t offset() const { return m_offset; }

private:
    ByteBuffer& m_buffer;
    Unix::size_t m_offset { 0 };
};

}

using AK::BufferStream;
