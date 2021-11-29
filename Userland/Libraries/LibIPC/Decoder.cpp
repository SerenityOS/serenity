/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/URL.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/File.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/socket.h>

namespace IPC {

ErrorOr<void> Decoder::decode(bool& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(u8& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(u16& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(unsigned& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(unsigned long& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(unsigned long long& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(i8& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(i16& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(i32& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(i64& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(float& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(double& value)
{
    m_stream >> value;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(String& value)
{
    i32 length;
    TRY(decode(length));

    if (length < 0) {
        value = {};
        return {};
    }
    if (length == 0) {
        value = String::empty();
        return {};
    }
    char* text_buffer = nullptr;
    auto text_impl = StringImpl::create_uninitialized(static_cast<size_t>(length), text_buffer);
    m_stream >> Bytes { text_buffer, static_cast<size_t>(length) };
    value = *text_impl;
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(ByteBuffer& value)
{
    i32 length;
    TRY(decode(length));

    if (length < 0) {
        value = {};
        return {};
    }
    if (length == 0) {
        value = {};
        return {};
    }

    if (auto buffer_result = ByteBuffer::create_uninitialized(length); buffer_result.has_value())
        value = buffer_result.release_value();
    else
        return Error::from_errno(ENOMEM);

    m_stream >> value.bytes();
    return m_stream.try_handle_any_error();
}

ErrorOr<void> Decoder::decode(URL& value)
{
    String string;
    TRY(decode(string));
    value = URL(string);
    return {};
}

ErrorOr<void> Decoder::decode(Dictionary& dictionary)
{
    u64 size;
    TRY(decode(size));
    if (size >= (size_t)NumericLimits<i32>::max())
        VERIFY_NOT_REACHED();

    for (size_t i = 0; i < size; ++i) {
        String key;
        TRY(decode(key));
        String value;
        TRY(decode(value));
        dictionary.add(move(key), move(value));
    }

    return {};
}

ErrorOr<void> Decoder::decode([[maybe_unused]] File& file)
{
#ifdef __serenity__
    int fd = TRY(Core::System::recvfd(m_sockfd, O_CLOEXEC));
    file = File(fd, File::ConstructWithReceivedFileDescriptor);
    return {};
#else
    [[maybe_unused]] auto fd = m_sockfd;
    return Error::from_string_literal("File descriptor passing not supported on this platform");
#endif
}

ErrorOr<void> decode(Decoder& decoder, Core::AnonymousBuffer& buffer)
{
    bool valid;
    TRY(decoder.decode(valid));
    if (!valid) {
        buffer = {};
        return {};
    }
    u32 size;
    TRY(decoder.decode(size));
    IPC::File anon_file;
    TRY(decoder.decode(anon_file));

    buffer = TRY(Core::AnonymousBuffer::create_from_anon_fd(anon_file.take_fd(), size));
    return {};
}

ErrorOr<void> decode(Decoder& decoder, Core::DateTime& datetime)
{
    i64 timestamp;
    TRY(decoder.decode(timestamp));
    datetime = Core::DateTime::from_timestamp(static_cast<time_t>(timestamp));
    return {};
}

}
