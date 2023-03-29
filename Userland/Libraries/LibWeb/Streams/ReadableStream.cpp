/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#rs-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStream>> ReadableStream::construct_impl(JS::Realm& realm)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<ReadableStream>(realm, realm));
}

ReadableStream::ReadableStream(JS::Realm& realm)
    : PlatformObject(realm)
{
}

ReadableStream::~ReadableStream() = default;

JS::ThrowCompletionOr<void> ReadableStream::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ReadableStreamPrototype>(realm, "ReadableStream"));

    return {};
}

void ReadableStream::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_controller);
    visitor.visit(m_stored_error);
}

// https://streams.spec.whatwg.org/#readablestream-locked
bool ReadableStream::is_readable() const
{
    // A ReadableStream stream is readable if stream.[[state]] is "readable".
    return m_state == State::Readable;
}

// https://streams.spec.whatwg.org/#readablestream-closed
bool ReadableStream::is_closed() const
{
    // A ReadableStream stream is closed if stream.[[state]] is "closed".
    return m_state == State::Closed;
}

// https://streams.spec.whatwg.org/#readablestream-errored
bool ReadableStream::is_errored() const
{
    // A ReadableStream stream is errored if stream.[[state]] is "errored".
    return m_state == State::Errored;
}
// https://streams.spec.whatwg.org/#readablestream-locked
bool ReadableStream::is_locked() const
{
    // A ReadableStream stream is locked if ! IsReadableStreamLocked(stream) returns true.
    return is_readable_stream_locked(*this);
}

// https://streams.spec.whatwg.org/#is-readable-stream-disturbed
bool ReadableStream::is_disturbed() const
{
    // A ReadableStream stream is disturbed if stream.[[disturbed]] is true.
    return m_disturbed;
}

}
