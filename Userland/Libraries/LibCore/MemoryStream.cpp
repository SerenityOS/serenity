/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 * Copyright (c) 2022, Tim Schumacher <timschumi@gmx.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/MemMem.h>
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
    return Error::from_errno(EBADF);
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

ErrorOr<size_t> FixedMemoryStream::seek(i64 offset, SeekMode seek_mode)
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
    return m_offset;
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

ErrorOr<Bytes> AllocatingMemoryStream::read(Bytes bytes)
{
    size_t read_bytes = 0;

    while (read_bytes < bytes.size()) {
        VERIFY(m_write_offset >= m_read_offset);

        auto range = TRY(next_read_range());
        if (range.size() == 0)
            break;

        auto copied_bytes = range.copy_trimmed_to(bytes.slice(read_bytes));

        read_bytes += copied_bytes;
        m_read_offset += copied_bytes;
    }

    cleanup_unused_chunks();

    return bytes.trim(read_bytes);
}

ErrorOr<size_t> AllocatingMemoryStream::write(ReadonlyBytes bytes)
{
    size_t written_bytes = 0;

    while (written_bytes < bytes.size()) {
        VERIFY(m_write_offset >= m_read_offset);

        auto range = TRY(next_write_range());

        auto copied_bytes = bytes.slice(written_bytes).copy_trimmed_to(range);

        written_bytes += copied_bytes;
        m_write_offset += copied_bytes;
    }

    return written_bytes;
}

ErrorOr<void> AllocatingMemoryStream::discard(size_t count)
{
    VERIFY(m_write_offset >= m_read_offset);

    if (count > used_buffer_size())
        return Error::from_string_literal("Number of discarded bytes is higher than the number of allocated bytes");

    m_read_offset += count;

    cleanup_unused_chunks();

    return {};
}

bool AllocatingMemoryStream::is_eof() const
{
    return used_buffer_size() == 0;
}

bool AllocatingMemoryStream::is_open() const
{
    return true;
}

void AllocatingMemoryStream::close()
{
}

size_t AllocatingMemoryStream::used_buffer_size() const
{
    return m_write_offset - m_read_offset;
}

ErrorOr<Optional<size_t>> AllocatingMemoryStream::offset_of(ReadonlyBytes needle) const
{
    VERIFY(m_write_offset >= m_read_offset);

    if (m_chunks.size() == 0)
        return Optional<size_t> {};

    // Ensure that we don't have to trim away more than one block.
    VERIFY(m_read_offset < chunk_size);
    VERIFY(m_chunks.size() * chunk_size - m_write_offset < chunk_size);

    auto chunk_count = m_chunks.size();
    auto search_spans = TRY(FixedArray<ReadonlyBytes>::try_create(chunk_count));

    for (size_t i = 0; i < chunk_count; i++) {
        search_spans[i] = m_chunks[i].span();
    }

    // Trimming is done first to ensure that we don't unintentionally shift around if the first and last chunks are the same.
    search_spans[chunk_count - 1] = search_spans[chunk_count - 1].trim(m_write_offset % chunk_size);
    search_spans[0] = search_spans[0].slice(m_read_offset);

    return AK::memmem(search_spans.begin(), search_spans.end(), needle);
}

ErrorOr<ReadonlyBytes> AllocatingMemoryStream::next_read_range()
{
    VERIFY(m_write_offset >= m_read_offset);

    size_t const chunk_index = m_read_offset / chunk_size;
    size_t const chunk_offset = m_read_offset % chunk_size;
    size_t const read_size = min(chunk_size - m_read_offset % chunk_size, m_write_offset - m_read_offset);

    if (read_size == 0)
        return ReadonlyBytes { static_cast<u8*>(nullptr), 0 };

    VERIFY(chunk_index < m_chunks.size());

    return ReadonlyBytes { m_chunks[chunk_index].data() + chunk_offset, read_size };
}

ErrorOr<Bytes> AllocatingMemoryStream::next_write_range()
{
    VERIFY(m_write_offset >= m_read_offset);

    size_t const chunk_index = m_write_offset / chunk_size;
    size_t const chunk_offset = m_write_offset % chunk_size;
    size_t const write_size = chunk_size - m_write_offset % chunk_size;

    if (chunk_index >= m_chunks.size())
        TRY(m_chunks.try_append(TRY(Chunk::create_uninitialized(chunk_size))));

    VERIFY(chunk_index < m_chunks.size());

    return Bytes { m_chunks[chunk_index].data() + chunk_offset, write_size };
}

void AllocatingMemoryStream::cleanup_unused_chunks()
{
    // FIXME: Move these all at once.
    while (m_read_offset >= chunk_size) {
        VERIFY(m_write_offset >= m_read_offset);

        auto buffer = m_chunks.take_first();
        m_read_offset -= chunk_size;
        m_write_offset -= chunk_size;

        m_chunks.append(move(buffer));
    }
}

}
