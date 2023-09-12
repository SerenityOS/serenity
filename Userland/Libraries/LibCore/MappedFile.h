/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/MemoryStream.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Stream.h>
#include <LibCore/Forward.h>

namespace Core {

class MappedFile : public SeekableStream {
    AK_MAKE_NONCOPYABLE(MappedFile);
    AK_MAKE_NONMOVABLE(MappedFile);

public:
    // Reflects a simplified version of mmap protection and flags.
    enum class OpenMode {
        ReadOnly,
        ReadWrite,
    };

    static ErrorOr<NonnullOwnPtr<MappedFile>> map(StringView path, OpenMode mode = OpenMode::ReadOnly);
    static ErrorOr<NonnullOwnPtr<MappedFile>> map_from_file(NonnullOwnPtr<Core::File>, StringView path);
    static ErrorOr<NonnullOwnPtr<MappedFile>> map_from_fd_and_close(int fd, StringView path, OpenMode mode = OpenMode::ReadOnly);
    virtual ~MappedFile();

    // Non-stream APIs for using MappedFile as a simple POSIX API wrapper.
    void* data() { return m_data; }
    void const* data() const { return m_data; }
    ReadonlyBytes bytes() const { return { m_data, m_size }; }
    ErrorOr<Bytes> writable_bytes()
    {
        // We need to catch write accesses ourselves, since otherwise the program crashes from a memory access violation.
        if (!m_stream.is_writing_enabled() || !m_stream.is_open())
            // Mirrors POSIX file errors (instead of memory access errors).
            return Error::from_errno(EBADF);
        return Bytes { m_data, m_size };
    }
    size_t size() const { return m_size; }

    // ^Stream
    virtual ErrorOr<Bytes> read_some(Bytes bytes) override { return m_stream.read_some(bytes); }
    virtual ErrorOr<void> read_until_filled(Bytes bytes) override { return m_stream.read_until_filled(bytes); }
    virtual ErrorOr<ByteBuffer> read_until_eof(size_t block_size) override { return m_stream.read_until_eof(block_size); }
    virtual ErrorOr<void> discard(size_t discarded_bytes) override { return m_stream.discard(discarded_bytes); }
    virtual ErrorOr<size_t> write_some(ReadonlyBytes bytes) override { return m_stream.write_some(bytes); }
    virtual ErrorOr<void> write_until_depleted(ReadonlyBytes bytes) override { return m_stream.write_until_depleted(bytes); }
    virtual bool is_eof() const override { return m_stream.is_eof(); }
    virtual bool is_open() const override { return m_stream.is_open(); }
    virtual void close() override { m_stream.close(); }

    // ^SeekableStream
    virtual ErrorOr<size_t> size() override { return m_size; }
    virtual ErrorOr<size_t> seek(i64 offset, SeekMode mode) override { return m_stream.seek(offset, mode); }
    virtual ErrorOr<size_t> tell() const override { return m_stream.tell(); }
    virtual ErrorOr<void> truncate(size_t) override { return Error::from_errno(ENOTSUP); }

private:
    explicit MappedFile(void*, size_t, OpenMode);

    void* m_data { nullptr };
    size_t m_size { 0 };

    FixedMemoryStream m_stream;
};

class SharedMappedFile : public RefCounted<SharedMappedFile> {
public:
    explicit SharedMappedFile(NonnullOwnPtr<MappedFile> file)
        : m_file(move(file))
    {
    }

    MappedFile const& operator->() const { return *m_file; }
    MappedFile& operator->() { return *m_file; }

private:
    NonnullOwnPtr<MappedFile> m_file;
};

}
