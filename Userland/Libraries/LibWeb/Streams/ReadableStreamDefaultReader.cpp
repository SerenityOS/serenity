/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/ReadableStreamDefaultReaderPrototype.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamDefaultReader.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#default-reader-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamDefaultReader>> ReadableStreamDefaultReader::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<ReadableStream> stream)
{
    auto reader = TRY(realm.heap().allocate<ReadableStreamDefaultReader>(realm, realm));

    // 1. Perform ? SetUpReadableStreamDefaultReader(this, stream);
    TRY(set_up_readable_stream_default_reader(reader, *stream));

    return reader;
}

ReadableStreamDefaultReader::ReadableStreamDefaultReader(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

JS::ThrowCompletionOr<void> ReadableStreamDefaultReader::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ReadableStreamDefaultReaderPrototype>(realm, "ReadableStreamDefaultReader"));

    return {};
}

void ReadableStreamDefaultReader::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    ReadableStreamGenericReaderMixin::visit_edges(visitor);
}

class DefaultReaderReadRequest : public ReadRequest {
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
    JS::Realm& m_realm;
    WebIDL::Promise& m_promise;
};

// https://streams.spec.whatwg.org/#default-reader-read
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> ReadableStreamDefaultReader::read()
{
    auto& realm = this->realm();

    // 1. If this.[[stream]] is undefined, return a promise rejected with a TypeError exception.
    if (!m_stream) {
        auto exception = TRY(JS::TypeError::create(realm, "Cannot read from an empty stream"sv));
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
    auto read_request = adopt_ref(*new DefaultReaderReadRequest(realm, promise_capability));

    // 4. Perform ! ReadableStreamDefaultReaderRead(this, readRequest).
    TRY(readable_stream_default_reader_read(*this, read_request));

    // 5. Return promise.
    return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise_capability->promise()) };
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
