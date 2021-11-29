/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/StdLibExtras.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

template<typename T>
bool encode(Encoder&, T&)
{
    static_assert(DependentFalse<T>, "Base IPC::encode() was instantiated");
    VERIFY_NOT_REACHED();
}

class Encoder {
public:
    explicit Encoder(MessageBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    Encoder& operator<<(bool);
    Encoder& operator<<(u8);
    Encoder& operator<<(u16);
    Encoder& operator<<(unsigned);
    Encoder& operator<<(unsigned long);
    Encoder& operator<<(unsigned long long);
    Encoder& operator<<(i8);
    Encoder& operator<<(i16);
    Encoder& operator<<(i32);
    Encoder& operator<<(i64);
    Encoder& operator<<(float);
    Encoder& operator<<(double);
    Encoder& operator<<(char const*);
    Encoder& operator<<(StringView);
    Encoder& operator<<(String const&);
    Encoder& operator<<(ByteBuffer const&);
    Encoder& operator<<(URL const&);
    Encoder& operator<<(Dictionary const&);
    Encoder& operator<<(File const&);
    template<typename K, typename V>
    Encoder& operator<<(HashMap<K, V> const& hashmap)
    {
        *this << (u32)hashmap.size();
        for (auto it : hashmap) {
            *this << it.key;
            *this << it.value;
        }
        return *this;
    }

    template<typename T>
    Encoder& operator<<(Vector<T> const& vector)
    {
        *this << (u64)vector.size();
        for (auto& value : vector)
            *this << value;
        return *this;
    }

    template<Enum T>
    Encoder& operator<<(T const& enum_value)
    {
        *this << AK::to_underlying(enum_value);
        return *this;
    }

    template<typename T>
    Encoder& operator<<(T const& value)
    {
        encode(value);
        return *this;
    }

    template<typename T>
    Encoder& operator<<(Optional<T> const& optional)
    {
        *this << optional.has_value();
        if (optional.has_value())
            *this << optional.value();
        return *this;
    }

    template<typename T>
    void encode(T const& value)
    {
        IPC::encode(*this, value);
    }

private:
    void encode_u32(u32);
    void encode_u64(u64);

    MessageBuffer& m_buffer;
};

}
