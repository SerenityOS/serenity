/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/MemoryStream.h>

namespace Core::Stream {

FixedMemoryStream::FixedMemoryStream(Bytes bytes)
    : m_bytes(bytes)
{
}

FixedMemoryStream::FixedMemoryStream(ReadonlyBytes bytes)
    : m_bytes({ const_cast<u8*>(bytes.data()), bytes.size() })
    , m_writing_enabled(false)
{
}

ErrorOr<NonnullOwnPtr<FixedMemoryStream>> FixedMemoryStream::construct(Bytes bytes)
{
    return adopt_nonnull_own_or_enomem<FixedMemoryStream>(new (nothrow) FixedMemoryStream(bytes));
}

ErrorOr<NonnullOwnPtr<FixedMemoryStream>> FixedMemoryStream::construct(ReadonlyBytes bytes)
{
    return adopt_nonnull_own_or_enomem<FixedMemoryStream>(new (nothrow) FixedMemoryStream(bytes));
}

bool FixedMemoryStream::is_eof() const
{
    return m_offset >= m_bytes.size();
}

bool FixedMemoryStream::is_open() const
{
    return true;
}

void FixedMemoryStream::close()
{
    // FIXME: It doesn't make sense to close a memory stream. Therefore, we don't do anything here. Is that fine?
}

ErrorOr<void> FixedMemoryStream::truncate(off_t)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<Bytes> FixedMemoryStream::read(Bytes bytes)
{
    auto to_read = min(remaining(), bytes.size());
    if (to_read == 0)
        return Bytes {};

    m_bytes.slice(m_offset, to_read).copy_to(bytes);
    m_offset += to_read;
    return bytes.trim(to_read);
}

ErrorOr<off_t> FixedMemoryStream::seek(i64 offset, SeekMode seek_mode)
{
    switch (seek_mode) {
    case SeekMode::SetPosition:
        if (offset > static_cast<i64>(m_bytes.size()))
            return Error::from_string_literal("Offset past the end of the stream memory");

        m_offset = offset;
        break;
    case SeekMode::FromCurrentPosition:
        if (offset + static_cast<i64>(m_offset) > static_cast<i64>(m_bytes.size()))
            return Error::from_string_literal("Offset past the end of the stream memory");

        m_offset += offset;
        break;
    case SeekMode::FromEndPosition:
        if (offset > static_cast<i64>(m_bytes.size()))
            return Error::from_string_literal("Offset past the start of the stream memory");

        m_offset = m_bytes.size() - offset;
        break;
    }
    return static_cast<off_t>(m_offset);
}

ErrorOr<size_t> FixedMemoryStream::write(ReadonlyBytes bytes)
{
    VERIFY(m_writing_enabled);

    // FIXME: Can this not error?
    auto const nwritten = bytes.copy_trimmed_to(m_bytes.slice(m_offset));
    m_offset += nwritten;
    return nwritten;
}

ErrorOr<void> FixedMemoryStream::write_entire_buffer(ReadonlyBytes bytes)
{
    if (remaining() < bytes.size())
        return Error::from_string_literal("Write of entire buffer ends past the memory area");

    TRY(write(bytes));
    return {};
}

Bytes FixedMemoryStream::bytes()
{
    VERIFY(m_writing_enabled);
    return m_bytes;
}
ReadonlyBytes FixedMemoryStream::bytes() const
{
    return m_bytes;
}

size_t FixedMemoryStream::offset() const
{
    return m_offset;
}

size_t FixedMemoryStream::remaining() const
{
    return m_bytes.size() - m_offset;
}

}
