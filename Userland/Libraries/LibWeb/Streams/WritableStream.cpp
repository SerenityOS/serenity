/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/WritableStreamPrototype.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/UnderlyingSink.h>
#include <LibWeb/Streams/WritableStream.h>
#include <LibWeb/Streams/WritableStreamDefaultController.h>
#include <LibWeb/Streams/WritableStreamDefaultWriter.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#ws-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStream>> WritableStream::construct_impl(JS::Realm& realm, Optional<JS::Handle<JS::Object>> const& underlying_sink_object, QueuingStrategy const& strategy)
{
    auto& vm = realm.vm();

    auto writable_stream = realm.heap().allocate<WritableStream>(realm, realm);

    // 1. If underlyingSink is missing, set it to null.
    auto underlying_sink = underlying_sink_object.has_value() ? JS::Value(underlying_sink_object.value().ptr()) : JS::js_null();

    // 2. Let underlyingSinkDict be underlyingSink, converted to an IDL value of type UnderlyingSink.
    auto underlying_sink_dict = TRY(UnderlyingSink::from_value(vm, underlying_sink));

    // 3. If underlyingSinkDict["type"] exists, throw a RangeError exception.
    if (underlying_sink_dict.type.has_value())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Invalid use of reserved key 'type'"sv };

    // 4. Perform ! InitializeWritableStream(this).
    // Note: This AO configures slot values which are already specified in the class's field initializers.

    // 5. Let sizeAlgorithm be ! ExtractSizeAlgorithm(strategy).
    auto size_algorithm = extract_size_algorithm(strategy);

    // 6. Let highWaterMark be ? ExtractHighWaterMark(strategy, 1).
    auto high_water_mark = TRY(extract_high_water_mark(strategy, 1));

    // 7. Perform ? SetUpWritableStreamDefaultControllerFromUnderlyingSink(this, underlyingSink, underlyingSinkDict, highWaterMark, sizeAlgorithm).
    TRY(set_up_writable_stream_default_controller_from_underlying_sink(*writable_stream, underlying_sink, underlying_sink_dict, high_water_mark, move(size_algorithm)));

    return writable_stream;
}

// https://streams.spec.whatwg.org/#ws-locked
bool WritableStream::locked() const
{
    // 1. Return ! IsWritableStreamLocked(this).
    return is_writable_stream_locked(*this);
}

// https://streams.spec.whatwg.org/#ws-close
WebIDL::ExceptionOr<JS::GCPtr<JS::Object>> WritableStream::close()
{
    auto& realm = this->realm();

    // 1. If ! IsWritableStreamLocked(this) is true, return a promise rejected with a TypeError exception.
    if (is_writable_stream_locked(*this)) {
        auto exception = JS::TypeError::create(realm, "Cannot close a locked stream"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 2. If ! WritableStreamCloseQueuedOrInFlight(this) is true, return a promise rejected with a TypeError exception.
    if (writable_stream_close_queued_or_in_flight(*this)) {
        auto exception = JS::TypeError::create(realm, "Cannot close a stream that is already closed or errored"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 3. Return ! WritableStreamClose(this).
    return TRY(writable_stream_close(*this))->promise();
}

// https://streams.spec.whatwg.org/#ws-abort
WebIDL::ExceptionOr<JS::GCPtr<JS::Object>> WritableStream::abort(JS::Value reason)
{
    auto& realm = this->realm();

    // 1. If ! IsWritableStreamLocked(this) is true, return a promise rejected with a TypeError exception.
    if (is_writable_stream_locked(*this)) {
        auto exception = JS::TypeError::create(realm, "Cannot abort a locked stream"sv);
        return WebIDL::create_rejected_promise(realm, exception)->promise();
    }

    // 2. Return ! WritableStreamAbort(this, reason).
    return TRY(writable_stream_abort(*this, reason))->promise();
}

// https://streams.spec.whatwg.org/#ws-get-writer
WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStreamDefaultWriter>> WritableStream::get_writer()
{
    // 1. Return ? AcquireWritableStreamDefaultWriter(this).
    return acquire_writable_stream_default_writer(*this);
}

WritableStream::WritableStream(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

void WritableStream::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WritableStreamPrototype>(realm, "WritableStream"));
}

void WritableStream::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_close_request);
    visitor.visit(m_controller);
    visitor.visit(m_in_flight_write_request);
    visitor.visit(m_in_flight_close_request);
    if (m_pending_abort_request.has_value()) {
        visitor.visit(m_pending_abort_request->promise);
        visitor.visit(m_pending_abort_request->reason);
    }
    visitor.visit(m_stored_error);
    visitor.visit(m_writer);
    for (auto& write_request : m_write_requests)
        visitor.visit(write_request);
}

}
