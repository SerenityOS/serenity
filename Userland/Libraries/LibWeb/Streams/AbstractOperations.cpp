/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamDefaultController.h>
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
    (void)reader;

    // 7. Let sourceCancelPromise be ! stream.[[controller]].[[CancelSteps]](reason).
    auto source_cancel_promise = MUST(stream.controller()->cancel_steps(reason));

    // 8. Return the result of reacting to sourceCancelPromise with a fulfillment step that returns undefined.
    auto react_result = WebIDL::react_to_promise(*source_cancel_promise,
        [](auto const&) -> WebIDL::ExceptionOr<JS::Value> { return JS::js_undefined(); },
        {});

    return WebIDL::create_resolved_promise(realm, react_result);
}

// https://streams.spec.whatwg.org/#readable-stream-fulfill-read-request
void readable_stream_fulfill_read_request(ReadableStream& stream, JS::Value chunk, bool done)
{
    // 1. Assert: ! ReadableStreamHasDefaultReader(stream) is true.
    VERIFY(readable_stream_has_default_reader(stream));

    // 2. Let reader be stream.[[reader]].
    auto& reader = *stream.reader();

    // 3. Assert: reader.[[readRequests]] is not empty.
    VERIFY(!reader.read_requests().is_empty());

    // 4. Let readRequest be reader.[[readRequests]][0].
    // 5. Remove readRequest from reader.[[readRequests]].
    auto read_request = reader.read_requests().take_first();

    // 6. If done is true, perform readRequest’s close steps.
    if (done) {
        read_request->on_close();
    }
    // 7. Otherwise, perform readRequest’s chunk steps, given chunk.
    else {
        read_request->on_chunk(chunk);
    }
}

// https://streams.spec.whatwg.org/#readable-stream-get-num-read-requests
size_t readable_stream_get_num_read_requests(ReadableStream& stream)
{
    // 1. Assert: ! ReadableStreamHasDefaultReader(stream) is true.
    VERIFY(readable_stream_has_default_reader(stream));

    // 2. Return stream.[[reader]].[[readRequests]]'s size.
    return stream.reader()->read_requests().size();
}

// https://streams.spec.whatwg.org/#readable-stream-has-default-reader
bool readable_stream_has_default_reader(ReadableStream& stream)
{
    // 1. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 2. If reader is undefined, return false.
    if (!reader)
        return false;

    // 3. If reader implements ReadableStreamDefaultReader, return true.
    if (reader->is_default_reader())
        return true;

    // 4. Return false.
    return false;
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

// https://streams.spec.whatwg.org/#readable-stream-error
void readable_stream_error(ReadableStream& stream, JS::Value error)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[state]] is "readable".
    VERIFY(stream.is_readable());

    // 2. Set stream.[[state]] to "errored".
    stream.set_stream_state(ReadableStream::State::Errored);

    // 3. Set stream.[[storedError]] to e.
    stream.set_stored_error(error);

    // 4. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 5. If reader is undefined, return.
    if (!reader)
        return;

    // 6. Reject reader.[[closedPromise]] with e.
    WebIDL::reject_promise(realm, *reader->closed_promise_capability(), error);

    // 7. Set reader.[[closedPromise]].[[PromiseIsHandled]] to true.
    WebIDL::mark_promise_as_handled(*reader->closed_promise_capability());

    // 8. If reader implements ReadableStreamDefaultReader,
    if (reader->is_default_reader()) {
        // 1. Perform ! ReadableStreamDefaultReaderErrorReadRequests(reader, e).
        readable_stream_default_reader_error_read_requests(*reader, error);
    }
    // 9. Otherwise,
    else {
        // 1. Assert: reader implements ReadableStreamBYOBReader.
        // 2. Perform ! ReadableStreamBYOBReaderErrorReadIntoRequests(reader, e).

        // FIXME: Handle BYOBReader
        TODO();
    }
}

// https://streams.spec.whatwg.org/#readable-stream-add-read-request
void readable_stream_add_read_request(ReadableStream& stream, ReadRequest const& read_request)
{
    // FIXME: Check implementation type
    // 1. Assert: stream.[[reader]] implements ReadableStreamDefaultReader.
    VERIFY(stream.reader());

    // 2. Assert: stream.[[state]] is "readable".
    VERIFY(stream.is_readable());

    // 3. Append readRequest to stream.[[reader]].[[readRequests]].
    stream.reader()->read_requests().append(read_request);
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

    // 7. Perform ! stream.[[controller]].[[ReleaseSteps]]().
    stream->controller()->release_steps();

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

        // 2. Perform ! stream.[[controller]].[[PullSteps]](readRequest).
        MUST(stream->controller()->pull_steps(read_request));
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

// https://streams.spec.whatwg.org/#readable-stream-default-controller-close
void readable_stream_default_controller_close(ReadableStreamDefaultController& controller)
{
    // 1. If ! ReadableStreamDefaultControllerCanCloseOrEnqueue(controller) is false, return.
    if (!readable_stream_default_controller_can_close_or_enqueue(controller))
        return;

    // 2. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 3. Set controller.[[closeRequested]] to true.
    controller.set_close_requested(true);

    // 4. If controller.[[queue]] is empty,
    if (controller.queue().is_empty()) {
        // 1. Perform ! ReadableStreamDefaultControllerClearAlgorithms(controller).
        readable_stream_default_controller_clear_algorithms(controller);

        // 2. Perform ! ReadableStreamClose(stream).
        readable_stream_close(*stream);
    }
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-enqueue
WebIDL::ExceptionOr<void> readable_stream_default_controller_enqueue(ReadableStreamDefaultController& controller, JS::Value chunk)
{
    auto& vm = controller.vm();

    // 1. If ! ReadableStreamDefaultControllerCanCloseOrEnqueue(controller) is false, return.
    if (!readable_stream_default_controller_can_close_or_enqueue(controller))
        return {};

    // 2. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 3. If ! IsReadableStreamLocked(stream) is true and ! ReadableStreamGetNumReadRequests(stream) > 0, perform ! ReadableStreamFulfillReadRequest(stream, chunk, false).
    if (is_readable_stream_locked(*stream) && readable_stream_get_num_read_requests(*stream) > 0) {
        readable_stream_fulfill_read_request(*stream, chunk, false);
    }
    // 4. Otherwise,
    else {
        // 1. Let result be the result of performing controller.[[strategySizeAlgorithm]], passing in chunk, and interpreting the result as a completion record.
        auto result = (*controller.strategy_size_algorithm())(chunk);

        // 2. If result is an abrupt completion,
        if (result.is_abrupt()) {
            // 1. Perform ! ReadableStreamDefaultControllerError(controller, result.[[Value]]).
            readable_stream_default_controller_error(controller, result.value().value());

            // 2. Return result.
            return result;
        }

        // 3. Let chunkSize be result.[[Value]].
        auto chunk_size = TRY(result.release_value().release_value().to_double(vm));

        // 4. Let enqueueResult be EnqueueValueWithSize(controller, chunk, chunkSize).
        auto enqueue_result = enqueue_value_with_size(controller, chunk, chunk_size);

        // 5. If enqueueResult is an abrupt completion,
        if (enqueue_result.is_error()) {
            auto throw_completion = Bindings::throw_dom_exception_if_needed(vm, [&] { return enqueue_result; }).throw_completion();

            // 1. Perform ! ReadableStreamDefaultControllerError(controller, enqueueResult.[[Value]]).
            readable_stream_default_controller_error(controller, throw_completion.value().value());

            // 2. Return enqueueResult.
            return enqueue_result;
        }
    }

    // 5. Perform ! ReadableStreamDefaultControllerCallPullIfNeeded(controller).
    return readable_stream_default_controller_can_pull_if_needed(controller);
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-call-pull-if-needed
WebIDL::ExceptionOr<void> readable_stream_default_controller_can_pull_if_needed(ReadableStreamDefaultController& controller)
{
    // 1. Let shouldPull be ! ReadableStreamDefaultControllerShouldCallPull(controller).
    auto should_pull = readable_stream_default_controller_should_call_pull(controller);

    // 2. If shouldPull is false, return.
    if (!should_pull)
        return {};

    // 3. If controller.[[pulling]] is true,
    if (controller.pulling()) {
        // 1. Set controller.[[pullAgain]] to true.
        controller.set_pull_again(true);

        // 2. Return.
        return {};
    }

    // 4. Assert: controller.[[pullAgain]] is false.
    VERIFY(!controller.pull_again());

    // 5. Set controller.[[pulling]] to true.
    controller.set_pulling(true);

    // 6. Let pullPromise be the result of performing controller.[[pullAlgorithm]].
    auto pull_promise = TRY((*controller.pull_algorithm())());

    // 7. Upon fulfillment of pullPromise,
    WebIDL::upon_fulfillment(*pull_promise, [&](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Set controller.[[pulling]] to false.
        controller.set_pulling(false);

        // 2. If controller.[[pullAgain]] is true,
        if (controller.pull_again()) {
            // 1. Set controller.[[pullAgain]] to false.
            controller.set_pull_again(false);

            // 2. Perform ! ReadableStreamDefaultControllerCallPullIfNeeded(controller).
            TRY(readable_stream_default_controller_can_pull_if_needed(controller));
        }

        return JS::js_undefined();
    });

    // 8. Upon rejection of pullPromise with reason e,
    WebIDL::upon_rejection(*pull_promise, [&](auto const& e) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! ReadableStreamDefaultControllerError(controller, e).
        readable_stream_default_controller_error(controller, e);

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-should-call-pull
bool readable_stream_default_controller_should_call_pull(ReadableStreamDefaultController& controller)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If ! ReadableStreamDefaultControllerCanCloseOrEnqueue(controller) is false, return false.
    if (!readable_stream_default_controller_can_close_or_enqueue(controller))
        return false;

    // 3. If controller.[[started]] is false, return false.
    if (!controller.started())
        return false;

    // 4. If ! IsReadableStreamLocked(stream) is true and ! ReadableStreamGetNumReadRequests(stream) > 0, return true.
    if (is_readable_stream_locked(*stream) && readable_stream_get_num_read_requests(*stream) > 0)
        return true;

    // 5. Let desiredSize be ! ReadableStreamDefaultControllerGetDesiredSize(controller).
    auto desired_size = readable_stream_default_controller_get_desired_size(controller);

    // 6. Assert: desiredSize is not null.
    VERIFY(desired_size.has_value());

    // 7. If desiredSize > 0, return true.
    if (desired_size.release_value() > 0)
        return true;

    // 8. Return false.
    return false;
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-clear-algorithms
void readable_stream_default_controller_clear_algorithms(ReadableStreamDefaultController& controller)
{
    // 1. Set controller.[[pullAlgorithm]] to undefined.
    controller.set_pull_algorithm({});

    // 2. Set controller.[[cancelAlgorithm]] to undefined.
    controller.set_cancel_algorithm({});

    // 3. Set controller.[[strategySizeAlgorithm]] to undefined.
    controller.set_strategy_size_algorithm({});
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-error
void readable_stream_default_controller_error(ReadableStreamDefaultController& controller, JS::Value error)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If stream.[[state]] is not "readable", return.
    if (!stream->is_readable())
        return;

    // 3. Perform ! ResetQueue(controller).
    reset_queue(controller);

    // 4. Perform ! ReadableStreamDefaultControllerClearAlgorithms(controller).
    readable_stream_default_controller_clear_algorithms(controller);

    // 5. Perform ! ReadableStreamError(stream, e).
    readable_stream_error(*stream, error);
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-get-desired-size
Optional<float> readable_stream_default_controller_get_desired_size(ReadableStreamDefaultController& controller)
{
    auto stream = controller.stream();

    // 1. Let state be controller.[[stream]].[[state]].

    // 2. If state is "errored", return null.
    if (stream->is_errored())
        return {};

    // 3. If state is "closed", return 0.
    if (stream->is_closed())
        return 0.0f;

    // 4. Return controller.[[strategyHWM]] − controller.[[queueTotalSize]].
    return controller.strategy_hwm() - controller.queue_total_size();
}

// https://streams.spec.whatwg.org/#readable-stream-default-controller-can-close-or-enqueue
bool readable_stream_default_controller_can_close_or_enqueue(ReadableStreamDefaultController& controller)
{
    // 1. Let state be controller.[[stream]].[[state]].
    // 2. If controller.[[closeRequested]] is false and state is "readable", return true.
    // 3. Otherwise, return false.
    return !controller.close_requested() && controller.stream()->is_readable();
}

}
