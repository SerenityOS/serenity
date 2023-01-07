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

    CircularBuffer(CircularBuffer&& other) = default;
    CircularBuffer& operator=(CircularBuffer&& other) = default;

    ~CircularBuffer() = default;

    size_t write(ReadonlyBytes bytes);
    Bytes read(Bytes bytes);
    ErrorOr<void> discard(size_t discarded_bytes);

    /// Compared to `read()`, this starts reading from an offset that is `distance` bytes
    /// before the current write pointer and allows for reading already-read data.
    ErrorOr<Bytes> read_with_seekback(Bytes bytes, size_t distance);

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
    [[nodiscard]] ReadonlyBytes next_read_span_with_seekback(size_t distance) const;

    ByteBuffer m_buffer {};

    size_t m_reading_head {};
    size_t m_used_space {};
    size_t m_seekback_limit {};
};

}
