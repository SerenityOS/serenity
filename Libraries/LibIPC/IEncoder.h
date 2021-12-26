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

#include <LibIPC/IMessage.h>

namespace IPC {

class Encoder {
public:
    explicit Encoder(MessageBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    Encoder& operator<<(bool value)
    {
        return *this << (u8)value;
    }

    Encoder& operator<<(u8 value)
    {
        m_buffer.append(value);
        return *this;
    }

    Encoder& operator<<(u16 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 2);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        return *this;
    }

    Encoder& operator<<(u32 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 4);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        m_buffer.unchecked_append((u8)(value >> 16));
        m_buffer.unchecked_append((u8)(value >> 24));
        return *this;
    }

    Encoder& operator<<(u64 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 8);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        m_buffer.unchecked_append((u8)(value >> 16));
        m_buffer.unchecked_append((u8)(value >> 24));
        m_buffer.unchecked_append((u8)(value >> 32));
        m_buffer.unchecked_append((u8)(value >> 40));
        m_buffer.unchecked_append((u8)(value >> 48));
        m_buffer.unchecked_append((u8)(value >> 56));
        return *this;
    }

    Encoder& operator<<(i8 value)
    {
        m_buffer.append((u8)value);
        return *this;
    }

    Encoder& operator<<(i16 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 2);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        return *this;
    }

    Encoder& operator<<(i32 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 4);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        m_buffer.unchecked_append((u8)(value >> 16));
        m_buffer.unchecked_append((u8)(value >> 24));
        return *this;
    }

    Encoder& operator<<(i64 value)
    {
        m_buffer.ensure_capacity(m_buffer.size() + 8);
        m_buffer.unchecked_append((u8)value);
        m_buffer.unchecked_append((u8)(value >> 8));
        m_buffer.unchecked_append((u8)(value >> 16));
        m_buffer.unchecked_append((u8)(value >> 24));
        m_buffer.unchecked_append((u8)(value >> 32));
        m_buffer.unchecked_append((u8)(value >> 40));
        m_buffer.unchecked_append((u8)(value >> 48));
        m_buffer.unchecked_append((u8)(value >> 56));
        return *this;
    }

    Encoder& operator<<(size_t value)
    {
        if constexpr (sizeof(size_t) == 4)
            return *this << (u32)value;
        else if constexpr (sizeof(size_t) == 8)
            return *this << (u64)value;
        ASSERT_NOT_REACHED();
    }

#ifndef __i386__
    Encoder& operator<<(ssize_t value)
    {
        if constexpr (sizeof(ssize_t) == 4)
            return *this << (i32)value;
        else if constexpr (sizeof(ssize_t) == 8)
            return *this << (i64)value;
        ASSERT_NOT_REACHED();
    }
#endif

    Encoder& operator<<(float value)
    {
        union bits {
            float as_float;
            u32 as_u32;
        } u;
        u.as_float = value;
        return *this << u.as_u32;
    }

    Encoder& operator<<(const char* value)
    {
        return *this << StringView(value);
    }

    Encoder& operator<<(const StringView& value)
    {
        m_buffer.append((const u8*)value.characters_without_null_termination(), value.length());
        return *this;
    }

private:
    MessageBuffer& m_buffer;
};

}
