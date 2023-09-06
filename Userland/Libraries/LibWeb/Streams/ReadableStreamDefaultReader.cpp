/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/ReadableStreamDefaultReaderPrototype.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamDefaultReader.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

void ReadLoopReadRequest::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_realm);
    visitor.visit(m_reader);
}

// https://streams.spec.whatwg.org/#default-reader-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamDefaultReader>> ReadableStreamDefaultReader::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<ReadableStream> stream)
{
    auto reader = realm.heap().allocate<ReadableStreamDefaultReader>(realm, realm);

    // 1. Perform ? SetUpReadableStreamDefaultReader(this, stream);
    TRY(set_up_readable_stream_default_reader(reader, *stream));

    return reader;
}

ReadableStreamDefaultReader::ReadableStreamDefaultReader(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
    , ReadableStreamGenericReaderMixin(realm)
{
}

void ReadableStreamDefaultReader::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ReadableStreamDefaultReaderPrototype>(realm, "ReadableStreamDefaultReader"));
}

void ReadableStreamDefaultReader::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    ReadableStreamGenericReaderMixin::visit_edges(visitor);
    for (auto& request : m_read_requests)
        visitor.visit(request);
}

// https://streams.spec.whatwg.org/#read-loop
ReadLoopReadRequest::ReadLoopReadRequest(JS::VM& vm, JS::Realm& realm, ReadableStreamDefaultReader& reader, SuccessSteps success_steps, FailureSteps failure_steps)
    : m_vm(vm)
    , m_realm(realm)
    , m_reader(reader)
    , m_success_steps(move(success_steps))
    , m_failure_steps(move(failure_steps))
{
}

// chunk steps, given chunk
void ReadLoopReadRequest::on_chunk(JS::Value chunk)
{
    // 1. If chunk is not a Uint8Array object, call failureSteps with a TypeError and abort these steps.
    if (!chunk.is_object() || !is<JS::Uint8Array>(chunk.as_object())) {
        m_failure_steps(JS::TypeError::create(m_realm, "Chunk data is not Uint8Array"sv));
        return;
    }

    auto const& array = static_cast<JS::Uint8Array const&>(chunk.as_object());
    auto const& buffer = array.viewed_array_buffer()->buffer();

    // 2. Append the bytes represented by chunk to bytes.
    m_bytes.append(buffer);

    // FIXME: As the spec suggests, implement this non-recursively - instead of directly. It is not too big of a deal currently
    //        as we enqueue the entire blob buffer in one go, meaning that we only recurse a single time. Once we begin queuing
    //        up more than one chunk at a time, we may run into stack overflow problems.
    //
    // 3. Read-loop given reader, bytes, successSteps, and failureSteps.
    auto maybe_error = readable_stream_default_reader_read(m_reader, *this);
    if (maybe_error.is_exception()) {
        auto throw_completion = Bindings::dom_exception_to_throw_completion(m_vm, maybe_error.exception());
        m_failure_steps(*throw_completion.release_error().value());
    }
}

// close steps
void ReadLoopReadRequest::on_close()
{
    // 1. Call successSteps with bytes.
    m_success_steps(m_bytes);
}

// error steps, given e
void ReadLoopReadRequest::on_error(JS::Value error)
{
    // 1. Call failureSteps with e.
    m_failure_steps(error);
}

class DefaultReaderReadRequest final : public ReadRequest {
    JS_CELL(DefaultReaderReadRequest, Cell);

public:
    DefaultReaderReadRequest(JS::Realm& realm, WebIDL::Promise& promise)
        : m_realm(realm)
        , m_promise(promise)
    {
    }

    virtual void on_chunk(JS::Value chunk) override
    {
        WebIDL::resolve_promise(m_realm, m_promise, JS::create_iterator_result_object(m_realm.vm(), chunk, false));
    }

    virtual void on_close() override
    {
        WebIDL::resolve_promise(m_realm, m_promise, JS::create_iterator_result_object(m_realm.vm(), JS::js_undefined(), true));
    }

    virtual void on_error(JS::Value error) override
    {
        WebIDL::reject_promise(m_realm, m_promise, error);
    }

private:
    virtual void visit_edges(Visitor& visitor) override
    {
        Base::visit_edges(visitor);
        visitor.visit(m_realm);
        visitor.visit(m_promise);
    }

    JS::Realm& m_realm;
    WebIDL::Promise& m_promise;
};

// https://streams.spec.whatwg.org/#default-reader-read
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> ReadableStreamDefaultReader::read()
{
    auto& realm = this->realm();

    // 1. If this.[[stream]] is undefined, return a promise rejected with a TypeError exception.
    if (!m_stream) {
        auto exception = JS::TypeError::create(realm, "Cannot read from an empty stream"sv);
        auto promise_capability = WebIDL::create_rejected_promise(realm, exception);
        return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise_capability->promise()) };
    }

    // 2. Let promise be a new promise.
    auto promise_capability = WebIDL::create_promise(realm);

    // 3. Let readRequest be a new read request with the following items:
    //    chunk steps, given chunk
    //        Resolve promise with «[ "value" → chunk, "done" → false ]».
    //    close steps
    //        Resolve promise with «[ "value" → undefined, "done" → true ]».
    //    error steps, given e
    //        Reject promise with e.
    auto read_request = heap().allocate_without_realm<DefaultReaderReadRequest>(realm, promise_capability);

    // 4. Perform ! ReadableStreamDefaultReaderRead(this, readRequest).
    TRY(readable_stream_default_reader_read(*this, read_request));

    // 5. Return promise.
    return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise_capability->promise()) };
}

// https://streams.spec.whatwg.org/#readablestreamdefaultreader-read-all-bytes
WebIDL::ExceptionOr<void> ReadableStreamDefaultReader::read_all_bytes(ReadLoopReadRequest::SuccessSteps success_steps, ReadLoopReadRequest::FailureSteps failure_steps)
{
    auto& realm = this->realm();
    auto& vm = realm.vm();

    // 1. Let readRequest be a new read request with the following items:
    //    NOTE: items and steps in ReadLoopReadRequest.
    auto read_request = heap().allocate_without_realm<ReadLoopReadRequest>(vm, realm, *this, move(success_steps), move(failure_steps));

    // 2. Perform ! ReadableStreamDefaultReaderRead(this, readRequest).
    TRY(readable_stream_default_reader_read(*this, read_request));

    return {};
}

// FIXME: This function is a promise-based wrapper around "read all bytes". The spec changed this function to not use promises
//        in https://github.com/whatwg/streams/commit/f894acdd417926a2121710803cef593e15127964 - however, it seems that the
//        FileAPI blob specification has not been updated to match, see: https://github.com/w3c/FileAPI/issues/187.
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> ReadableStreamDefaultReader::read_all_bytes_deprecated()
{
    auto& realm = this->realm();

    auto promise = WebIDL::create_promise(realm);

    auto success_steps = [promise, &realm](ByteBuffer bytes) {
        auto buffer = JS::ArrayBuffer::create(realm, move(bytes));
        WebIDL::resolve_promise(realm, promise, buffer);
    };

    auto failure_steps = [promise, &realm](JS::Value error) {
        WebIDL::reject_promise(realm, promise, error);
    };

    TRY(read_all_bytes(move(success_steps), move(failure_steps)));

    return promise;
}

// https://streams.spec.whatwg.org/#default-reader-release-lock
WebIDL::ExceptionOr<void> ReadableStreamDefaultReader::release_lock()
{
    // 1. If this.[[stream]] is undefined, return.
    if (!m_stream)
        return {};

    // 2. Perform ! ReadableStreamDefaultReaderRelease(this).
    return readable_stream_default_reader_release(*this);
}

}
