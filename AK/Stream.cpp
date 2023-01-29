/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/Stream.h>

namespace AK {

ErrorOr<void> Stream::read_entire_buffer(Bytes buffer)
{
    size_t nread = 0;
    while (nread < buffer.size()) {
        if (is_eof())
            return Error::from_string_literal("Reached end-of-file before filling the entire buffer");

        auto result = read(buffer.slice(nread));
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() == EINTR) {
                continue;
            }

            return result.release_error();
        }

        nread += result.value().size();
    }

    return {};
}

ErrorOr<ByteBuffer> Stream::read_until_eof(size_t block_size)
{
    return read_until_eof_impl(block_size);
}

ErrorOr<ByteBuffer> Stream::read_until_eof_impl(size_t block_size, size_t expected_file_size)
{
    ByteBuffer data;
    data.ensure_capacity(expected_file_size);

    size_t total_read = 0;
    Bytes buffer;
    while (!is_eof()) {
        if (buffer.is_empty()) {
            buffer = TRY(data.get_bytes_for_writing(block_size));
        }

        auto nread = TRY(read(buffer)).size();
        total_read += nread;
        buffer = buffer.slice(nread);
    }

    data.resize(total_read);
    return data;
}

ErrorOr<void> Stream::discard(size_t discarded_bytes)
{
    // Note: This was chosen arbitrarily.
    // Note: This can't be PAGE_SIZE because it is defined to sysconf() on Lagom.
    constexpr size_t continuous_read_size = 4096;

    Array<u8, continuous_read_size> buffer;

    while (discarded_bytes > 0) {
        if (is_eof())
            return Error::from_string_literal("Reached end-of-file before reading all discarded bytes");

        auto slice = TRY(read(buffer.span().slice(0, min(discarded_bytes, continuous_read_size))));
        discarded_bytes -= slice.size();
    }

    return {};
}

ErrorOr<void> Stream::write_entire_buffer(ReadonlyBytes buffer)
{
    size_t nwritten = 0;
    while (nwritten < buffer.size()) {
        auto result = write(buffer.slice(nwritten));
        if (result.is_error()) {
            if (result.error().is_errno() && result.error().code() == EINTR) {
                continue;
            }

            return result.release_error();
        }

        nwritten += result.value();
    }

    return {};
}

ErrorOr<size_t> SeekableStream::tell() const
{
    // Seek with 0 and SEEK_CUR does not modify anything despite the const_cast,
    // so it's safe to do this.
    return const_cast<SeekableStream*>(this)->seek(0, SeekMode::FromCurrentPosition);
}

ErrorOr<size_t> SeekableStream::size()
{
    auto original_position = TRY(tell());

    auto seek_result = seek(0, SeekMode::FromEndPosition);
    if (seek_result.is_error()) {
        // Let's try to restore the original position, just in case.
        auto restore_result = seek(original_position, SeekMode::SetPosition);
        if (restore_result.is_error()) {
            dbgln("SeekableStream::size: Couldn't restore initial position, stream might have incorrect position now!");
        }

        return seek_result.release_error();
    }

    TRY(seek(original_position, SeekMode::SetPosition));
    return seek_result.value();
}

ErrorOr<void> SeekableStream::discard(size_t discarded_bytes)
{
    TRY(seek(discarded_bytes, SeekMode::FromCurrentPosition));
    return {};
}

}
