/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamGenericReader.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#generic-reader-closed
WebIDL::ExceptionOr<JS::GCPtr<JS::Promise>> ReadableStreamGenericReaderMixin::closed()
{
    // 1. Return this.[[closedPromise]].
    return JS::GCPtr { verify_cast<JS::Promise>(*m_closed_promise->promise()) };
}

// https://streams.spec.whatwg.org/#generic-reader-cancel
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> ReadableStreamGenericReaderMixin::cancel(JS::Value reason)
{
    // 1. If this.[[stream]] is undefined, return a promise rejected with a TypeError exception.
    if (!m_stream) {
        auto exception = JS::TypeError::create(m_realm, "No stream present to cancel"sv);
        auto promise_capability = WebIDL::create_rejected_promise(m_realm, exception);
        return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise_capability->promise().ptr()) };
    }

    // 2. Return ! ReadableStreamReaderGenericCancel(this, reason).
    auto promise_capability = TRY(readable_stream_reader_generic_cancel(*this, reason));
    return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise_capability->promise().ptr()) };
}

ReadableStreamGenericReaderMixin::ReadableStreamGenericReaderMixin(JS::Realm& realm)
    : m_realm(realm)
{
}

void ReadableStreamGenericReaderMixin::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_closed_promise);
    visitor.visit(m_stream);
    visitor.visit(m_realm);
}

}
