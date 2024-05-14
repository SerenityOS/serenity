/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AsyncStream.h>
#include <AK/Generator.h>
#include <AK/MaybeOwned.h>
#include <AK/TemporaryChange.h>

namespace AK {

template<typename T>
class AsyncStreamTransform : public AsyncInputStream {
public:
    AsyncStreamTransform(MaybeOwned<T>&& stream, AK::Generator<Empty, ErrorOr<void>>&& generator)
        : m_stream(move(stream))
        , m_generator(move(generator))
    {
    }

    ~AsyncStreamTransform()
    {
        // 1. Assert that nobody is awaiting on the resource.
        VERIFY(!m_generator_has_awaiters);

        // 2. If resource is open, perform Reset AO.
        if (is_open())
            reset();
    }

    void reset() override
    {
        VERIFY(is_open());
        m_stream->reset();
        if (!m_generator_has_awaiters)
            m_generator.destroy();
        m_is_open = false;
    }

    Coroutine<ErrorOr<void>> close() override
    {
        VERIFY(is_open());
        TemporaryChange await_guard(m_generator_has_awaiters, true);

        if (!m_generator.is_done()) {
            Variant<Empty, ErrorOr<void>> chunk_or_eof = co_await m_generator.next();
            if (chunk_or_eof.has<Empty>()) {
                reset();
                co_return Error::from_errno(EBUSY);
            } else {
                m_is_open = false;
                auto& error_or_eof = chunk_or_eof.get<ErrorOr<void>>();
                if (error_or_eof.is_error()) {
                    co_return error_or_eof.release_error();
                } else {
                    if (m_stream.is_owned())
                        CO_TRY(co_await m_stream->close());
                    co_return {};
                }
            }
        } else {
            m_is_open = false;
            if (m_stream.is_owned())
                CO_TRY(co_await m_stream->close());
            co_return {};
        }
    }

    bool is_open() const override
    {
        return m_is_open;
    }

    Coroutine<ErrorOr<bool>> enqueue_some(Badge<AsyncInputStream>) override
    {
        VERIFY(is_open());
        TemporaryChange await_guard(m_generator_has_awaiters, true);

        if (m_generator.is_done())
            co_return false;

        Variant<Empty, ErrorOr<void>> chunk_or_eof = co_await m_generator.next();
        if (chunk_or_eof.has<Empty>()) {
            co_return true;
        } else {
            auto& error_or_eof = chunk_or_eof.get<ErrorOr<void>>();
            if (error_or_eof.is_error()) {
                m_is_open = false;
                co_return error_or_eof.release_error();
            } else {
                co_return false;
            }
        }
    }

protected:
    using Generator = AK::Generator<Empty, ErrorOr<void>>;

    MaybeOwned<T> m_stream;

private:
    Generator m_generator;
    bool m_is_open { true };
    bool m_generator_has_awaiters { false };
};

}

#ifdef USING_AK_GLOBALLY
using AK::AsyncStreamTransform;
#endif
