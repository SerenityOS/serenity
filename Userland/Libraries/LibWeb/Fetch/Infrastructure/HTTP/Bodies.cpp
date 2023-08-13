/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/Fetch/Infrastructure/Task.h>
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
Body Body::clone(JS::Realm& realm) const
{
    // To clone a body body, run these steps:
    // FIXME: 1. Let « out1, out2 » be the result of teeing body’s stream.
    // FIXME: 2. Set body’s stream to out1.
    auto out2 = realm.heap().allocate<Streams::ReadableStream>(realm, realm);

    // 3. Return a body whose stream is out2 and other members are copied from body.
    return Body { JS::make_handle(out2), m_source, m_length };
}

// https://fetch.spec.whatwg.org/#body-fully-read
WebIDL::ExceptionOr<void> Body::fully_read(JS::Realm& realm, Web::Fetch::Infrastructure::Body::ProcessBodyCallback process_body, Web::Fetch::Infrastructure::Body::ProcessBodyErrorCallback process_body_error, TaskDestination task_destination) const
{
    auto& vm = realm.vm();

    // FIXME: 1. If taskDestination is null, then set taskDestination to the result of starting a new parallel queue.
    // FIXME: Handle 'parallel queue' task destination
    VERIFY(!task_destination.has<Empty>());
    auto task_destination_object = task_destination.get<JS::NonnullGCPtr<JS::Object>>();

    // 2. Let successSteps given a byte sequence bytes be to queue a fetch task to run processBody given bytes, with taskDestination.
    auto success_steps = [process_body = move(process_body), task_destination_object = JS::make_handle(task_destination_object)](ByteBuffer const& bytes) mutable -> ErrorOr<void> {
        // Make a copy of the bytes, as the source of the bytes may disappear between the time the task is queued and executed.
        auto bytes_copy = TRY(ByteBuffer::copy(bytes));
        queue_fetch_task(*task_destination_object, [process_body = move(process_body), bytes_copy = move(bytes_copy)]() {
            process_body(move(bytes_copy));
        });
        return {};
    };

    // 3. Let errorSteps optionally given an exception exception be to queue a fetch task to run processBodyError given exception, with taskDestination.
    auto error_steps = [process_body_error = move(process_body_error), task_destination_object = JS::make_handle(task_destination_object)](JS::GCPtr<WebIDL::DOMException> exception) mutable {
        queue_fetch_task(*task_destination_object, [process_body_error = move(process_body_error), exception = JS::make_handle(exception)]() {
            process_body_error(*exception);
        });
    };

    // 4. Let reader be the result of getting a reader for body’s stream. If that threw an exception, then run errorSteps with that exception and return.
    // 5. Read all bytes from reader, given successSteps and errorSteps.
    // FIXME: Implement the streams spec - this is completely made up for now :^)
    if (auto const* byte_buffer = m_source.get_pointer<ByteBuffer>()) {
        TRY_OR_THROW_OOM(vm, success_steps(*byte_buffer));
    } else if (auto const* blob_handle = m_source.get_pointer<JS::Handle<FileAPI::Blob>>()) {
        auto byte_buffer = TRY_OR_THROW_OOM(vm, ByteBuffer::copy((*blob_handle)->bytes()));
        TRY_OR_THROW_OOM(vm, success_steps(move(byte_buffer)));
    } else {
        // Empty, Blob, FormData
        error_steps(WebIDL::DOMException::create(realm, "DOMException", "Reading from Blob, FormData or null source is not yet implemented"sv));
    }
    return {};
}

// https://fetch.spec.whatwg.org/#byte-sequence-as-a-body
WebIDL::ExceptionOr<Body> byte_sequence_as_body(JS::Realm& realm, ReadonlyBytes bytes)
{
    // To get a byte sequence bytes as a body, return the body of the result of safely extracting bytes.
    auto [body, _] = TRY(safely_extract_body(realm, bytes));
    return body;
}

}
