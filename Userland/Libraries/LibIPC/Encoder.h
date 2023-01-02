/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/HashMap.h>
#include <AK/StdLibExtras.h>
#include <AK/Variant.h>
#include <LibCore/SharedCircularQueue.h>
#include <LibIPC/Concepts.h>
#include <LibIPC/Forward.h>
#include <LibIPC/Message.h>

namespace IPC {

template<typename T>
bool encode(Encoder&, T const&)
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

    template<typename T>
    Encoder& operator<<(T const& value)
    {
        encode(value);
        return *this;
    }

    template<typename T>
    bool encode(T const& value);

    ErrorOr<void> extend_capacity(size_t capacity)
    {
        return m_buffer.data.try_ensure_capacity(m_buffer.data.size() + capacity);
    }

    void append(u8 value)
    {
        m_buffer.data.unchecked_append(value);
    }

    ErrorOr<void> append(u8 const* values, size_t count)
    {
        TRY(extend_capacity(count));
        m_buffer.data.unchecked_append(values, count);
        return {};
    }

    ErrorOr<void> append_file_descriptor(int fd)
    {
        auto auto_fd = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) AutoCloseFileDescriptor(fd)));
        return m_buffer.fds.try_append(move(auto_fd));
    }

private:
    void encode_u32(u32);
    void encode_u64(u64);

    MessageBuffer& m_buffer;
};

template<Arithmetic T>
bool encode(Encoder& encoder, T const& value)
{
    if (encoder.extend_capacity(sizeof(T)).is_error())
        return false;

    if constexpr (sizeof(T) == 1) {
        encoder.append(static_cast<u8>(value));
    } else if constexpr (sizeof(T) == 2) {
        encoder.append(static_cast<u8>(value));
        encoder.append(static_cast<u8>(value >> 8));
    } else if constexpr (sizeof(T) == 4) {
        encoder.append(static_cast<u8>(value));
        encoder.append(static_cast<u8>(value >> 8));
        encoder.append(static_cast<u8>(value >> 16));
        encoder.append(static_cast<u8>(value >> 24));
    } else if constexpr (sizeof(T) == 8) {
        encoder.append(static_cast<u8>(value));
        encoder.append(static_cast<u8>(value >> 8));
        encoder.append(static_cast<u8>(value >> 16));
        encoder.append(static_cast<u8>(value >> 24));
        encoder.append(static_cast<u8>(value >> 32));
        encoder.append(static_cast<u8>(value >> 40));
        encoder.append(static_cast<u8>(value >> 48));
        encoder.append(static_cast<u8>(value >> 56));
    } else {
        static_assert(DependentFalse<T>);
    }

    return true;
}

template<Enum T>
bool encode(Encoder& encoder, T const& value)
{
    return encoder.encode(to_underlying(value));
}

template<>
bool encode(Encoder&, float const&);

template<>
bool encode(Encoder&, double const&);

template<>
bool encode(Encoder&, StringView const&);

template<>
bool encode(Encoder&, DeprecatedString const&);

template<>
bool encode(Encoder&, ByteBuffer const&);

template<>
bool encode(Encoder&, JsonValue const&);

template<>
bool encode(Encoder&, URL const&);

template<>
bool encode(Encoder&, Dictionary const&);

template<>
bool encode(Encoder&, File const&);

template<>
bool encode(Encoder&, Empty const&);

template<Concepts::Vector T>
bool encode(Encoder& encoder, T const& vector)
{
    if (!encoder.encode(static_cast<u64>(vector.size())))
        return false;

    for (auto const& value : vector) {
        if (!encoder.encode(value))
            return false;
    }

    return true;
}

template<Concepts::HashMap T>
bool encode(Encoder& encoder, T const& hashmap)
{
    if (!encoder.encode(static_cast<u32>(hashmap.size())))
        return false;

    for (auto it : hashmap) {
        if (!encoder.encode(it.key))
            return false;
        if (!encoder.encode(it.value))
            return false;
    }

    return true;
}

template<Concepts::SharedSingleProducerCircularQueue T>
bool encode(Encoder& encoder, T const& queue)
{
    return encoder.encode(IPC::File { queue.fd() });
}

template<Concepts::Optional T>
bool encode(Encoder& encoder, T const& optional)
{
    if (!encoder.encode(optional.has_value()))
        return false;

    if (optional.has_value())
        return encoder.encode(optional.value());
    return true;
}

template<Concepts::Variant T>
bool encode(Encoder& encoder, T const& variant)
{
    if (!encoder.encode(variant.index()))
        return false;

    return variant.visit([&](auto const& value) {
        return encoder.encode(value);
    });
}

// This must be last so that it knows about the above specializations.
template<typename T>
bool Encoder::encode(T const& value)
{
    return IPC::encode(*this, value);
}

}
