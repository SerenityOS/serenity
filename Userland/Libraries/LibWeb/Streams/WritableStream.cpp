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
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#ws-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStream>> WritableStream::construct_impl(JS::Realm& realm, Optional<JS::Handle<JS::Object>> const& underlying_sink_object)
{
    auto& vm = realm.vm();

    auto writable_stream = MUST_OR_THROW_OOM(realm.heap().allocate<WritableStream>(realm, realm));

    // 1. If underlyingSink is missing, set it to null.
    auto underlying_sink = underlying_sink_object.has_value() ? JS::Value(underlying_sink_object.value().ptr()) : JS::js_null();

    // 2. Let underlyingSinkDict be underlyingSink, converted to an IDL value of type UnderlyingSink.
    auto underlying_sink_dict = TRY(UnderlyingSink::from_value(vm, underlying_sink));

    // 3. If underlyingSinkDict["type"] exists, throw a RangeError exception.
    if (!underlying_sink_dict.type.has_value())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Invalid use of reserved key 'type'"sv };

    // 4. Perform ! InitializeWritableStream(this).
    // Note: This AO configures slot values which are already specified in the class's field initializers.

    // FIXME: 5. Let sizeAlgorithm be ! ExtractSizeAlgorithm(strategy).
    SizeAlgorithm size_algorithm = [](auto const&) { return JS::normal_completion(JS::Value(1)); };

    // FIXME: 6. Let highWaterMark be ? ExtractHighWaterMark(strategy, 1).
    auto high_water_mark = 1.0;

    // FIXME: 7. Perform ? SetUpWritableStreamDefaultControllerFromUnderlyingSink(this, underlyingSink, underlyingSinkDict, highWaterMark, sizeAlgorithm).
    (void)high_water_mark;

    return writable_stream;
}

// https://streams.spec.whatwg.org/#ws-locked
bool WritableStream::locked() const
{
    // 1. Return ! IsWritableStreamLocked(this).
    return is_writable_stream_locked(*this);
}

WritableStream::WritableStream(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

JS::ThrowCompletionOr<void> WritableStream::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WritableStreamPrototype>(realm, "WritableStream"));

    return {};
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
