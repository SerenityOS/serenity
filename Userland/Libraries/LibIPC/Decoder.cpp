/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonValue.h>
#include <AK/URL.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/DateTime.h>
#include <LibCore/Proxy.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/File.h>
#include <fcntl.h>

namespace IPC {

template<>
ErrorOr<DeprecatedString> decode(Decoder& decoder)
{
    auto length = TRY(decoder.decode<i32>());
    if (length < 0)
        return DeprecatedString {};
    if (length == 0)
        return DeprecatedString::empty();

    char* text_buffer = nullptr;
    auto text_impl = StringImpl::create_uninitialized(static_cast<size_t>(length), text_buffer);

    Bytes bytes { text_buffer, static_cast<size_t>(length) };
    TRY(decoder.decode_into(bytes));

    return DeprecatedString { *text_impl };
}

template<>
ErrorOr<ByteBuffer> decode(Decoder& decoder)
{
    auto length = TRY(decoder.decode<i32>());
    if (length <= 0)
        return ByteBuffer {};

    auto buffer = TRY(ByteBuffer::create_uninitialized(length));
    auto bytes = buffer.bytes();

    TRY(decoder.decode_into(bytes));
    return buffer;
}

template<>
ErrorOr<JsonValue> decode(Decoder& decoder)
{
    auto json = TRY(decoder.decode<DeprecatedString>());
    return JsonValue::from_string(json);
}

template<>
ErrorOr<URL> decode(Decoder& decoder)
{
    auto url = TRY(decoder.decode<DeprecatedString>());
    return URL { url };
}

template<>
ErrorOr<Dictionary> decode(Decoder& decoder)
{
    auto size = TRY(decoder.decode<u64>());
    if (size >= NumericLimits<i32>::max())
        VERIFY_NOT_REACHED();

    Dictionary dictionary {};

    for (size_t i = 0; i < size; ++i) {
        auto key = TRY(decoder.decode<DeprecatedString>());
        auto value = TRY(decoder.decode<DeprecatedString>());
        dictionary.add(move(key), move(value));
    }

    return dictionary;
}

template<>
ErrorOr<File> decode(Decoder& decoder)
{
    int fd = TRY(decoder.socket().receive_fd(O_CLOEXEC));
    return File { fd, File::ConstructWithReceivedFileDescriptor };
}

template<>
ErrorOr<Empty> decode(Decoder&)
{
    return Empty {};
}

template<>
ErrorOr<Core::AnonymousBuffer> decode(Decoder& decoder)
{
    if (auto valid = TRY(decoder.decode<bool>()); !valid)
        return Core::AnonymousBuffer {};

    auto size = TRY(decoder.decode<u32>());
    auto anon_file = TRY(decoder.decode<IPC::File>());

    return Core::AnonymousBuffer::create_from_anon_fd(anon_file.take_fd(), size);
}

template<>
ErrorOr<Core::DateTime> decode(Decoder& decoder)
{
    auto timestamp = TRY(decoder.decode<i64>());
    return Core::DateTime::from_timestamp(static_cast<time_t>(timestamp));
}

template<>
ErrorOr<Core::ProxyData> decode(Decoder& decoder)
{
    auto type = TRY(decoder.decode<Core::ProxyData::Type>());
    auto host_ipv4 = TRY(decoder.decode<u32>());
    auto port = TRY(decoder.decode<int>());

    return Core::ProxyData { type, host_ipv4, port };
}

}
