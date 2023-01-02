/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitCast.h>
#include <AK/ByteBuffer.h>
#include <AK/DeprecatedString.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/URL.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/DateTime.h>
#include <LibCore/Proxy.h>
#include <LibCore/System.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>

namespace IPC {

template<>
ErrorOr<void> encode(Encoder& encoder, float const& value)
{
    return encoder.encode(bit_cast<u32>(value));
}

template<>
ErrorOr<void> encode(Encoder& encoder, double const& value)
{
    return encoder.encode(bit_cast<u64>(value));
}

template<>
ErrorOr<void> encode(Encoder& encoder, StringView const& value)
{
    TRY(encoder.append(reinterpret_cast<u8 const*>(value.characters_without_null_termination()), value.length()));
    return {};
}

template<>
ErrorOr<void> encode(Encoder& encoder, DeprecatedString const& value)
{
    if (value.is_null())
        return encoder.encode(-1);

    TRY(encoder.encode(static_cast<i32>(value.length())));
    TRY(encoder.encode(value.view()));
    return {};
}

template<>
ErrorOr<void> encode(Encoder& encoder, ByteBuffer const& value)
{
    TRY(encoder.encode(static_cast<i32>(value.size())));
    TRY(encoder.append(value.data(), value.size()));
    return {};
}

template<>
ErrorOr<void> encode(Encoder& encoder, JsonValue const& value)
{
    return encoder.encode(value.serialized<StringBuilder>());
}

template<>
ErrorOr<void> encode(Encoder& encoder, URL const& value)
{
    return encoder.encode(value.to_deprecated_string());
}

template<>
ErrorOr<void> encode(Encoder& encoder, Dictionary const& dictionary)
{
    TRY(encoder.encode(static_cast<u64>(dictionary.size())));

    TRY(dictionary.try_for_each_entry([&](auto const& key, auto const& value) -> ErrorOr<void> {
        TRY(encoder.encode(key));
        TRY(encoder.encode(value));
        return {};
    }));

    return {};
}

template<>
ErrorOr<void> encode(Encoder& encoder, File const& file)
{
    int fd = file.fd();

    if (fd != -1)
        fd = TRY(Core::System::dup(fd));

    TRY(encoder.append_file_descriptor(fd));
    return {};
}

template<>
ErrorOr<void> encode(Encoder&, Empty const&)
{
    return {};
}

template<>
ErrorOr<void> encode(Encoder& encoder, Core::AnonymousBuffer const& buffer)
{
    TRY(encoder.encode(buffer.is_valid()));

    if (buffer.is_valid()) {
        TRY(encoder.encode(static_cast<u32>(buffer.size())));
        TRY(encoder.encode(IPC::File { buffer.fd() }));
    }

    return {};
}

template<>
ErrorOr<void> encode(Encoder& encoder, Core::DateTime const& datetime)
{
    return encoder.encode(static_cast<i64>(datetime.timestamp()));
}

template<>
ErrorOr<void> encode(Encoder& encoder, Core::ProxyData const& proxy)
{
    TRY(encoder.encode(proxy.type));
    TRY(encoder.encode(proxy.host_ipv4));
    TRY(encoder.encode(proxy.port));
    return {};
}

}
