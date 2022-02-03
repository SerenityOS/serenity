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

class MemoryStream final : public SeekableStream {
public:
    static ErrorOr<NonnullOwnPtr<MemoryStream>> construct(Bytes bytes)
    {
        return adopt_nonnull_own_or_enomem<MemoryStream>(new (nothrow) MemoryStream(bytes));
    }

    virtual bool is_eof() const override { return m_offset >= m_bytes.size(); }
    virtual bool is_open() const override { return true; }
    // FIXME: It doesn't make sense to close an memory stream. Therefore, we don't do anything here. Is that fine?
    virtual void close() override { }
    // FIXME: It doesn't make sense to truncate a memory stream. Therefore, we don't do anything here. Is that fine?
    virtual ErrorOr<void> truncate(off_t) override { return Error::from_errno(ENOTSUP); }

    virtual ErrorOr<size_t> read(Bytes bytes) override
    {
        auto to_read = min(remaining(), bytes.size());
        if (to_read == 0)
            return 0;

        m_bytes.slice(m_offset, to_read).copy_to(bytes);
        m_offset += to_read;
        return bytes.size();
    }

    virtual ErrorOr<off_t> seek(i64 offset, SeekMode seek_mode = SeekMode::SetPosition) override
    {
        switch (seek_mode) {
        case SeekMode::SetPosition:
            if (offset >= static_cast<i64>(m_bytes.size()))
                return Error::from_string_literal("Offset past the end of the stream memory"sv);

            m_offset = offset;
            break;
        case SeekMode::FromCurrentPosition:
            if (offset + static_cast<i64>(m_offset) >= static_cast<i64>(m_bytes.size()))
                return Error::from_string_literal("Offset past the end of the stream memory"sv);

            m_offset += offset;
            break;
        case SeekMode::FromEndPosition:
            if (offset >= static_cast<i64>(m_bytes.size()))
                return Error::from_string_literal("Offset past the start of the stream memory"sv);

            m_offset = m_bytes.size() - offset;
            break;
        }
        return static_cast<off_t>(m_offset);
    }

    virtual ErrorOr<size_t> write(ReadonlyBytes bytes) override
    {
        // FIXME: Can this not error?
        const auto nwritten = bytes.copy_trimmed_to(m_bytes.slice(m_offset));
        m_offset += nwritten;
        return nwritten;
    }
    virtual bool write_or_error(ReadonlyBytes bytes) override
    {
        if (remaining() < bytes.size())
            return false;

        MUST(write(bytes));
        return true;
    }

    Bytes bytes() { return m_bytes; }
    ReadonlyBytes bytes() const { return m_bytes; }
    size_t offset() const { return m_offset; }
    size_t remaining() const { return m_bytes.size() - m_offset; }

protected:
    explicit MemoryStream(Bytes bytes)
        : m_bytes(bytes)
    {
    }

private:
    Bytes m_bytes;
    size_t m_offset { 0 };
};

}
