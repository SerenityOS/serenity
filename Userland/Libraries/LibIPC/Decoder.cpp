/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/URL.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/DateTime.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/File.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/socket.h>

namespace IPC {

bool Decoder::decode(bool& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u8& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u16& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u32& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(u64& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i8& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i16& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i32& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(i64& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(float& value)
{
    m_stream >> value;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(String& value)
{
    i32 length = 0;
    m_stream >> length;
    if (m_stream.handle_any_error())
        return false;
    if (length < 0) {
        value = {};
        return true;
    }
    if (length == 0) {
        value = String::empty();
        return true;
    }
    char* text_buffer = nullptr;
    auto text_impl = StringImpl::create_uninitialized(static_cast<size_t>(length), text_buffer);
    m_stream >> Bytes { text_buffer, static_cast<size_t>(length) };
    value = *text_impl;
    return !m_stream.handle_any_error();
}

bool Decoder::decode(ByteBuffer& value)
{
    i32 length = 0;
    m_stream >> length;
    if (m_stream.handle_any_error())
        return false;
    if (length < 0) {
        value = {};
        return true;
    }
    if (length == 0) {
        value = ByteBuffer::create_uninitialized(0);
        return true;
    }
    value = ByteBuffer::create_uninitialized(length);
    m_stream >> value.bytes();
    return !m_stream.handle_any_error();
}

bool Decoder::decode(URL& value)
{
    String string;
    if (!decode(string))
        return false;
    value = URL(string);
    return true;
}

bool Decoder::decode(Dictionary& dictionary)
{
    u64 size = 0;
    m_stream >> size;
    if (m_stream.handle_any_error())
        return false;
    if (size >= (size_t)NumericLimits<i32>::max()) {
        VERIFY_NOT_REACHED();
    }

    for (size_t i = 0; i < size; ++i) {
        String key;
        if (!decode(key))
            return false;
        String value;
        if (!decode(value))
            return false;
        dictionary.add(move(key), move(value));
    }

    return true;
}

bool Decoder::decode([[maybe_unused]] File& file)
{
#ifdef __serenity__
    int fd = recvfd(m_sockfd, O_CLOEXEC);
    if (fd < 0) {
        dbgln("recvfd: {}", strerror(errno));
        return false;
    }
    file = File(fd, File::ConstructWithReceivedFileDescriptor);
    return true;
#else
    [[maybe_unused]] auto fd = m_sockfd;
    warnln("fd passing is not supported on this platform, sorry :(");
    return false;
#endif
}

bool decode(Decoder& decoder, Core::AnonymousBuffer& buffer)
{
    bool valid = false;
    if (!decoder.decode(valid))
        return false;
    if (!valid) {
        buffer = {};
        return true;
    }
    u32 size;
    if (!decoder.decode(size))
        return false;
    IPC::File anon_file;
    if (!decoder.decode(anon_file))
        return false;

    buffer = Core::AnonymousBuffer::create_from_anon_fd(anon_file.take_fd(), size);
    return buffer.is_valid();
}

bool decode(Decoder& decoder, Core::DateTime& datetime)
{
    i64 timestamp = -1;
    if (!decoder.decode(timestamp))
        return false;

    datetime = Core::DateTime::from_timestamp(static_cast<time_t>(timestamp));
    return true;
}

}
