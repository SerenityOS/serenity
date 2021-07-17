/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/String.h>
#include <string.h>

namespace SQL {

inline void dump(u8 const* ptr, size_t sz, String const& prefix)
{
    StringBuilder builder;
    builder.appendff("{0} {1:04x} | ", prefix, sz);
    Vector<String> bytes;
    for (auto ix = 0u; ix < sz; ++ix) {
        bytes.append(String::formatted("{0:02x}", *(ptr + ix)));
    }
    StringBuilder bytes_builder;
    bytes_builder.join(" ", bytes);
    builder.append(bytes_builder.to_string());
    dbgln(builder.to_string());
}

inline void write(ByteBuffer& buffer, u8 const* ptr, size_t sz)
{
    if constexpr (SQL_DEBUG)
        dump(ptr, sz, "->");
    buffer.append(ptr, sz);
}

inline u8* read(ByteBuffer& buffer, size_t& at_offset, size_t sz)
{
    auto buffer_ptr = buffer.offset_pointer((int)at_offset);
    if constexpr (SQL_DEBUG)
        dump(buffer_ptr, sz, "<-");
    at_offset += sz;
    return buffer_ptr;
}

template<typename T>
void deserialize_from(ByteBuffer& buffer, size_t& at_offset, T& t)
{
    memcpy(&t, read(buffer, at_offset, sizeof(T)), sizeof(T));
}

template<typename T>
void serialize_to(ByteBuffer& buffer, T const& t)
{
    write(buffer, (u8 const*)(&t), sizeof(T));
}

template<>
inline void deserialize_from<String>(ByteBuffer& buffer, size_t& at_offset, String& string)
{
    u32 length;
    deserialize_from(buffer, at_offset, length);
    if (length > 0) {
        string = String((const char*)read(buffer, at_offset, length), length);
    } else {
        string = "";
    }
}

template<>
inline void serialize_to<String>(ByteBuffer& buffer, String const& t)
{
    u32 number_of_bytes = min(t.length(), 64);
    serialize_to(buffer, number_of_bytes);
    if (t.length() > 0) {
        write(buffer, (u8 const*)(t.characters()), number_of_bytes);
    }
}

}
