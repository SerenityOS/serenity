/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Concepts.h>
#include <YAK/Forward.h>
#include <YAK/NumericLimits.h>
#include <YAK/StdLibExtras.h>
#include <YAK/String.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

template<typename T>
inline bool decode(Decoder&, T&)
{
    static_assert(DependentFalse<T>, "Base IPC::decoder() instantiated");
    VERIFY_NOT_REACHED();
}

class Decoder {
public:
    Decoder(InputMemoryStream& stream, int sockfd)
        : m_stream(stream)
        , m_sockfd(sockfd)
    {
    }

    bool decode(bool&);
    bool decode(u8&);
    bool decode(u16&);
    bool decode(u32&);
    bool decode(u64&);
    bool decode(i8&);
    bool decode(i16&);
    bool decode(i32&);
    bool decode(i64&);
    bool decode(float&);
    bool decode(String&);
    bool decode(ByteBuffer&);
    bool decode(URL&);
    bool decode(Dictionary&);
    bool decode(File&);
    template<typename K, typename V>
    bool decode(HashMap<K, V>& hashmap)
    {
        u32 size;
        if (!decode(size) || size > NumericLimits<i32>::max())
            return false;

        for (size_t i = 0; i < size; ++i) {
            K key;
            if (!decode(key))
                return false;

            V value;
            if (!decode(value))
                return false;

            hashmap.set(move(key), move(value));
        }
        return true;
    }

    template<Enum T>
    bool decode(T& enum_value)
    {
        UnderlyingType<T> inner_value;
        if (!decode(inner_value))
            return false;

        enum_value = T(inner_value);
        return true;
    }

    template<typename T>
    bool decode(T& value)
    {
        return IPC::decode(*this, value);
    }

    template<typename T>
    bool decode(Vector<T>& vector)
    {
        u64 size;
        if (!decode(size) || size > NumericLimits<i32>::max())
            return false;
        for (size_t i = 0; i < size; ++i) {
            T value;
            if (!decode(value))
                return false;
            vector.append(move(value));
        }
        return true;
    }

    template<typename T>
    bool decode(Optional<T>& optional)
    {
        bool has_value;
        if (!decode(has_value))
            return false;
        if (!has_value) {
            optional = {};
            return true;
        }
        T value;
        if (!decode(value))
            return false;
        optional = move(value);
        return true;
    }

private:
    InputMemoryStream& m_stream;
    int m_sockfd { -1 };
};

}
