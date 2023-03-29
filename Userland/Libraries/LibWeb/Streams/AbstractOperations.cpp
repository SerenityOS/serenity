/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamDefaultReader.h>
#include <LibWeb/Streams/ReadableStreamGenericReader.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#is-readable-stream-locked
bool is_readable_stream_locked(ReadableStream const& stream)
{
    // 1. If stream.[[reader]] is undefined, return false.
    if (!stream.reader())
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

    // 6. If reader implements ReadableStreamDefaultReader,
    if (reader->is_default_reader()) {
        // 1. Let readRequests be reader.[[readRequests]].
        // 2. Set reader.[[readRequests]] to an empty list.
        auto read_requests = move(reader->read_requests());

        // 3. For each readRequest of readRequests,
        for (auto& read_request : read_requests) {
            // 1. Perform readRequest’s close steps.
            read_request->on_close();
        }
    }
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

// https://streams.spec.whatwg.org/#readable-stream-reader-generic-initialize
void readable_stream_reader_generic_initialize(ReadableStreamGenericReaderMixin& reader, ReadableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Set reader.[[stream]] to stream.
    reader.set_stream(stream);

    // 2. Set stream.[[reader]] to reader.
    if (reader.is_default_reader()) {
        stream.set_reader(static_cast<ReadableStreamDefaultReader&>(reader));
    } else {
        // FIXME: Handle other descendents of ReadableStreamGenericReaderMixin (i.e. BYOBReader)
        TODO();
    }

    // 3. If stream.[[state]] is "readable",
    if (stream.is_readable()) {
        // 1. Set reader.[[closedPromise]] to a new promise.
        reader.set_closed_promise_capability(WebIDL::create_promise(realm));
    }
    // 4. Otherwise, if stream.[[state]] is "closed",
    else if (stream.is_closed()) {
        // 1. Set reader.[[closedPromise]] to a promise resolved with undefined.
        reader.set_closed_promise_capability(WebIDL::create_resolved_promise(realm, JS::js_undefined()));
    }
    // 5. Otherwise,
    else {
        // 1. Assert: stream.[[state]] is "errored".
        VERIFY(stream.is_errored());

        // 2. Set reader.[[closedPromise]] to a promise rejected with stream.[[storedError]].
        reader.set_closed_promise_capability(WebIDL::create_rejected_promise(realm, stream.stored_error()));

        // 3. Set reader.[[closedPromise]].[[PromiseIsHandled]] to true.
        WebIDL::mark_promise_as_handled(*reader.closed_promise_capability());
    }
}

// https://streams.spec.whatwg.org/#readable-stream-reader-generic-release
WebIDL::ExceptionOr<void> readable_stream_reader_generic_release(ReadableStreamGenericReaderMixin& reader)
{
    // 1. Let stream be reader.[[stream]].
    auto stream = reader.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Assert: stream.[[reader]] is reader.
    VERIFY(stream->reader().ptr() == &reader);

    // 4. If stream.[[state]] is "readable", reject reader.[[closedPromise]] with a TypeError exception.
    auto exception = TRY(JS::TypeError::create(stream->realm(), "Released readable stream"sv));
    if (stream->is_readable()) {
        WebIDL::reject_promise(stream->realm(), *reader.closed_promise_capability(), exception);
    }
    // 5. Otherwise, set reader.[[closedPromise]] to a promise rejected with a TypeError exception.
    else {
        reader.set_closed_promise_capability(WebIDL::create_rejected_promise(stream->realm(), exception));
    }

    // 6. Set reader.[[closedPromise]].[[PromiseIsHandled]] to true.
    WebIDL::mark_promise_as_handled(*reader.closed_promise_capability());

    // FIXME: 7. Perform ! stream.[[controller]].[[ReleaseSteps]]().

    // 8. Set stream.[[reader]] to undefined.
    stream->set_reader({});

    // 9. Set reader.[[stream]] to undefined.
    reader.set_stream({});

    return {};
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablestreamdefaultreadererrorreadrequests
void readable_stream_default_reader_error_read_requests(ReadableStreamDefaultReader& reader, JS::Value error)
{
    // 1. Let readRequests be reader.[[readRequests]].
    // 2. Set reader.[[readRequests]] to a new empty list.
    auto read_requests = move(reader.read_requests());

    // 3. For each readRequest of readRequests,
    for (auto& read_request : read_requests) {
        // 1. Perform readRequest’s error steps, given e.
        read_request->on_error(error);
    }
}

// https://streams.spec.whatwg.org/#readable-stream-default-reader-read
void readable_stream_default_reader_read(ReadableStreamDefaultReader& reader, ReadRequest& read_request)
{
    // 1. Let stream be reader.[[stream]].
    auto stream = reader.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Set stream.[[disturbed]] to true.
    stream->set_disturbed(true);

    // 4. If stream.[[state]] is "closed", perform readRequest’s close steps.
    if (stream->is_closed()) {
        read_request.on_close();
    }
    // 5. Otherwise, if stream.[[state]] is "errored", perform readRequest’s error steps given stream.[[storedError]].
    else if (stream->is_errored()) {
        read_request.on_error(stream->stored_error());
    }
    // 6. Otherwise,
    else {
        // 1. Assert: stream.[[state]] is "readable".
        VERIFY(stream->is_readable());

        // FIXME: 2. Perform ! stream.[[controller]].[[PullSteps]](readRequest).
    }
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablestreamdefaultreaderrelease
WebIDL::ExceptionOr<void> readable_stream_default_reader_release(ReadableStreamDefaultReader& reader)
{
    // 1. Perform ! ReadableStreamReaderGenericRelease(reader).
    TRY(readable_stream_reader_generic_release(reader));

    // 2. Let e be a new TypeError exception.
    auto e = TRY(JS::TypeError::create(reader.realm(), "Reader has been released"sv));

    // 3. Perform ! ReadableStreamDefaultReaderErrorReadRequests(reader, e).
    readable_stream_default_reader_error_read_requests(reader, e);

    return {};
}

// https://streams.spec.whatwg.org/#set-up-readable-stream-default-reader
WebIDL::ExceptionOr<void> set_up_readable_stream_default_reader(ReadableStreamDefaultReader& reader, ReadableStream& stream)
{
    // 1. If ! IsReadableStreamLocked(stream) is true, throw a TypeError exception.
    if (is_readable_stream_locked(stream))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot create stream reader for a locked stream"sv };

    // 2. Perform ! ReadableStreamReaderGenericInitialize(reader, stream).
    // 3. Set reader.[[readRequests]] to a new empty list.
    readable_stream_reader_generic_initialize(reader, stream);

    return {};
}

}
