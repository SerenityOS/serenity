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
#include <LibIPC/Dictionary.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>

namespace IPC {

template<>
bool encode(Encoder& encoder, float const& value)
{
    return encoder.encode(bit_cast<u32>(value));
}

template<>
bool encode(Encoder& encoder, double const& value)
{
    return encoder.encode(bit_cast<u64>(value));
}

template<>
bool encode(Encoder& encoder, StringView const& value)
{
    auto result = encoder.append(reinterpret_cast<u8 const*>(value.characters_without_null_termination()), value.length());
    return !result.is_error();
}

template<>
bool encode(Encoder& encoder, DeprecatedString const& value)
{
    if (value.is_null())
        return encoder.encode(-1);

    if (!encoder.encode(static_cast<i32>(value.length())))
        return false;
    return encoder.encode(value.view());
}

template<>
bool encode(Encoder& encoder, ByteBuffer const& value)
{
    if (!encoder.encode(static_cast<i32>(value.size())))
        return false;

    auto result = encoder.append(value.data(), value.size());
    return !result.is_error();
}

template<>
bool encode(Encoder& encoder, JsonValue const& value)
{
    return encoder.encode(value.serialized<StringBuilder>());
}

template<>
bool encode(Encoder& encoder, URL const& value)
{
    return encoder.encode(value.to_deprecated_string());
}

template<>
bool encode(Encoder& encoder, Dictionary const& dictionary)
{
    if (!encoder.encode(static_cast<u64>(dictionary.size())))
        return false;

    bool had_error = false;

    dictionary.for_each_entry([&](auto const& key, auto const& value) {
        if (had_error)
            return;
        if (!encoder.encode(key) || !encoder.encode(value))
            had_error = true;
    });

    return !had_error;
}

template<>
bool encode(Encoder& encoder, File const& file)
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

    if (encoder.append_file_descriptor(fd).is_error())
        return false;
    return true;
}

template<>
bool encode(Encoder&, Empty const&)
{
    return true;
}

template<>
bool encode(Encoder& encoder, Core::AnonymousBuffer const& buffer)
{
    if (!encoder.encode(buffer.is_valid()))
        return false;

    if (buffer.is_valid()) {
        if (!encoder.encode(static_cast<u32>(buffer.size())))
            return false;
        if (!encoder.encode(IPC::File { buffer.fd() }))
            return false;
    }

    return true;
}

template<>
bool encode(Encoder& encoder, Core::DateTime const& datetime)
{
    return encoder.encode(static_cast<i64>(datetime.timestamp()));
}

template<>
bool encode(Encoder& encoder, Core::ProxyData const& proxy)
{
    if (!encoder.encode(proxy.type))
        return false;
    if (!encoder.encode(proxy.host_ipv4))
        return false;
    if (!encoder.encode(proxy.port))
        return false;
    return true;
}

}
