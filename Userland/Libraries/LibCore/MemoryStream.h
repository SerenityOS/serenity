/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <AK/TypedTransfer.h>
#include <LibCore/Stream.h>

namespace Core::Stream {

/// A stream class that allows for reading/writing on a preallocated memory area
/// using a single read/write head.
class FixedMemoryStream final : public SeekableStream {
public:
    static ErrorOr<NonnullOwnPtr<FixedMemoryStream>> construct(Bytes bytes);
    static ErrorOr<NonnullOwnPtr<FixedMemoryStream>> construct(ReadonlyBytes bytes);

    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;
    virtual ErrorOr<void> truncate(off_t) override;
    virtual ErrorOr<Bytes> read(Bytes bytes) override;

    virtual ErrorOr<off_t> seek(i64 offset, SeekMode seek_mode = SeekMode::SetPosition) override;

    virtual ErrorOr<size_t> write(ReadonlyBytes bytes) override;
    virtual ErrorOr<void> write_entire_buffer(ReadonlyBytes bytes) override;

    Bytes bytes();
    ReadonlyBytes bytes() const;
    size_t offset() const;
    size_t remaining() const;

private:
    explicit FixedMemoryStream(Bytes bytes);
    explicit FixedMemoryStream(ReadonlyBytes bytes);

    Bytes m_bytes;
    size_t m_offset { 0 };
    bool m_writing_enabled { true };
};

/// A stream class that allows for writing to an automatically allocating memory area
/// and reading back the written data afterwards.
class AllocatingMemoryStream final : public Stream {
public:
    virtual ErrorOr<Bytes> read(Bytes) override;
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;
    virtual ErrorOr<void> discard(size_t) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;

    size_t used_buffer_size() const;

    ErrorOr<Optional<size_t>> offset_of(ReadonlyBytes needle) const;

private:
    // Note: We set the inline buffer capacity to zero to make moving chunks as efficient as possible.
    using Chunk = AK::Detail::ByteBuffer<0>;
    static constexpr size_t chunk_size = 4096;

    ErrorOr<ReadonlyBytes> next_read_range();
    ErrorOr<Bytes> next_write_range();
    void cleanup_unused_chunks();

    Vector<Chunk> m_chunks;
    size_t m_read_offset = 0;
    size_t m_write_offset = 0;
};

}
