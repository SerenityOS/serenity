/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AsyncStream.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringUtils.h>

namespace AK {

struct AsyncStreamHelpers {
    static Coroutine<ErrorOr<ReadonlyBytes>> consume_until(AsyncInputStream& stream, StringView delimiter, Optional<size_t> max_size = {})
    {
        size_t start_position = 0;
        while (true) {
            auto buffer = CO_TRY(co_await stream.peek());
            auto position = StringUtils::find(StringView { buffer }, delimiter, start_position);
            auto end_position = position.map([&](auto value) { return value + delimiter.length(); });
            if (max_size.has_value() && !end_position.has_value() && buffer.size() >= max_size.value())
                end_position = buffer.size();
            if (end_position.has_value())
                co_return must_sync(stream.read(*end_position));
            start_position = max(buffer.size(), delimiter.length() - 1) - delimiter.length() + 1;
        }
    }
};

class AsyncInputStreamSlice final : public AsyncInputStream {
public:
    AsyncInputStreamSlice(AsyncInputStream& stream, size_t length)
        : m_stream(stream)
        , m_length(length)
        , m_encountered_eof(stream.buffered_data().size() >= m_length)
    {
    }

    ~AsyncInputStreamSlice()
    {
        if (is_open())
            reset();
    }

    void reset() override
    {
        VERIFY(is_open());
        m_stream.reset();
        m_is_open = false;
    }

    Coroutine<ErrorOr<void>> close() override
    {
        VERIFY(is_open());
        if (m_length != 0) {
            reset();
            co_return Error::from_errno(EBUSY);
        }
        m_is_open = false;
        co_return {};
    }

    bool is_open() const override
    {
        return m_is_open;
    }

    Coroutine<ErrorOr<bool>> enqueue_some(Badge<AsyncInputStream>) override
    {
        if (m_encountered_eof)
            co_return false;
        auto eof_or_error = co_await m_stream.enqueue_some(badge());
        if (eof_or_error.is_error()) {
            m_is_open = false;
            co_return eof_or_error.release_error();
        } else if (!eof_or_error.release_value()) {
            reset();
            co_return Error::from_errno(EIO);
        }
        if (m_stream.buffered_data_unchecked(badge()).size() >= m_length)
            m_encountered_eof = true;
        co_return true;
    }

    ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override
    {
        auto data = m_stream.buffered_data_unchecked(badge());
        return data.slice(0, min(data.size(), m_length));
    }

    void dequeue(Badge<AsyncInputStream>, size_t bytes) override
    {
        m_stream.dequeue(badge(), bytes);
        m_length -= bytes;
    }

private:
    AsyncInputStream& m_stream;
    size_t m_length { 0 };
    bool m_encountered_eof { false };
    bool m_is_open { true };
};

class AsyncStreamPair final : public AsyncStream {
public:
    AsyncStreamPair(NonnullOwnPtr<AsyncInputStream>&& input_stream, NonnullOwnPtr<AsyncOutputStream>&& output_stream)
        : m_input_stream(move(input_stream))
        , m_output_stream(move(output_stream))
    {
    }

    ~AsyncStreamPair()
    {
        if (is_open())
            reset();
    }

    void reset() override
    {
        VERIFY(is_open());
        m_input_stream->reset();
        m_output_stream->reset();
        m_is_open = false;
    }

    Coroutine<ErrorOr<void>> close() override
    {
        VERIFY(is_open());
        m_is_open = false;

        auto result = co_await m_input_stream->close();
        if (result.is_error()) {
            m_output_stream->reset();
            co_return result;
        }
        CO_TRY(co_await m_output_stream->close());
        co_return {};
    }

    bool is_open() const override
    {
        return m_is_open;
    }

    Coroutine<ErrorOr<bool>> enqueue_some(Badge<AsyncInputStream>) override
    {
        auto result = co_await m_input_stream->enqueue_some(badge());
        if (result.is_error()) {
            m_is_open = false;
            m_output_stream->reset();
        }
        co_return result;
    }

    ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const override
    {
        return m_input_stream->buffered_data_unchecked(badge());
    }

    void dequeue(Badge<AsyncInputStream>, size_t bytes) override
    {
        m_input_stream->dequeue(badge(), bytes);
    }

    Coroutine<ErrorOr<size_t>> write_some(ReadonlyBytes buffer) override
    {
        auto result = co_await m_output_stream->write_some(buffer);
        if (result.is_error()) {
            m_is_open = false;
            m_input_stream->reset();
        }
        co_return result;
    }

    Coroutine<ErrorOr<void>> write(ReadonlySpan<ReadonlyBytes> buffers) override
    {
        auto result = co_await m_output_stream->write(buffers);
        if (result.is_error()) {
            m_is_open = false;
            m_input_stream->reset();
        }
        co_return result;
    }

private:
    NonnullOwnPtr<AsyncInputStream> m_input_stream;
    NonnullOwnPtr<AsyncOutputStream> m_output_stream;
    bool m_is_open { true };
};

}

#ifdef USING_AK_GLOBALLY
using AK::AsyncInputStreamSlice;
using AK::AsyncStreamHelpers;
using AK::AsyncStreamPair;
#endif
