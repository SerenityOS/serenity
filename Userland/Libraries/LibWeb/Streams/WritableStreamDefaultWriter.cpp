/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/WritableStreamDefaultWriterPrototype.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/WritableStream.h>
#include <LibWeb/Streams/WritableStreamDefaultWriter.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStreamDefaultWriter>> WritableStreamDefaultWriter::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<WritableStream> stream)
{
    auto writer = realm.heap().allocate<WritableStreamDefaultWriter>(realm, realm);

    // 1. Perform ? SetUpWritableStreamDefaultWriter(this, stream).
    TRY(set_up_writable_stream_default_writer(*writer, stream));

    return writer;
}

// https://streams.spec.whatwg.org/#default-writer-closed
JS::GCPtr<JS::Object> WritableStreamDefaultWriter::closed()
{
    // 1. Return this.[[closedPromise]].
    return m_closed_promise->promise();
}

// https://streams.spec.whatwg.org/#default-writer-desired-size
WebIDL::ExceptionOr<Optional<double>> WritableStreamDefaultWriter::desired_size() const
{
    // 1. If this.[[stream]] is undefined, throw a TypeError exception.
    if (!m_stream)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot get desired size of writer that has no locked stream"sv };

    // 2. Return ! WritableStreamDefaultWriterGetDesiredSize(this).
    return writable_stream_default_writer_get_desired_size(*this);
}

// https://streams.spec.whatwg.org/#default-writer-ready
JS::GCPtr<JS::Object> WritableStreamDefaultWriter::ready()
{
    // 1. Return this.[[readyPromise]].
    return m_ready_promise->promise();
}

// https://streams.spec.whatwg.org/#default-writer-abort
WebIDL::ExceptionOr<JS::GCPtr<JS::Object>> WritableStreamDefaultWriter::abort(JS::Value reason)
{
    auto& realm = this->realm();

    // 1. If this.[[stream]] is undefined, return a promise rejected with a TypeError exception.
    if (!m_stream) {
        auto exception = JS::TypeError::create(realm, "Cannot abort a writer that has no locked stream"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 2. Return ! WritableStreamDefaultWriterAbort(this, reason).
    return TRY(writable_stream_default_writer_abort(*this, reason))->promise();
}

// https://streams.spec.whatwg.org/#default-writer-close
WebIDL::ExceptionOr<JS::GCPtr<JS::Object>> WritableStreamDefaultWriter::close()
{
    auto& realm = this->realm();

    // 1. Let stream be this.[[stream]].

    // 2. If stream is undefined, return a promise rejected with a TypeError exception.
    if (!m_stream) {
        auto exception = JS::TypeError::create(realm, "Cannot close a writer that has no locked stream"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 3. If ! WritableStreamCloseQueuedOrInFlight(stream) is true, return a promise rejected with a TypeError exception.
    if (writable_stream_close_queued_or_in_flight(*m_stream)) {
        auto exception = JS::TypeError::create(realm, "Cannot close a stream that is already closed or errored"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 4. Return ! WritableStreamDefaultWriterClose(this).
    return TRY(writable_stream_default_writer_close(*this))->promise();
}

// https://streams.spec.whatwg.org/#default-writer-release-lock
WebIDL::ExceptionOr<void> WritableStreamDefaultWriter::release_lock()
{
    // 1. Let stream be this.[[stream]].

    // 2. If stream is undefined, return.
    if (!m_stream)
        return {};

    // 3. Assert: stream.[[writer]] is not undefined.
    VERIFY(m_stream->writer());

    // 4. Perform ! WritableStreamDefaultWriterRelease(this).
    return writable_stream_default_writer_release(*this);
}

// https://streams.spec.whatwg.org/#default-writer-write
WebIDL::ExceptionOr<JS::GCPtr<JS::Object>> WritableStreamDefaultWriter::write(JS::Value chunk)
{
    auto& realm = this->realm();

    // 1. If this.[[stream]] is undefined, return a promise rejected with a TypeError exception.
    if (!m_stream) {
        auto exception = JS::TypeError::create(realm, "Cannot write to a writer that has no locked stream"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 2. Return ! WritableStreamDefaultWriterWrite(this, chunk).
    return TRY(writable_stream_default_writer_write(*this, chunk))->promise();
}

WritableStreamDefaultWriter::WritableStreamDefaultWriter(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

void WritableStreamDefaultWriter::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WritableStreamDefaultWriterPrototype>(realm, "WritableStreamDefaultWriter"));
}

void WritableStreamDefaultWriter::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_closed_promise);
    visitor.visit(m_ready_promise);
    visitor.visit(m_stream);
}

}
