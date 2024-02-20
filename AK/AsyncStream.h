/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/Coroutine.h>
#include <AK/Error.h>
#include <AK/ScopeGuard.h>

namespace AK {

// AsyncResource represents a generic resource (e. g. POSIX file descriptor, AsyncStream, HTTP
// response body) with a failible and/or asynchronous destructor. Refer to AsynchronousDesign.md
// documentation page for a description tailored for users of the asynchronous resources.
//
// In order to correctly implement methods of AsyncResource, you first have to define (not
// necessarily in code) two abstract operations: Close and Reset. They should have the following
// semantics:
//
//  * Close AO:
//     1. Assert that nobody is awaiting on a resource.
//     2. Ensure that further attempts to wait on a resource will assert.
//     3. Shutdown (possibly asynchronously) the associated low-level resource. Shutdown must ensure
//        that if the state of a resource is clean, it will remain so indefinitely. The "clean"
//        state is resource-specific--for example, streams might define it as "no outstanding writes
//        and no unread data".
//     4. Check if the state of the resource is clean. If it is not, call Reset AO and return an
//        error (preferably, EBUSY).
//     5. Free (possibly asynchronously) the associated low-level resource.
//     6. Return success.
//
//  * Reset AO:
//     1. Schedule returning an error (preferably, ECANCELED) from the current resource awaiters.
//     2. Ensure that further attempts to wait on a resource will assert.
//     3. Free synchronously the associated low-level resource. Preferably, this should be done in a
//        way that cleanly indicates an error for the event producer.
//     4. Return synchronously.
class AsyncResource {
    AK_MAKE_NONCOPYABLE(AsyncResource);
    AK_MAKE_NONMOVABLE(AsyncResource);

public:
    AsyncResource() = default;

    // Destructor of an AsyncResource must perform the following steps when called:
    // 1. Assert that nobody is awaiting on the resource.
    // 2. If resource is open, perform Reset AO.
    virtual ~AsyncResource() = default;

    // reset() must perform the following steps when called:
    // 1. Assert that the resource is open.
    // 2. Perform Reset AO.
    virtual void reset() = 0;

    // close() must perform the following steps when called:
    // 1. Assert that the object is fully constructed. For example, a socket might assert that it is
    //    connected.
    // 2. Assert that the resource is open.
    // 3. Perform Close AO, await and return its result.
    virtual Coroutine<ErrorOr<void>> close() = 0;

    // Resource is said to be in an error state if either Reset AO was invoked or if an operation on
    // a resource has failed and an implementation deemed the error unrecoverable. If a resource is
    // being transitioned to an error state because of an internal error, Reset AO (or its
    // equivalent) must be executed by an implementation. Resource is said to be open if it is not
    // in a error state and Close AO has never been called on it.
    virtual bool is_open() const = 0;
};

// AsyncInputStream is a base class for all asynchronous input streams. Refer to
// AsynchronousDesign.md documentation page for a description tailored for users of the streams.
//
// In order to implement a brand new AsyncInputStream, you generally have to define a destructor and
// overload six virtual functions: 3 from AsyncResource and 3 from AsyncInputStream. When
// implementing the AsyncResource interface, please note that AsyncInputStream is considered clean
// if there's no data left to be read.
class AsyncInputStream : public virtual AsyncResource {
public:
    struct PeekOrEofResult {
        ReadonlyBytes data;
        bool is_eof;
    };

    AsyncInputStream() = default;

    ReadonlyBytes buffered_data() const
    {
        VERIFY(is_open());
        return buffered_data_unchecked({});
    }

    Coroutine<ErrorOr<PeekOrEofResult>> peek_or_eof()
    {
        VERIFY(is_open());
        if (!m_is_reading_peek) {
            m_is_reading_peek = true;
            auto data = buffered_data_unchecked({});
            if (!data.is_empty())
                co_return PeekOrEofResult { data, false };
        }
        bool is_not_eof = CO_TRY(co_await enqueue_some({}));
        co_return PeekOrEofResult { buffered_data_unchecked({}), !is_not_eof };
    }

    Coroutine<ErrorOr<ReadonlyBytes>> peek()
    {
        auto [data, is_eof] = CO_TRY(co_await peek_or_eof());
        if (is_eof) {
            reset();
            co_return Error::from_errno(EIO);
        }
        co_return data;
    }

    Coroutine<ErrorOr<ReadonlyBytes>> read(size_t bytes)
    {
        m_is_reading_peek = false;

        if (bytes) {
            auto buffer = buffered_data();
            while (buffer.size() < bytes) {
                if (!CO_TRY(co_await enqueue_some({}))) {
                    reset();
                    co_return Error::from_errno(EIO);
                }
                buffer = buffered_data_unchecked({});
            }
            dequeue({}, bytes);
            co_return buffer.slice(0, bytes);
        } else {
            co_return Bytes {};
        }
    }

    template<typename T>
    Coroutine<ErrorOr<T>> read_object()
    {
        auto bytes = CO_TRY(co_await read(sizeof(T)));
        union {
            T object;
            char representation[sizeof(T)];
        } reinterpreter = {};
        memcpy(&reinterpreter, bytes.data(), sizeof(T));
        co_return reinterpreter.object;
    }

    // If EOF has not been reached, `enqueue_some` should read at least one byte from the underlying
    // stream to the internal buffer and return true. Otherwise, it must not change the buffer and
    // return false. If read fails and, consequently, `enqueue_some` returns Error, it must
    // perform Reset AO (or an equivalent of it). Therefore, all reading errors are considered fatal
    // for AsyncInputStream. Additionally, implementation must assert if `enqueue_some` is called
    // concurrently. This is the only method that can be interrupted by `reset`.
    virtual Coroutine<ErrorOr<bool>> enqueue_some(Badge<AsyncInputStream>) = 0;

    // `buffered_data_unchecked` should just return a view of the buffer. It must not invalidate
    // previously returned views of the buffer.
    virtual ReadonlyBytes buffered_data_unchecked(Badge<AsyncInputStream>) const = 0;

    // `dequeue` should remove `bytes` bytes from the buffer. It is guaranteed that this amount of
    // bytes will be present in the buffer at the point of the call. `dequeue` must not invalidate
    // previously returned views of the buffer. There are some restrictions on `bytes` parameter
    // originating from the length condition (see documentation), so if you just use
    // AsyncStreamBuffer as the stream buffer, `dequeue` and `enqueue_some` will have amortized
    // O(stream_length) complexity.
    virtual void dequeue(Badge<AsyncInputStream>, size_t bytes) = 0;

protected:
    static Badge<AsyncInputStream> badge() { return {}; }

    bool m_is_reading_peek { false };
};

class AsyncOutputStream : public virtual AsyncResource {
public:
    AsyncOutputStream() = default;

    virtual Coroutine<ErrorOr<size_t>> write_some(ReadonlyBytes buffer) = 0;

    virtual Coroutine<ErrorOr<void>> write(ReadonlySpan<ReadonlyBytes> buffers)
    {
        for (auto buffer : buffers) {
            while (!buffer.is_empty()) {
                auto nwritten = CO_TRY(co_await write_some(buffer));
                buffer = buffer.slice(nwritten);
            }
        }
        co_return {};
    }
};

class AsyncStream
    : public AsyncInputStream
    , public AsyncOutputStream {
public:
    AsyncStream() = default;
};

template<typename T>
class StreamWrapper : public virtual AsyncResource {
public:
    StreamWrapper(NonnullOwnPtr<T>&& stream)
        : m_stream(move(stream))
    {
    }

    void reset() override { return m_stream->reset(); }
    Coroutine<ErrorOr<void>> close() override { return m_stream->close(); }
    bool is_open() const override { return m_stream->is_open(); }

protected:
    NonnullOwnPtr<T> m_stream;
};

}

#ifdef USING_AK_GLOBALLY
using AK::AsyncInputStream;
using AK::AsyncOutputStream;
using AK::AsyncResource;
using AK::AsyncStream;
using AK::StreamWrapper;
#endif
