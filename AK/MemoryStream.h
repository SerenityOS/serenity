/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Stream.h>
#include <AK/Vector.h>

namespace AK {

/// A stream class that allows for reading/writing on a preallocated memory area
/// using a single read/write head.
class FixedMemoryStream final : public SeekableStream {
public:
    explicit FixedMemoryStream(Bytes bytes);
    explicit FixedMemoryStream(ReadonlyBytes bytes);

    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;
    virtual ErrorOr<void> truncate(size_t) override;
    virtual ErrorOr<Bytes> read_some(Bytes bytes) override;
    virtual ErrorOr<void> read_until_filled(Bytes bytes) override;

    virtual ErrorOr<size_t> seek(i64 offset, SeekMode seek_mode = SeekMode::SetPosition) override;

    virtual ErrorOr<size_t> write_some(ReadonlyBytes bytes) override;
    virtual ErrorOr<void> write_until_depleted(ReadonlyBytes bytes) override;

    Bytes bytes();
    ReadonlyBytes readonly_bytes() const;
    size_t offset() const;
    size_t remaining() const;

private:
    Bytes m_bytes;
    size_t m_offset { 0 };
    bool m_writing_enabled { true };
};

/// A stream class that allows for writing to an automatically allocating memory area
/// and reading back the written data afterwards.
class AllocatingMemoryStream final : public Stream {
public:
    static constexpr size_t CHUNK_SIZE = 4096;

    virtual ErrorOr<Bytes> read_some(Bytes) override;
    virtual ErrorOr<size_t> write_some(ReadonlyBytes) override;
    virtual ErrorOr<void> discard(size_t) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;

    size_t used_buffer_size() const;

    ErrorOr<Optional<size_t>> offset_of(ReadonlyBytes needle) const;

private:
    // Note: We set the inline buffer capacity to zero to make moving chunks as efficient as possible.
    using Chunk = AK::Detail::ByteBuffer<0>;

    ErrorOr<ReadonlyBytes> next_read_range();
    ErrorOr<Bytes> next_write_range();
    void cleanup_unused_chunks();

    Vector<Chunk> m_chunks;
    size_t m_read_offset = 0;
    size_t m_write_offset = 0;
};

}

#if USING_AK_GLOBALLY
using AK::AllocatingMemoryStream;
using AK::FixedMemoryStream;
#endif
