/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Forward.h>
#include <AK/NumericLimits.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

template<typename T>
inline ErrorOr<void> decode(Decoder&, T&)
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

    ErrorOr<void> decode(bool&);
    ErrorOr<void> decode(u8&);
    ErrorOr<void> decode(u16&);
    ErrorOr<void> decode(unsigned&);
    ErrorOr<void> decode(unsigned long&);
    ErrorOr<void> decode(unsigned long long&);
    ErrorOr<void> decode(i8&);
    ErrorOr<void> decode(i16&);
    ErrorOr<void> decode(i32&);
    ErrorOr<void> decode(i64&);
    ErrorOr<void> decode(float&);
    ErrorOr<void> decode(double&);
    ErrorOr<void> decode(String&);
    ErrorOr<void> decode(ByteBuffer&);
    ErrorOr<void> decode(URL&);
    ErrorOr<void> decode(Dictionary&);
    ErrorOr<void> decode(File&);
    template<typename K, typename V>
    ErrorOr<void> decode(HashMap<K, V>& hashmap)
    {
        u32 size;
        TRY(decode(size));
        if (size > NumericLimits<i32>::max())
            return Error::from_string_literal("IPC: Invalid HashMap size"sv);

        for (size_t i = 0; i < size; ++i) {
            K key;
            TRY(decode(key));
            V value;
            TRY(decode(value));
            TRY(hashmap.try_set(move(key), move(value)));
        }
        return {};
    }

    template<Enum T>
    ErrorOr<void> decode(T& enum_value)
    {
        UnderlyingType<T> inner_value;
        TRY(decode(inner_value));
        enum_value = T(inner_value);
        return {};
    }

    template<typename T>
    ErrorOr<void> decode(T& value)
    {
        return IPC::decode(*this, value);
    }

    template<typename T>
    ErrorOr<void> decode(Vector<T>& vector)
    {
        u64 size;
        TRY(decode(size));
        if (size > NumericLimits<i32>::max())
            return Error::from_string_literal("IPC: Invalid Vector size"sv);
        VERIFY(vector.is_empty());
        TRY(vector.try_ensure_capacity(size));
        for (size_t i = 0; i < size; ++i) {
            T value;
            TRY(decode(value));
            vector.template unchecked_append(move(value));
        }
        return {};
    }

    template<typename T>
    ErrorOr<void> decode(Optional<T>& optional)
    {
        bool has_value;
        TRY(decode(has_value));
        if (!has_value) {
            optional = {};
            return {};
        }
        T value;
        TRY(decode(value));
        optional = move(value);
        return {};
    }

private:
    InputMemoryStream& m_stream;
    int m_sockfd { -1 };
};

}
