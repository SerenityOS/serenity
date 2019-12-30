#pragma once

#include <LibIPC/IMessage.h>

class IEncoder {
public:
    explicit IEncoder(IMessageBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    IEncoder& operator<<(bool value)
    {
        return *this << (u8)value;
    }

    IEncoder& operator<<(u8 value)
    {
        m_buffer.append(value);
        return *this;
    }

    IEncoder& operator<<(u16 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 2);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        return *this;
    }

    IEncoder& operator<<(u32 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 4);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        m_buffer.unchecked_append((u8)(value >> 16));
        m_buffer.unchecked_append((u8)(value >> 24));
        return *this;
    }

    IEncoder& operator<<(i8 value)
    {
        m_buffer.append((u8)value);
        return *this;
    }

    IEncoder& operator<<(i16 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 2);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        return *this;
    }

    IEncoder& operator<<(i32 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 4);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        m_buffer.unchecked_append((u8)(value >> 16));
        m_buffer.unchecked_append((u8)(value >> 24));
        return *this;
    }

#ifdef __serenity__
    IEncoder& operator<<(size_t value)
    {
        return *this << (u32)value;
    }
#endif

    IEncoder& operator<<(float value)
    {
        union bits {
            float as_float;
            u32 as_u32;
        } u;
        u.as_float = value;
        return *this << u.as_u32;
    }

    IEncoder& operator<<(const char* value)
    {
        return *this << StringView(value);
    }

    IEncoder& operator<<(const StringView& value)
    {
        m_buffer.append((const u8*)value.characters_without_null_termination(), value.length());
        return *this;
    }

private:
    IMessageBuffer& m_buffer;
};
