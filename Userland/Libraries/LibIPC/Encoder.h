/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Concepts.h>
#include <YAK/StdLibExtras.h>
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
    Encoder& operator<<(u32);
    Encoder& operator<<(u64);
    Encoder& operator<<(i8);
    Encoder& operator<<(i16);
    Encoder& operator<<(i32);
    Encoder& operator<<(i64);
    Encoder& operator<<(float);
    Encoder& operator<<(const char*);
    Encoder& operator<<(const StringView&);
    Encoder& operator<<(const String&);
    Encoder& operator<<(const ByteBuffer&);
    Encoder& operator<<(const URL&);
    Encoder& operator<<(const Dictionary&);
    Encoder& operator<<(const File&);
    template<typename K, typename V>
    Encoder& operator<<(const HashMap<K, V>& hashmap)
    {
        *this << (u32)hashmap.size();
        for (auto it : hashmap) {
            *this << it.key;
            *this << it.value;
        }
        return *this;
    }

    template<typename T>
    Encoder& operator<<(const Vector<T>& vector)
    {
        *this << (u64)vector.size();
        for (auto& value : vector)
            *this << value;
        return *this;
    }

    template<Enum T>
    Encoder& operator<<(T const& enum_value)
    {
        *this << YAK::to_underlying(enum_value);
        return *this;
    }

    template<typename T>
    Encoder& operator<<(const T& value)
    {
        encode(value);
        return *this;
    }

    template<typename T>
    Encoder& operator<<(const Optional<T>& optional)
    {
        *this << optional.has_value();
        if (optional.has_value())
            *this << optional.value();
        return *this;
    }

    template<typename T>
    void encode(const T& value)
    {
        IPC::encode(*this, value);
    }

private:
    MessageBuffer& m_buffer;
};

}
