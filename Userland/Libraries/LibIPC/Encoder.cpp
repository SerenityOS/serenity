/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitCast.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/DateTime.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>

namespace IPC {

Encoder& Encoder::operator<<(bool value)
{
    return *this << (u8)value;
}

Encoder& Encoder::operator<<(u8 value)
{
    m_buffer.data.append(value);
    return *this;
}

Encoder& Encoder::operator<<(u16 value)
{
    m_buffer.data.ensure_capacity(m_buffer.data.size() + 2);
    m_buffer.data.unchecked_append((u8)value);
    m_buffer.data.unchecked_append((u8)(value >> 8));
    return *this;
}

void Encoder::encode_u32(u32 value)
{
    m_buffer.data.ensure_capacity(m_buffer.data.size() + 4);
    m_buffer.data.unchecked_append((u8)value);
    m_buffer.data.unchecked_append((u8)(value >> 8));
    m_buffer.data.unchecked_append((u8)(value >> 16));
    m_buffer.data.unchecked_append((u8)(value >> 24));
}

void Encoder::encode_u64(u64 value)
{
    m_buffer.data.ensure_capacity(m_buffer.data.size() + 8);
    m_buffer.data.unchecked_append((u8)value);
    m_buffer.data.unchecked_append((u8)(value >> 8));
    m_buffer.data.unchecked_append((u8)(value >> 16));
    m_buffer.data.unchecked_append((u8)(value >> 24));
    m_buffer.data.unchecked_append((u8)(value >> 32));
    m_buffer.data.unchecked_append((u8)(value >> 40));
    m_buffer.data.unchecked_append((u8)(value >> 48));
    m_buffer.data.unchecked_append((u8)(value >> 56));
}

Encoder& Encoder::operator<<(unsigned value)
{
    encode_u32(value);
    return *this;
}

Encoder& Encoder::operator<<(unsigned long value)
{
    if constexpr (sizeof(value) == 4)
        encode_u32(value);
    else
        encode_u64(value);
    return *this;
}

Encoder& Encoder::operator<<(unsigned long long value)
{
    if constexpr (sizeof(value) == 4)
        encode_u32(value);
    else
        encode_u64(value);
    return *this;
}

Encoder& Encoder::operator<<(i8 value)
{
    m_buffer.data.append((u8)value);
    return *this;
}

Encoder& Encoder::operator<<(i16 value)
{
    m_buffer.data.ensure_capacity(m_buffer.data.size() + 2);
    m_buffer.data.unchecked_append((u8)value);
    m_buffer.data.unchecked_append((u8)(value >> 8));
    return *this;
}

Encoder& Encoder::operator<<(i32 value)
{
    m_buffer.data.ensure_capacity(m_buffer.data.size() + 4);
    m_buffer.data.unchecked_append((u8)value);
    m_buffer.data.unchecked_append((u8)(value >> 8));
    m_buffer.data.unchecked_append((u8)(value >> 16));
    m_buffer.data.unchecked_append((u8)(value >> 24));
    return *this;
}

Encoder& Encoder::operator<<(i64 value)
{
    m_buffer.data.ensure_capacity(m_buffer.data.size() + 8);
    m_buffer.data.unchecked_append((u8)value);
    m_buffer.data.unchecked_append((u8)(value >> 8));
    m_buffer.data.unchecked_append((u8)(value >> 16));
    m_buffer.data.unchecked_append((u8)(value >> 24));
    m_buffer.data.unchecked_append((u8)(value >> 32));
    m_buffer.data.unchecked_append((u8)(value >> 40));
    m_buffer.data.unchecked_append((u8)(value >> 48));
    m_buffer.data.unchecked_append((u8)(value >> 56));
    return *this;
}

Encoder& Encoder::operator<<(float value)
{
    u32 as_u32 = bit_cast<u32>(value);
    return *this << as_u32;
}

Encoder& Encoder::operator<<(double value)
{
    u64 as_u64 = bit_cast<u64>(value);
    return *this << as_u64;
}

Encoder& Encoder::operator<<(char const* value)
{
    return *this << StringView(value);
}

Encoder& Encoder::operator<<(StringView value)
{
    m_buffer.data.append((u8 const*)value.characters_without_null_termination(), value.length());
    return *this;
}

Encoder& Encoder::operator<<(String const& value)
{
    if (value.is_null())
        return *this << (i32)-1;
    *this << static_cast<i32>(value.length());
    return *this << value.view();
}

Encoder& Encoder::operator<<(ByteBuffer const& value)
{
    *this << static_cast<i32>(value.size());
    m_buffer.data.append(value.data(), value.size());
    return *this;
}

Encoder& Encoder::operator<<(URL const& value)
{
    return *this << value.to_string();
}

Encoder& Encoder::operator<<(Dictionary const& dictionary)
{
    *this << (u64)dictionary.size();
    dictionary.for_each_entry([this](auto& key, auto& value) {
        *this << key << value;
    });
    return *this;
}

Encoder& Encoder::operator<<(File const& file)
{
    int fd = file.fd();
    if (fd != -1) {
        auto result = dup(fd);
        if (result < 0) {
            perror("dup");
            VERIFY_NOT_REACHED();
        }
        fd = result;
    }
    m_buffer.fds.append(adopt_ref(*new AutoCloseFileDescriptor(fd)));
    return *this;
}

bool encode(Encoder& encoder, Core::AnonymousBuffer const& buffer)
{
    encoder << buffer.is_valid();
    if (buffer.is_valid()) {
        encoder << (u32)buffer.size();
        encoder << IPC::File(buffer.fd());
    }
    return true;
}

bool encode(Encoder& encoder, Core::DateTime const& datetime)
{
    encoder << static_cast<i64>(datetime.timestamp());
    return true;
}

}
