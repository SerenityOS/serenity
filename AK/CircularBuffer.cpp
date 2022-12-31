/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CircularBuffer.h>
#include <AK/MemMem.h>

namespace AK {

CircularBuffer::CircularBuffer(ByteBuffer buffer)
    : m_buffer(move(buffer))
{
}

ErrorOr<CircularBuffer> CircularBuffer::create_empty(size_t size)
{
    auto temporary_buffer = TRY(ByteBuffer::create_uninitialized(size));

    CircularBuffer circular_buffer { move(temporary_buffer) };

    return circular_buffer;
}

ErrorOr<CircularBuffer> CircularBuffer::create_initialized(ByteBuffer buffer)
{
    CircularBuffer circular_buffer { move(buffer) };

    circular_buffer.m_used_space = circular_buffer.m_buffer.size();

    return circular_buffer;
}

size_t CircularBuffer::empty_space() const
{
    return capacity() - m_used_space;
}

size_t CircularBuffer::used_space() const
{
    return m_used_space;
}

size_t CircularBuffer::capacity() const
{
    return m_buffer.size();
}

bool CircularBuffer::is_wrapping_around() const
{
    return capacity() <= m_reading_head + m_used_space;
}

Optional<size_t> CircularBuffer::offset_of(StringView needle, Optional<size_t> until) const
{
    auto const read_until = until.value_or(m_used_space);

    Array<ReadonlyBytes, 2> spans {};
    spans[0] = next_read_span();

    if (spans[0].size() > read_until)
        spans[0] = spans[0].trim(read_until);
    else if (is_wrapping_around())
        spans[1] = m_buffer.span().slice(0, read_until - spans[0].size());

    return AK::memmem(spans.begin(), spans.end(), needle.bytes());
}

void CircularBuffer::clear()
{
    m_reading_head = 0;
    m_used_space = 0;
    m_seekback_limit = 0;
}

Bytes CircularBuffer::next_write_span()
{
    if (is_wrapping_around())
        return m_buffer.span().slice(m_reading_head + m_used_space - capacity(), capacity() - m_used_space);
    return m_buffer.span().slice(m_reading_head + m_used_space, capacity() - (m_reading_head + m_used_space));
}

ReadonlyBytes CircularBuffer::next_read_span() const
{
    return m_buffer.span().slice(m_reading_head, min(capacity() - m_reading_head, m_used_space));
}

ReadonlyBytes CircularBuffer::next_read_span_with_seekback(size_t distance) const
{
    VERIFY(m_seekback_limit <= capacity());
    VERIFY(distance <= m_seekback_limit);

    // Note: We are adding the capacity once here to ensure that we can wrap around the negative space by using modulo.
    auto read_offset = (capacity() + m_reading_head + m_used_space - distance) % capacity();

    return m_buffer.span().slice(read_offset, min(capacity() - read_offset, m_seekback_limit));
}

size_t CircularBuffer::write(ReadonlyBytes bytes)
{
    auto remaining = bytes.size();

    while (remaining > 0) {
        auto const next_span = next_write_span();
        if (next_span.size() == 0)
            break;

        auto const written_bytes = bytes.slice(bytes.size() - remaining).copy_trimmed_to(next_span);

        m_used_space += written_bytes;

        m_seekback_limit += written_bytes;
        if (m_seekback_limit > capacity())
            m_seekback_limit = capacity();

        remaining -= written_bytes;
    }

    return bytes.size() - remaining;
}

Bytes CircularBuffer::read(Bytes bytes)
{
    auto remaining = bytes.size();

    while (remaining > 0) {
        auto const next_span = next_read_span();
        if (next_span.size() == 0)
            break;

        auto written_bytes = next_span.copy_trimmed_to(bytes.slice(bytes.size() - remaining));

        m_used_space -= written_bytes;
        m_reading_head += written_bytes;

        if (m_reading_head >= capacity())
            m_reading_head -= capacity();

        remaining -= written_bytes;
    }

    return bytes.trim(bytes.size() - remaining);
}

ErrorOr<Bytes> CircularBuffer::read_with_seekback(Bytes bytes, size_t distance)
{
    if (distance > m_seekback_limit)
        return Error::from_string_literal("Tried a seekback read beyond the seekback limit");

    auto remaining = bytes.size();

    while (remaining > 0) {
        auto const next_span = next_read_span_with_seekback(distance);
        if (next_span.size() == 0)
            break;

        auto written_bytes = next_span.copy_trimmed_to(bytes.slice(bytes.size() - remaining));

        distance -= written_bytes;
        remaining -= written_bytes;
    }

    return bytes.trim(bytes.size() - remaining);
}

ErrorOr<void> CircularBuffer::discard(size_t discarding_size)
{
    if (m_used_space < discarding_size)
        return Error::from_string_literal("Can not discard more data than what the buffer contains");
    m_used_space -= discarding_size;
    m_reading_head = (m_reading_head + discarding_size) % capacity();

    return {};
}

}
