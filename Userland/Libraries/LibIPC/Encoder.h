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
ErrorOr<void> encode(Encoder&, T const&)
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
    ErrorOr<void> encode(T const& value);

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

    ErrorOr<void> encode_size(size_t size);

private:
    void encode_u32(u32);
    void encode_u64(u64);

    MessageBuffer& m_buffer;
};

template<Arithmetic T>
ErrorOr<void> encode(Encoder& encoder, T const& value)
{
    TRY(encoder.extend_capacity(sizeof(T)));

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

    return {};
}

template<Enum T>
ErrorOr<void> encode(Encoder& encoder, T const& value)
{
    return encoder.encode(to_underlying(value));
}

template<>
ErrorOr<void> encode(Encoder&, float const&);

template<>
ErrorOr<void> encode(Encoder&, double const&);

template<>
ErrorOr<void> encode(Encoder&, StringView const&);

template<>
ErrorOr<void> encode(Encoder&, DeprecatedString const&);

template<>
ErrorOr<void> encode(Encoder&, ByteBuffer const&);

template<>
ErrorOr<void> encode(Encoder&, JsonValue const&);

template<>
ErrorOr<void> encode(Encoder&, URL const&);

template<>
ErrorOr<void> encode(Encoder&, Dictionary const&);

template<>
ErrorOr<void> encode(Encoder&, File const&);

template<>
ErrorOr<void> encode(Encoder&, Empty const&);

template<Concepts::Vector T>
ErrorOr<void> encode(Encoder& encoder, T const& vector)
{
    // NOTE: Do not change this encoding without also updating LibC/netdb.cpp.
    TRY(encoder.encode_size(vector.size()));

    for (auto const& value : vector)
        TRY(encoder.encode(value));

    return {};
}

template<Concepts::HashMap T>
ErrorOr<void> encode(Encoder& encoder, T const& hashmap)
{
    TRY(encoder.encode_size(hashmap.size()));

    for (auto it : hashmap) {
        TRY(encoder.encode(it.key));
        TRY(encoder.encode(it.value));
    }

    return {};
}

template<Concepts::SharedSingleProducerCircularQueue T>
ErrorOr<void> encode(Encoder& encoder, T const& queue)
{
    return encoder.encode(IPC::File { queue.fd() });
}

template<Concepts::Optional T>
ErrorOr<void> encode(Encoder& encoder, T const& optional)
{
    TRY(encoder.encode(optional.has_value()));

    if (optional.has_value())
        TRY(encoder.encode(optional.value()));

    return {};
}

template<Concepts::Variant T>
ErrorOr<void> encode(Encoder& encoder, T const& variant)
{
    TRY(encoder.encode(variant.index()));

    return variant.visit([&](auto const& value) {
        return encoder.encode(value);
    });
}

// This must be last so that it knows about the above specializations.
template<typename T>
ErrorOr<void> Encoder::encode(T const& value)
{
    return IPC::encode(*this, value);
}

}
