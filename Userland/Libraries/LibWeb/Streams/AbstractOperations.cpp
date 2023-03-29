/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamGenericReader.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#is-readable-stream-locked
bool is_readable_stream_locked(ReadableStream const& stream)
{
    // 1. If stream.[[reader]] is undefined, return false.
    if (stream.reader() == nullptr)
        return false;

    // 2. Return true.
    return true;
}

// https://streams.spec.whatwg.org/#readable-stream-cancel
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> readable_stream_cancel(ReadableStream& stream, JS::Value reason)
{
    auto& realm = stream.realm();

    // 1. Set stream.[[disturbed]] to true.
    stream.set_disturbed(true);

    // 2. If stream.[[state]] is "closed", return a promise resolved with undefined.
    if (stream.is_closed())
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());

    // 3. If stream.[[state]] is "errored", return a promise rejected with stream.[[storedError]].
    if (stream.is_errored())
        return WebIDL::create_rejected_promise(realm, stream.stored_error());

    // 4. Perform ! ReadableStreamClose(stream).
    readable_stream_close(stream);

    // 5. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // FIXME:
    // 6. If reader is not undefined and reader implements ReadableStreamBYOBReader,
    //    1. Let readIntoRequests be reader.[[readIntoRequests]].
    //    2. Set reader.[[readIntoRequests]] to an empty list.
    //    3. For each readIntoRequest of readIntoRequests,
    //       1. Perform readIntoRequest’s close steps, given undefined.
    // 7. Let sourceCancelPromise be ! stream.[[controller]].[[CancelSteps]](reason).
    // 8. Return the result of reacting to sourceCancelPromise with a fulfillment step that returns undefined.
    (void)reader;
    (void)reason;

    return WebIDL::create_resolved_promise(realm, JS::js_undefined());
}

// https://streams.spec.whatwg.org/#readable-stream-close
void readable_stream_close(ReadableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[state]] is "readable".
    VERIFY(stream.is_readable());

    // 2. Set stream.[[state]] to "closed".
    stream.set_stream_state(ReadableStream::State::Closed);

    // 3. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 4. If reader is undefined, return.
    if (!reader)
        return;

    // 5. Resolve reader.[[closedPromise]] with undefined.
    WebIDL::resolve_promise(realm, *reader->closed_promise_capability());

    // FIXME:
    // 6. If reader implements ReadableStreamDefaultReader,
    //    1. Let readRequests be reader.[[readRequests]].
    //    2. Set reader.[[readRequests]] to an empty list.
    //    3. For each readRequest of readRequests,
    //       1. Perform readRequest’s close steps.
}

// https://streams.spec.whatwg.org/#readable-stream-reader-generic-cancel
JS::NonnullGCPtr<WebIDL::Promise> readable_stream_reader_generic_cancel(ReadableStreamGenericReaderMixin& reader, JS::Value reason)
{
    // 1. Let stream be reader.[[stream]]
    auto stream = reader.stream();

    // 2. Assert: stream is not undefined
    VERIFY(stream);

    // 3. Return ! ReadableStreamCancel(stream, reason)
    return MUST(readable_stream_cancel(*stream, reason));
}

}
