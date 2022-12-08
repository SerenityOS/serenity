/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/Noncopyable.h>

namespace AK {

class CircularBuffer {
    AK_MAKE_NONCOPYABLE(CircularBuffer);

public:
    static ErrorOr<CircularBuffer> create_empty(size_t size);
    static ErrorOr<CircularBuffer> create_initialized(ByteBuffer);

    CircularBuffer(CircularBuffer&& other)
    {
        operator=(move(other));
    }

    CircularBuffer& operator=(CircularBuffer&& other)
    {
        if (&other == this)
            return *this;

        swap(m_buffer, other.m_buffer);
        swap(m_reading_head, other.m_reading_head);
        swap(m_used_space, other.m_used_space);

        return *this;
    }

    ~CircularBuffer() = default;

    size_t write(ReadonlyBytes bytes);
    Bytes read(Bytes bytes);
    ErrorOr<void> discard(size_t discarded_bytes);

    [[nodiscard]] size_t empty_space() const;
    [[nodiscard]] size_t used_space() const;
    [[nodiscard]] size_t capacity() const;

    Optional<size_t> offset_of(StringView needle, Optional<size_t> until = {}) const;

    void clear();

private:
    CircularBuffer(ByteBuffer);

    [[nodiscard]] bool is_wrapping_around() const;

    [[nodiscard]] Bytes next_write_span();
    [[nodiscard]] ReadonlyBytes next_read_span() const;

    ByteBuffer m_buffer {};

    size_t m_reading_head {};
    size_t m_used_space {};
};

}
