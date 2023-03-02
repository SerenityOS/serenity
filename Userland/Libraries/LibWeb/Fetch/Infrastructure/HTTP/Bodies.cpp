/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Fetch::Infrastructure {

Body::Body(JS::Handle<Streams::ReadableStream> stream)
    : m_stream(move(stream))
{
}

Body::Body(JS::Handle<Streams::ReadableStream> stream, SourceType source, Optional<u64> length)
    : m_stream(move(stream))
    , m_source(move(source))
    , m_length(move(length))
{
}

// https://fetch.spec.whatwg.org/#concept-body-clone
WebIDL::ExceptionOr<Body> Body::clone() const
{
    // To clone a body body, run these steps:

    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // FIXME: 1. Let « out1, out2 » be the result of teeing body’s stream.
    // FIXME: 2. Set body’s stream to out1.
    auto out2 = MUST_OR_THROW_OOM(vm.heap().allocate<Streams::ReadableStream>(realm, realm));

    // 3. Return a body whose stream is out2 and other members are copied from body.
    return Body { JS::make_handle(out2), m_source, m_length };
}

// https://fetch.spec.whatwg.org/#fully-reading-body-as-promise
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> Body::fully_read_as_promise() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // FIXME: Implement the streams spec - this is completely made up for now :^)
    if (auto const* byte_buffer = m_source.get_pointer<ByteBuffer>()) {
        // FIXME: The buffer may or may not be valid UTF-8.
        auto result = TRY_OR_THROW_OOM(vm, String::from_utf8(*byte_buffer));
        return WebIDL::create_resolved_promise(realm, JS::PrimitiveString::create(vm, move(result)));
    }
    // Empty, Blob, FormData
    return WebIDL::create_rejected_promise(realm, JS::InternalError::create(realm, "Reading body isn't fully implemented"sv).release_allocated_value_but_fixme_should_propagate_errors());
}

// https://fetch.spec.whatwg.org/#byte-sequence-as-a-body
WebIDL::ExceptionOr<Body> byte_sequence_as_body(JS::Realm& realm, ReadonlyBytes bytes)
{
    // To get a byte sequence bytes as a body, return the body of the result of safely extracting bytes.
    auto [body, _] = TRY(safely_extract_body(realm, bytes));
    return body;
}

}
