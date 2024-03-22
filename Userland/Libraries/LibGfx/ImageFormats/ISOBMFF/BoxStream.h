/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MaybeOwned.h>
#include <AK/Stream.h>

namespace Gfx::ISOBMFF {

class BoxStream final : public Stream {
public:
    explicit BoxStream(MaybeOwned<Stream> stream, size_t size)
        : m_stream(move(stream))
        , m_data_left(size)
    {
    }

    virtual bool is_eof() const override { return m_stream->is_eof() || remaining() == 0; }
    virtual bool is_open() const override { return m_stream->is_open(); }
    virtual void close() override { m_stream->close(); }
    virtual ErrorOr<Bytes> read_some(Bytes bytes) override
    {
        auto read_bytes = TRY(m_stream->read_some(bytes));
        m_data_left -= min(read_bytes.size(), m_data_left);
        return read_bytes;
    }

    virtual ErrorOr<size_t> write_some(ReadonlyBytes) override { VERIFY_NOT_REACHED(); }
    virtual ErrorOr<void> write_until_depleted(ReadonlyBytes) override { VERIFY_NOT_REACHED(); }

    size_t remaining() const
    {
        return m_data_left;
    }
    ErrorOr<void> discard_remaining()
    {
        return discard(remaining());
    }

private:
    MaybeOwned<Stream> m_stream;
    size_t m_data_left;
};

}
