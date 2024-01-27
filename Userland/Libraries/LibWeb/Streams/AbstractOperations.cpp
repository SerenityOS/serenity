/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2023-2024, Shannon Booth <shannon@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/DataViewConstructor.h>
#include <LibJS/Runtime/Intrinsics.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/QueuingStrategy.h>
#include <LibWeb/Streams/ReadableByteStreamController.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamBYOBReader.h>
#include <LibWeb/Streams/ReadableStreamBYOBRequest.h>
#include <LibWeb/Streams/ReadableStreamDefaultController.h>
#include <LibWeb/Streams/ReadableStreamDefaultReader.h>
#include <LibWeb/Streams/ReadableStreamGenericReader.h>
#include <LibWeb/Streams/TransformStream.h>
#include <LibWeb/Streams/TransformStreamDefaultController.h>
#include <LibWeb/Streams/Transformer.h>
#include <LibWeb/Streams/UnderlyingSink.h>
#include <LibWeb/Streams/UnderlyingSource.h>
#include <LibWeb/Streams/WritableStream.h>
#include <LibWeb/Streams/WritableStreamDefaultController.h>
#include <LibWeb/Streams/WritableStreamDefaultWriter.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/Buffers.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#acquire-readable-stream-reader
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamDefaultReader>> acquire_readable_stream_default_reader(ReadableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Let reader be a new ReadableStreamDefaultReader.
    auto reader = realm.heap().allocate<ReadableStreamDefaultReader>(realm, realm);

    // 2. Perform ? SetUpReadableStreamDefaultReader(reader, stream).
    TRY(set_up_readable_stream_default_reader(reader, stream));

    // 3. Return reader.
    return reader;
}

// https://streams.spec.whatwg.org/#acquire-readable-stream-byob-reader
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamBYOBReader>> acquire_readable_stream_byob_reader(ReadableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Let reader be a new ReadableStreamBYOBReader.
    auto reader = realm.heap().allocate<ReadableStreamBYOBReader>(realm, realm);

    // 2. Perform ? SetUpReadableStreamBYOBReader(reader, stream).
    TRY(set_up_readable_stream_byob_reader(reader, stream));

    // 3. Return reader.
    return reader;
}

// https://streams.spec.whatwg.org/#is-readable-stream-locked
bool is_readable_stream_locked(ReadableStream const& stream)
{
    // 1. If stream.[[reader]] is undefined, return false.
    if (!stream.reader().has_value())
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
    if (stream.state() == ReadableStream::State::Closed)
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());

    // 3. If stream.[[state]] is "errored", return a promise rejected with stream.[[storedError]].
    if (stream.state() == ReadableStream::State::Errored)
        return WebIDL::create_rejected_promise(realm, stream.stored_error());

    // 4. Perform ! ReadableStreamClose(stream).
    readable_stream_close(stream);

    // 5. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 6. If reader is not undefined and reader implements ReadableStreamBYOBReader,
    if (reader.has_value() && reader->has<JS::NonnullGCPtr<ReadableStreamBYOBReader>>()) {
        // 1. Let readIntoRequests be reader.[[readIntoRequests]].
        // 2. Set reader.[[readIntoRequests]] to an empty list.
        auto read_into_requests = move(reader->get<JS::NonnullGCPtr<ReadableStreamBYOBReader>>()->read_into_requests());

        // 3. For each readIntoRequest of readIntoRequests,
        for (auto& read_into_request : read_into_requests) {
            // 1. Perform readIntoRequest’s close steps, given undefined.
            read_into_request->on_close(JS::js_undefined());
        }
    }

    // 7. Let sourceCancelPromise be ! stream.[[controller]].[[CancelSteps]](reason).
    auto source_cancel_promise = TRY(stream.controller()->visit([&](auto const& controller) {
        return controller->cancel_steps(reason);
    }));

    // 8. Return the result of reacting to sourceCancelPromise with a fulfillment step that returns undefined.
    auto react_result = WebIDL::react_to_promise(*source_cancel_promise,
        [](auto const&) -> WebIDL::ExceptionOr<JS::Value> { return JS::js_undefined(); },
        {});

    return WebIDL::create_resolved_promise(realm, react_result);
}

// https://streams.spec.whatwg.org/#readable-stream-fulfill-read-into-request
void readable_stream_fulfill_read_into_request(ReadableStream& stream, JS::Value chunk, bool done)
{
    // 1. Assert: ! ReadableStreamHasBYOBReader(stream) is true.
    VERIFY(readable_stream_has_byob_reader(stream));

    // 2. Let reader be stream.[[reader]].
    auto reader = stream.reader()->get<JS::NonnullGCPtr<ReadableStreamBYOBReader>>();

    // 3. Assert: reader.[[readIntoRequests]] is not empty.
    VERIFY(!reader->read_into_requests().is_empty());

    // 4. Let readIntoRequest be reader.[[readIntoRequests]][0].
    // 5. Remove readIntoRequest from reader.[[readIntoRequests]].
    auto read_into_request = reader->read_into_requests().take_first();

    // 6. If done is true, perform readIntoRequest’s close steps, given chunk.
    if (done) {
        read_into_request->on_close(chunk);
    }
    // 7. Otherwise, perform readIntoRequest’s chunk steps, given chunk.
    else {
        read_into_request->on_chunk(chunk);
    }
}

// https://streams.spec.whatwg.org/#readable-stream-fulfill-read-request
void readable_stream_fulfill_read_request(ReadableStream& stream, JS::Value chunk, bool done)
{
    // 1. Assert: ! ReadableStreamHasDefaultReader(stream) is true.
    VERIFY(readable_stream_has_default_reader(stream));

    // 2. Let reader be stream.[[reader]].
    auto reader = stream.reader()->get<JS::NonnullGCPtr<ReadableStreamDefaultReader>>();

    // 3. Assert: reader.[[readRequests]] is not empty.
    VERIFY(!reader->read_requests().is_empty());

    // 4. Let readRequest be reader.[[readRequests]][0].
    // 5. Remove readRequest from reader.[[readRequests]].
    auto read_request = reader->read_requests().take_first();

    // 6. If done is true, perform readRequest’s close steps.
    if (done) {
        read_request->on_close();
    }
    // 7. Otherwise, perform readRequest’s chunk steps, given chunk.
    else {
        read_request->on_chunk(chunk);
    }
}

// https://streams.spec.whatwg.org/#readable-stream-get-num-read-into-requests
size_t readable_stream_get_num_read_into_requests(ReadableStream const& stream)
{
    // 1. Assert: ! ReadableStreamHasBYOBReader(stream) is true.
    VERIFY(readable_stream_has_byob_reader(stream));

    // 2. Return stream.[[reader]].[[readIntoRequests]]'s size.
    return stream.reader()->get<JS::NonnullGCPtr<ReadableStreamBYOBReader>>()->read_into_requests().size();
}

// https://streams.spec.whatwg.org/#readable-stream-get-num-read-requests
size_t readable_stream_get_num_read_requests(ReadableStream const& stream)
{
    // 1. Assert: ! ReadableStreamHasDefaultReader(stream) is true.
    VERIFY(readable_stream_has_default_reader(stream));

    // 2. Return stream.[[reader]].[[readRequests]]'s size.
    return stream.reader()->get<JS::NonnullGCPtr<ReadableStreamDefaultReader>>()->read_requests().size();
}

// https://streams.spec.whatwg.org/#readable-stream-has-byob-reader
bool readable_stream_has_byob_reader(ReadableStream const& stream)
{
    // 1. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 2. If reader is undefined, return false.
    if (!reader.has_value())
        return false;

    // 3. If reader implements ReadableStreamBYOBReader, return true.
    if (reader->has<JS::NonnullGCPtr<ReadableStreamBYOBReader>>())
        return true;

    // 4. Return false.
    return false;
}

// https://streams.spec.whatwg.org/#readable-stream-has-default-reader
bool readable_stream_has_default_reader(ReadableStream const& stream)
{
    // 1. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 2. If reader is undefined, return false.
    if (!reader.has_value())
        return false;

    // 3. If reader implements ReadableStreamDefaultReader, return true.
    if (reader->has<JS::NonnullGCPtr<ReadableStreamDefaultReader>>())
        return true;

    // 4. Return false.
    return false;
}

// https://streams.spec.whatwg.org/#make-size-algorithm-from-size-function
JS::NonnullGCPtr<SizeAlgorithm> extract_size_algorithm(JS::VM& vm, QueuingStrategy const& strategy)
{
    // 1. If strategy["size"] does not exist, return an algorithm that returns 1.
    if (!strategy.size)
        return JS::create_heap_function(vm.heap(), [](JS::Value) { return JS::normal_completion(JS::Value(1)); });

    // 2. Return an algorithm that performs the following steps, taking a chunk argument:
    return JS::create_heap_function(vm.heap(), [size = strategy.size](JS::Value chunk) {
        return WebIDL::invoke_callback(*size, JS::js_undefined(), chunk);
    });
}

// https://streams.spec.whatwg.org/#validate-and-normalize-high-water-mark
WebIDL::ExceptionOr<double> extract_high_water_mark(QueuingStrategy const& strategy, double default_hwm)
{
    // 1. If strategy["highWaterMark"] does not exist, return defaultHWM.
    if (!strategy.high_water_mark.has_value())
        return default_hwm;

    // 2. Let highWaterMark be strategy["highWaterMark"].
    auto high_water_mark = strategy.high_water_mark.value();

    // 3. If highWaterMark is NaN or highWaterMark < 0, throw a RangeError exception.
    if (isnan(high_water_mark) || high_water_mark < 0)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Invalid value for high water mark"sv };

    // 4. Return highWaterMark.
    return high_water_mark;
}

// https://streams.spec.whatwg.org/#readable-stream-close
void readable_stream_close(ReadableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[state]] is "readable".
    VERIFY(stream.state() == ReadableStream::State::Readable);

    // 2. Set stream.[[state]] to "closed".
    stream.set_state(ReadableStream::State::Closed);

    // 3. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 4. If reader is undefined, return.
    if (!reader.has_value())
        return;

    // 5. Resolve reader.[[closedPromise]] with undefined.
    WebIDL::resolve_promise(realm, *reader->visit([](auto& reader) {
        return reader->closed_promise_capability();
    }));

    // 6. If reader implements ReadableStreamDefaultReader,
    if (reader->has<JS::NonnullGCPtr<ReadableStreamDefaultReader>>()) {
        // 1. Let readRequests be reader.[[readRequests]].
        // 2. Set reader.[[readRequests]] to an empty list.
        auto read_requests = move(reader->get<JS::NonnullGCPtr<ReadableStreamDefaultReader>>()->read_requests());

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
    VERIFY(stream.state() == ReadableStream::State::Readable);

    // 2. Set stream.[[state]] to "errored".
    stream.set_state(ReadableStream::State::Errored);

    // 3. Set stream.[[storedError]] to e.
    stream.set_stored_error(error);

    // 4. Let reader be stream.[[reader]].
    auto reader = stream.reader();

    // 5. If reader is undefined, return.
    if (!reader.has_value())
        return;

    auto closed_promise_capability = reader->visit([](auto& reader) { return reader->closed_promise_capability(); });

    // 6. Reject reader.[[closedPromise]] with e.
    WebIDL::reject_promise(realm, *closed_promise_capability, error);

    // 7. Set reader.[[closedPromise]].[[PromiseIsHandled]] to true.
    WebIDL::mark_promise_as_handled(*closed_promise_capability);

    // 8. If reader implements ReadableStreamDefaultReader,
    if (reader->has<JS::NonnullGCPtr<ReadableStreamDefaultReader>>()) {
        // 1. Perform ! ReadableStreamDefaultReaderErrorReadRequests(reader, e).
        readable_stream_default_reader_error_read_requests(*reader->get<JS::NonnullGCPtr<ReadableStreamDefaultReader>>(), error);
    }
    // 9. Otherwise,
    else {
        // 1. Assert: reader implements ReadableStreamBYOBReader.
        VERIFY(reader->has<JS::NonnullGCPtr<ReadableStreamBYOBReader>>());

        // 2. Perform ! ReadableStreamBYOBReaderErrorReadIntoRequests(reader, e).
        readable_stream_byob_reader_error_read_into_requests(*reader->get<JS::NonnullGCPtr<ReadableStreamBYOBReader>>(), error);
    }
}

// https://streams.spec.whatwg.org/#readable-stream-add-read-request
void readable_stream_add_read_request(ReadableStream& stream, JS::NonnullGCPtr<ReadRequest> read_request)
{
    // 1. Assert: stream.[[reader]] implements ReadableStreamDefaultReader.
    VERIFY(stream.reader().has_value() && stream.reader()->has<JS::NonnullGCPtr<ReadableStreamDefaultReader>>());

    // 2. Assert: stream.[[state]] is "readable".
    VERIFY(stream.state() == ReadableStream::State::Readable);

    // 3. Append readRequest to stream.[[reader]].[[readRequests]].
    stream.reader()->get<JS::NonnullGCPtr<ReadableStreamDefaultReader>>()->read_requests().append(read_request);
}

// https://streams.spec.whatwg.org/#readable-stream-add-read-into-request
void readable_stream_add_read_into_request(ReadableStream& stream, JS::NonnullGCPtr<ReadIntoRequest> read_into_request)
{
    // 1. Assert: stream.[[reader]] implements ReadableStreamBYOBReader.
    VERIFY(stream.reader().has_value() && stream.reader()->has<JS::NonnullGCPtr<ReadableStreamBYOBReader>>());

    // 2. Assert: stream.[[state]] is "readable" or "closed".
    VERIFY(stream.is_readable() || stream.is_closed());

    // 3. Append readRequest to stream.[[reader]].[[readIntoRequests]].
    stream.reader()->get<JS::NonnullGCPtr<ReadableStreamBYOBReader>>()->read_into_requests().append(read_into_request);
}

// https://streams.spec.whatwg.org/#readable-stream-reader-generic-cancel
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> readable_stream_reader_generic_cancel(ReadableStreamGenericReaderMixin& reader, JS::Value reason)
{
    // 1. Let stream be reader.[[stream]]
    auto stream = reader.stream();

    // 2. Assert: stream is not undefined
    VERIFY(stream);

    // 3. Return ! ReadableStreamCancel(stream, reason)
    return TRY(readable_stream_cancel(*stream, reason));
}

// https://streams.spec.whatwg.org/#readable-stream-reader-generic-initialize
void readable_stream_reader_generic_initialize(ReadableStreamReader reader, ReadableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Set reader.[[stream]] to stream.
    reader.visit([&](auto& reader) { reader->set_stream(stream); });

    // 2. Set stream.[[reader]] to reader.
    stream.set_reader(reader);

    // 3. If stream.[[state]] is "readable",
    if (stream.state() == ReadableStream::State::Readable) {
        // 1. Set reader.[[closedPromise]] to a new promise.
        reader.visit([&](auto& reader) { reader->set_closed_promise_capability(WebIDL::create_promise(realm)); });
    }
    // 4. Otherwise, if stream.[[state]] is "closed",
    else if (stream.state() == ReadableStream::State::Closed) {
        // 1. Set reader.[[closedPromise]] to a promise resolved with undefined.
        reader.visit([&](auto& reader) {
            reader->set_closed_promise_capability(WebIDL::create_resolved_promise(realm, JS::js_undefined()));
        });
    }
    // 5. Otherwise,
    else {
        // 1. Assert: stream.[[state]] is "errored".
        VERIFY(stream.state() == ReadableStream::State::Errored);

        // 2. Set reader.[[closedPromise]] to a promise rejected with stream.[[storedError]].
        // 3. Set reader.[[closedPromise]].[[PromiseIsHandled]] to true.
        reader.visit([&](auto& reader) {
            reader->set_closed_promise_capability(WebIDL::create_rejected_promise(realm, stream.stored_error()));
            WebIDL::mark_promise_as_handled(*reader->closed_promise_capability());
        });
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
    VERIFY(stream->reader()->visit([](auto& reader) -> ReadableStreamGenericReaderMixin* { return reader.ptr(); }) == &reader);

    auto& realm = stream->realm();

    // 4. If stream.[[state]] is "readable", reject reader.[[closedPromise]] with a TypeError exception.
    auto exception = JS::TypeError::create(realm, "Released readable stream"sv);
    if (stream->is_readable()) {
        WebIDL::reject_promise(realm, *reader.closed_promise_capability(), exception);
    }
    // 5. Otherwise, set reader.[[closedPromise]] to a promise rejected with a TypeError exception.
    else {
        reader.set_closed_promise_capability(WebIDL::create_rejected_promise(realm, exception));
    }

    // 6. Set reader.[[closedPromise]].[[PromiseIsHandled]] to true.
    WebIDL::mark_promise_as_handled(*reader.closed_promise_capability());

    // 7. Perform ! stream.[[controller]].[[ReleaseSteps]]().
    TRY(stream->controller()->visit([](auto const& controller) { return controller->release_steps(); }));

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
    auto read_requests = move(reader.read_requests());

    // 2. Set reader.[[readRequests]] to a new empty list.
    reader.read_requests().clear();

    // 3. For each readRequest of readRequests,
    for (auto& read_request : read_requests) {
        // 1. Perform readRequest’s error steps, given e.
        read_request->on_error(error);
    }
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablestreambyobreadererrorreadintorequests
void readable_stream_byob_reader_error_read_into_requests(ReadableStreamBYOBReader& reader, JS::Value error)
{
    // 1. Let readIntoRequests be reader.[[readIntoRequests]].
    auto read_into_requests = move(reader.read_into_requests());

    // 2. Set reader.[[readIntoRequests]] to a new empty list.
    reader.read_into_requests().clear();

    // 3. For each readIntoRequest of readIntoRequests,
    for (auto& read_into_request : read_into_requests) {

        // 1. Perform readIntoRequest’s error steps, given e.
        read_into_request->on_error(error);
    }
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-fill-head-pull-into-descriptor
void readable_byte_stream_controller_fill_head_pull_into_descriptor(ReadableByteStreamController const& controller, u64 size, PullIntoDescriptor& pull_into_descriptor)
{
    // 1. Assert: either controller.[[pendingPullIntos]] is empty, or controller.[[pendingPullIntos]][0] is pullIntoDescriptor.
    VERIFY(controller.pending_pull_intos().is_empty() || &controller.pending_pull_intos().first() == &pull_into_descriptor);

    // 2. Assert: controller.[[byobRequest]] is null.
    VERIFY(!controller.raw_byob_request());

    // 3. Set pullIntoDescriptor’s bytes filled to bytes filled + size.
    pull_into_descriptor.bytes_filled += size;
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-fill-pull-into-descriptor-from-queue
bool readable_byte_stream_controller_fill_pull_into_descriptor_from_queue(ReadableByteStreamController& controller, PullIntoDescriptor& pull_into_descriptor)
{
    // 1. Let elementSize be pullIntoDescriptor.[[elementSize]].
    auto element_size = pull_into_descriptor.element_size;

    // 2. Let currentAlignedBytes be pullIntoDescriptor’s bytes filled − (pullIntoDescriptor’s bytes filled mod elementSize).
    auto current_aligned_bytes = pull_into_descriptor.bytes_filled - (pull_into_descriptor.bytes_filled % pull_into_descriptor.element_size);

    // 3. Let maxBytesToCopy be min(controller.[[queueTotalSize]], pullIntoDescriptor’s byte length − pullIntoDescriptor’s bytes filled).
    auto max_bytes_to_copy = min(controller.queue_total_size(), pull_into_descriptor.byte_length - pull_into_descriptor.bytes_filled);

    // 4. Let maxBytesFilled be pullIntoDescriptor’s bytes filled + maxBytesToCopy.
    u64 max_bytes_filled = pull_into_descriptor.bytes_filled + max_bytes_to_copy;

    // 5. Let maxAlignedBytes be maxBytesFilled − (maxBytesFilled mod elementSize).
    auto max_aligned_bytes = max_bytes_filled - (max_bytes_filled % element_size);

    // 6. Let totalBytesToCopyRemaining be maxBytesToCopy.
    auto total_bytes_to_copy_remaining = max_bytes_to_copy;

    // 7. Let ready be false.
    bool ready = false;

    // 8. If maxAlignedBytes > currentAlignedBytes,
    if (max_aligned_bytes > current_aligned_bytes) {
        // 1. Set totalBytesToCopyRemaining to maxAlignedBytes − pullIntoDescriptor’s bytes filled.
        total_bytes_to_copy_remaining = max_aligned_bytes - pull_into_descriptor.bytes_filled;

        // 2. Set ready to true.
        ready = true;
    }

    // 9. Let queue be controller.[[queue]].
    auto& queue = controller.queue();

    // 10. While totalBytesToCopyRemaining > 0,
    while (total_bytes_to_copy_remaining > 0) {
        // 1. Let headOfQueue be queue[0].
        auto& head_of_queue = queue.first();

        // 2. Let bytesToCopy be min(totalBytesToCopyRemaining, headOfQueue’s byte length).
        auto bytes_to_copy = min(total_bytes_to_copy_remaining, head_of_queue.byte_length);

        // 3. Let destStart be pullIntoDescriptor’s byte offset + pullIntoDescriptor’s bytes filled.
        auto dest_start = pull_into_descriptor.byte_offset + pull_into_descriptor.bytes_filled;

        // 4. Perform ! CopyDataBlockBytes(pullIntoDescriptor’s buffer.[[ArrayBufferData]], destStart, headOfQueue’s buffer.[[ArrayBufferData]], headOfQueue’s byte offset, bytesToCopy).
        JS::copy_data_block_bytes(pull_into_descriptor.buffer->buffer(), dest_start, head_of_queue.buffer->buffer(), head_of_queue.byte_offset, bytes_to_copy);

        // 5. If headOfQueue’s byte length is bytesToCopy,
        if (head_of_queue.byte_length == bytes_to_copy) {
            // 1. Remove queue[0].
            queue.take_first();
        }
        // 6. Otherwise,
        else {
            // 1. Set headOfQueue’s byte offset to headOfQueue’s byte offset + bytesToCopy.
            head_of_queue.byte_offset += bytes_to_copy;

            // 2. Set headOfQueue’s byte length to headOfQueue’s byte length − bytesToCopy.
            head_of_queue.byte_length -= bytes_to_copy;
        }

        // 7. Set controller.[[queueTotalSize]] to controller.[[queueTotalSize]] − bytesToCopy.
        controller.set_queue_total_size(controller.queue_total_size() - bytes_to_copy);

        // 8, Perform ! ReadableByteStreamControllerFillHeadPullIntoDescriptor(controller, bytesToCopy, pullIntoDescriptor).
        readable_byte_stream_controller_fill_head_pull_into_descriptor(controller, bytes_to_copy, pull_into_descriptor);

        // 9. Set totalBytesToCopyRemaining to totalBytesToCopyRemaining − bytesToCopy.
        total_bytes_to_copy_remaining -= bytes_to_copy;
    }

    // 11. If ready is false,
    if (!ready) {
        // 1. Assert: controller.[[queueTotalSize]] is 0.
        VERIFY(controller.queue_total_size() == 0);

        // 2. Assert: pullIntoDescriptor’s bytes filled > 0.
        VERIFY(pull_into_descriptor.bytes_filled > 0);

        // 3. Assert: pullIntoDescriptor’s bytes filled < pullIntoDescriptor’s element size.
        VERIFY(pull_into_descriptor.bytes_filled < pull_into_descriptor.element_size);
    }

    // 12. Return ready.
    return ready;
}

// https://streams.spec.whatwg.org/#readable-stream-default-reader-read
WebIDL::ExceptionOr<void> readable_stream_default_reader_read(ReadableStreamDefaultReader& reader, ReadRequest& read_request)
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
        TRY(stream->controller()->visit([&](auto const& controller) {
            return controller->pull_steps(read_request);
        }));
    }

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-convert-pull-into-descriptor
JS::Value readable_byte_stream_controller_convert_pull_into_descriptor(JS::Realm& realm, PullIntoDescriptor const& pull_into_descriptor)
{
    auto& vm = realm.vm();

    // 1. Let bytesFilled be pullIntoDescriptor’s bytes filled.
    auto bytes_filled = pull_into_descriptor.bytes_filled;

    // 2. Let elementSize be pullIntoDescriptor’s element size.
    auto element_size = pull_into_descriptor.element_size;

    // 3. Assert: bytesFilled ≤ pullIntoDescriptor’s byte length.
    VERIFY(bytes_filled <= pull_into_descriptor.byte_length);

    // 4. Assert: bytesFilled mod elementSize is 0.
    VERIFY(bytes_filled % element_size == 0);

    // 5. Let buffer be ! TransferArrayBuffer(pullIntoDescriptor’s buffer).
    auto buffer = MUST(transfer_array_buffer(realm, pull_into_descriptor.buffer));

    // 6. Return ! Construct(pullIntoDescriptor’s view constructor, « buffer, pullIntoDescriptor’s byte offset, bytesFilled ÷ elementSize »).
    return MUST(JS::construct(vm, *pull_into_descriptor.view_constructor, buffer, JS::Value(pull_into_descriptor.byte_offset), JS::Value(bytes_filled / element_size)));
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-pull-into
void readable_byte_stream_controller_pull_into(ReadableByteStreamController& controller, WebIDL::ArrayBufferView& view, ReadIntoRequest& read_into_request)
{
    auto& vm = controller.vm();
    auto& realm = controller.realm();

    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. Let elementSize be 1.
    size_t element_size = 1;

    // 3. Let ctor be %DataView%.
    JS::NativeFunction* ctor = realm.intrinsics().data_view_constructor();

    // 4. If view has a [[TypedArrayName]] internal slot (i.e., it is not a DataView),
    if (view.bufferable_object().has<JS::NonnullGCPtr<JS::TypedArrayBase>>()) {
        auto const& typed_array = *view.bufferable_object().get<JS::NonnullGCPtr<JS::TypedArrayBase>>();

        // 1. Set elementSize to the element size specified in the typed array constructors table for view.[[TypedArrayName]].
        element_size = typed_array.element_size();

        // 2. Set ctor to the constructor specified in the typed array constructors table for view.[[TypedArrayName]].
        switch (typed_array.kind()) {
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, Type) \
    case JS::TypedArrayBase::Kind::ClassName:                                       \
        ctor = realm.intrinsics().snake_name##_constructor();                       \
        break;
            JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE
        }
    }

    // 5. Let byteOffset be view.[[ByteOffset]].
    auto byte_offset = view.byte_offset();

    // 6. Let byteLength be view.[[ByteLength]].
    auto byte_length = view.byte_length();

    // 7. Let bufferResult be TransferArrayBuffer(view.[[ViewedArrayBuffer]]).
    auto buffer_result = transfer_array_buffer(realm, *view.viewed_array_buffer());

    // 8. If bufferResult is an abrupt completion,
    if (buffer_result.is_exception()) {
        // 1. Perform readIntoRequest’s error steps, given bufferResult.[[Value]].
        auto throw_completion = Bindings::dom_exception_to_throw_completion(vm, buffer_result.exception());
        read_into_request.on_error(*throw_completion.release_value());

        // 2. Return.
        return;
    }

    // 9. Let buffer be bufferResult.[[Value]].
    auto buffer = buffer_result.value();

    // 10. Let pullIntoDescriptor be a new pull-into descriptor with buffer buffer, buffer byte length buffer.[[ArrayBufferByteLength]],
    //     byte offset byteOffset, byte length byteLength, bytes filled 0, element size elementSize, view constructor ctor, and reader type "byob".
    PullIntoDescriptor pull_into_descriptor {
        .buffer = buffer,
        .buffer_byte_length = buffer->byte_length(),
        .byte_offset = byte_offset,
        .byte_length = byte_length,
        .bytes_filled = 0,
        .element_size = element_size,
        .view_constructor = *ctor,
        .reader_type = ReaderType::Byob,
    };

    // 11. If controller.[[pendingPullIntos]] is not empty,
    if (!controller.pending_pull_intos().is_empty()) {
        // 1. Append pullIntoDescriptor to controller.[[pendingPullIntos]].
        controller.pending_pull_intos().append(pull_into_descriptor);

        // 2. Perform ! ReadableStreamAddReadIntoRequest(stream, readIntoRequest).
        readable_stream_add_read_into_request(*stream, read_into_request);

        // 3. Return.
        return;
    }

    // 12. If stream.[[state]] is "closed",
    if (stream->is_closed()) {
        // 1. Let emptyView be ! Construct(ctor, « pullIntoDescriptor’s buffer, pullIntoDescriptor’s byte offset, 0 »).
        auto empty_view = MUST(JS::construct(vm, *ctor, pull_into_descriptor.buffer, JS::Value(pull_into_descriptor.byte_offset), JS::Value(0)));

        // 2. Perform readIntoRequest’s close steps, given emptyView.
        read_into_request.on_close(empty_view);

        // 3. Return.
        return;
    }

    // 13. If controller.[[queueTotalSize]] > 0,
    if (controller.queue_total_size() > 0) {
        // 1. If ! ReadableByteStreamControllerFillPullIntoDescriptorFromQueue(controller, pullIntoDescriptor) is true,
        if (readable_byte_stream_controller_fill_pull_into_descriptor_from_queue(controller, pull_into_descriptor)) {
            // 1. Let filledView be ! ReadableByteStreamControllerConvertPullIntoDescriptor(pullIntoDescriptor).
            auto filled_view = readable_byte_stream_controller_convert_pull_into_descriptor(realm, pull_into_descriptor);

            // 2. Perform ! ReadableByteStreamControllerHandleQueueDrain(controller).
            MUST(readable_byte_stream_controller_handle_queue_drain(controller));

            // 3. Perform readIntoRequest’s chunk steps, given filledView.
            read_into_request.on_chunk(filled_view);

            // 4. Return.
            return;
        }

        // 2. If controller.[[closeRequested]] is true,
        if (controller.close_requested()) {
            // 1. Let e be a TypeError exception.
            auto error = JS::TypeError::create(realm, "Reader has been released"sv);

            // 2. Perform ! ReadableByteStreamControllerError(controller, e).
            readable_byte_stream_controller_error(controller, error);

            // 3. Perform readIntoRequest’s error steps, given e.
            read_into_request.on_error(error);

            // 4. Return.
            return;
        }
    }

    // 14. Append pullIntoDescriptor to controller.[[pendingPullIntos]].
    controller.pending_pull_intos().append(pull_into_descriptor);

    // 15. Perform ! ReadableStreamAddReadIntoRequest(stream, readIntoRequest).
    readable_stream_add_read_into_request(*stream, read_into_request);

    // 16. Perform ! ReadableByteStreamControllerCallPullIfNeeded(controller).
    MUST(readable_byte_stream_controller_call_pull_if_needed(controller));
}

// https://streams.spec.whatwg.org/#readable-stream-byob-reader-read
void readable_stream_byob_reader_read(ReadableStreamBYOBReader& reader, WebIDL::ArrayBufferView& view, ReadIntoRequest& read_into_request)
{
    // 1. Let stream be reader.[[stream]].
    auto stream = reader.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Set stream.[[disturbed]] to true.
    stream->set_disturbed(true);

    // 4. If stream.[[state]] is "errored", perform readIntoRequest’s error steps given stream.[[storedError]].
    if (stream->is_errored()) {
        read_into_request.on_error(stream->stored_error());
    }
    // 5. Otherwise, perform ! ReadableByteStreamControllerPullInto(stream.[[controller]], view, readIntoRequest).
    else {
        readable_byte_stream_controller_pull_into(*stream->controller()->get<JS::NonnullGCPtr<ReadableByteStreamController>>(), view, read_into_request);
    }
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablestreamdefaultreaderrelease
WebIDL::ExceptionOr<void> readable_stream_default_reader_release(ReadableStreamDefaultReader& reader)
{
    auto& realm = reader.realm();

    // 1. Perform ! ReadableStreamReaderGenericRelease(reader).
    TRY(readable_stream_reader_generic_release(reader));

    // 2. Let e be a new TypeError exception.
    auto exception = JS::TypeError::create(realm, "Reader has been released"sv);

    // 3. Perform ! ReadableStreamDefaultReaderErrorReadRequests(reader, e).
    readable_stream_default_reader_error_read_requests(reader, exception);

    return {};
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablestreambyobreaderrelease
void readable_stream_byob_reader_release(ReadableStreamBYOBReader& reader)
{
    auto& realm = reader.realm();

    // 1. Perform ! ReadableStreamReaderGenericRelease(reader).
    MUST(readable_stream_reader_generic_release(reader));

    // 2. Let e be a new TypeError exception.
    auto exception = JS::TypeError::create(realm, "Reader has been released"sv);

    // 3. Perform ! ReadableStreamBYOBReaderErrorReadIntoRequests(reader, e).
    readable_stream_byob_reader_error_read_into_requests(reader, exception);
}

// https://streams.spec.whatwg.org/#set-up-readable-stream-default-reader
WebIDL::ExceptionOr<void> set_up_readable_stream_default_reader(ReadableStreamDefaultReader& reader, ReadableStream& stream)
{
    // 1. If ! IsReadableStreamLocked(stream) is true, throw a TypeError exception.
    if (is_readable_stream_locked(stream))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot create stream reader for a locked stream"sv };

    // 2. Perform ! ReadableStreamReaderGenericInitialize(reader, stream).
    // 3. Set reader.[[readRequests]] to a new empty list.
    readable_stream_reader_generic_initialize(ReadableStreamReader { reader }, stream);

    return {};
}

// https://streams.spec.whatwg.org/#set-up-readable-stream-byob-reader
WebIDL::ExceptionOr<void> set_up_readable_stream_byob_reader(ReadableStreamBYOBReader& reader, ReadableStream& stream)
{
    // 1. If ! IsReadableStreamLocked(stream) is true, throw a TypeError exception.
    if (is_readable_stream_locked(stream))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot create stream reader for a locked stream"sv };

    // 2. If stream.[[controller]] does not implement ReadableByteStreamController, throw a TypeError exception.
    if (!stream.controller()->has<JS::NonnullGCPtr<ReadableByteStreamController>>())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "BYOB reader cannot set up reader from non-byte stream"sv };

    // 3. Perform ! ReadableStreamReaderGenericInitialize(reader, stream).
    readable_stream_reader_generic_initialize(ReadableStreamReader { reader }, stream);

    // 4. Set reader.[[readIntoRequests]] to a new empty list.
    reader.read_into_requests().clear();

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

// https://streams.spec.whatwg.org/#rs-default-controller-has-backpressure
bool readable_stream_default_controller_has_backpressure(ReadableStreamDefaultController& controller)
{
    // 1. If ! ReadableStreamDefaultControllerShouldCallPull(controller) is true, return false.
    if (readable_stream_default_controller_should_call_pull(controller))
        return false;

    // 2. Otherwise, return true.
    return true;
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
        auto result = controller.strategy_size_algorithm()->function()(chunk);

        // 2. If result is an abrupt completion,
        if (result.is_abrupt()) {
            // 1. Perform ! ReadableStreamDefaultControllerError(controller, result.[[Value]]).
            readable_stream_default_controller_error(controller, result.value().value());

            // 2. Return result.
            return result;
        }

        // 3. Let chunkSize be result.[[Value]].
        auto chunk_size = result.release_value().release_value();

        // 4. Let enqueueResult be EnqueueValueWithSize(controller, chunk, chunkSize).
        auto enqueue_result = enqueue_value_with_size(controller, chunk, chunk_size);

        // 5. If enqueueResult is an abrupt completion,
        if (enqueue_result.is_error()) {
            auto throw_completion = Bindings::throw_dom_exception_if_needed(vm, [&] { return enqueue_result; }).throw_completion();

            // 1. Perform ! ReadableStreamDefaultControllerError(controller, enqueueResult.[[Value]]).
            readable_stream_default_controller_error(controller, throw_completion.value().value());

            // 2. Return enqueueResult.
            // Note: We need to return the throw_completion object here, as enqueue needs to throw the same object that the controller is errored with
            return throw_completion;
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
    auto pull_promise = TRY(controller.pull_algorithm()->function()());

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
    if (desired_size.release_value() > 0.0)
        return true;

    // 8. Return false.
    return false;
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablebytestreamcontrollergetbyobrequest
JS::GCPtr<ReadableStreamBYOBRequest> readable_byte_stream_controller_get_byob_request(JS::NonnullGCPtr<ReadableByteStreamController> controller)
{
    auto& vm = controller->vm();
    auto& realm = controller->realm();

    // 1. If controller.[[byobRequest]] is null and controller.[[pendingPullIntos]] is not empty,
    if (!controller->raw_byob_request() && !controller->pending_pull_intos().is_empty()) {
        // 1. Let firstDescriptor be controller.[[pendingPullIntos]][0].
        auto const& first_descriptor = controller->pending_pull_intos().first();

        // 2. Let view be ! Construct(%Uint8Array%, « firstDescriptor’s buffer, firstDescriptor’s byte offset + firstDescriptor’s bytes filled, firstDescriptor’s byte length − firstDescriptor’s bytes filled »).
        auto view = MUST(JS::construct(vm, *realm.intrinsics().uint8_array_constructor(), first_descriptor.buffer, JS::Value(first_descriptor.byte_offset + first_descriptor.bytes_filled), JS::Value(first_descriptor.byte_length - first_descriptor.bytes_filled)));

        // 3. Let byobRequest be a new ReadableStreamBYOBRequest.
        auto byob_request = realm.heap().allocate<ReadableStreamBYOBRequest>(realm, realm);

        // 4. Set byobRequest.[[controller]] to controller.
        byob_request->set_controller(controller);

        // 5. Set byobRequest.[[view]] to view.
        auto array_buffer_view = vm.heap().allocate<WebIDL::ArrayBufferView>(realm, view);
        byob_request->set_view(array_buffer_view);

        // 6. Set controller.[[byobRequest]] to byobRequest.
        controller->set_byob_request(byob_request);
    }

    // 2. Return controller.[[byobRequest]].
    return controller->raw_byob_request();
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

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-respond-in-readable-state
WebIDL::ExceptionOr<void> readable_byte_stream_controller_respond_in_readable_state(ReadableByteStreamController& controller, u64 bytes_written, PullIntoDescriptor& pull_into_descriptor)
{
    // 1. Assert: pullIntoDescriptor’s bytes filled + bytesWritten ≤ pullIntoDescriptor’s byte length.
    VERIFY(pull_into_descriptor.bytes_filled + bytes_written <= pull_into_descriptor.byte_length);

    // 2. Perform ! ReadableByteStreamControllerFillHeadPullIntoDescriptor(controller, bytesWritten, pullIntoDescriptor).
    readable_byte_stream_controller_fill_head_pull_into_descriptor(controller, bytes_written, pull_into_descriptor);

    // 3. If pullIntoDescriptor’s reader type is "none",
    if (pull_into_descriptor.reader_type == ReaderType::None) {
        // 1. Perform ? ReadableByteStreamControllerEnqueueDetachedPullIntoToQueue(controller, pullIntoDescriptor).
        TRY(readable_byte_stream_controller_enqueue_detached_pull_into_queue(controller, pull_into_descriptor));

        // 2. Perform ! ReadableByteStreamControllerProcessPullIntoDescriptorsUsingQueue(controller).
        readable_byte_stream_controller_process_pull_into_descriptors_using_queue(controller);

        // 3. Return.
        return {};
    }

    // 4. If pullIntoDescriptor’s bytes filled < pullIntoDescriptor’s minimum fill, return.
    // FIXME: Support minimum fill.
    if (pull_into_descriptor.bytes_filled < pull_into_descriptor.element_size)
        return {};

    // NOTE: A descriptor for a read() request that is not yet filled up to its minimum length will stay at the head of the queue, so the underlying source can keep filling it.

    // 5. Perform ! ReadableByteStreamControllerShiftPendingPullInto(controller).
    // NOTE: We need to take a copy of pull_into_descriptor here as the shift destroys the pull into descriptor we are given.
    auto pull_into_descriptor_copy = readable_byte_stream_controller_shift_pending_pull_into(controller);

    // 6. Let remainderSize be the remainder after dividing pullIntoDescriptor’s bytes filled by pullIntoDescriptor’s element size.
    auto remainder_size = pull_into_descriptor_copy.bytes_filled % pull_into_descriptor_copy.element_size;

    // 7. If remainderSize > 0,
    if (remainder_size > 0) {
        // 1. Let end be pullIntoDescriptor’s byte offset + pullIntoDescriptor’s bytes filled.
        auto end = pull_into_descriptor_copy.byte_offset + pull_into_descriptor_copy.bytes_filled;

        // 2. Perform ? ReadableByteStreamControllerEnqueueClonedChunkToQueue(controller, pullIntoDescriptor’s buffer, end − remainderSize, remainderSize).
        TRY(readable_byte_stream_controller_enqueue_cloned_chunk_to_queue(controller, *pull_into_descriptor_copy.buffer, end - remainder_size, remainder_size));
    }

    // 8. Set pullIntoDescriptor’s bytes filled to pullIntoDescriptor’s bytes filled − remainderSize.
    pull_into_descriptor_copy.bytes_filled -= remainder_size;

    // 9. Perform ! ReadableByteStreamControllerCommitPullIntoDescriptor(controller.[[stream]], pullIntoDescriptor).
    readable_byte_stream_controller_commit_pull_into_descriptor(*controller.stream(), pull_into_descriptor_copy);
    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-respond-in-closed-state
void readable_byte_stream_controller_respond_in_closed_state(ReadableByteStreamController& controller, PullIntoDescriptor& first_descriptor)
{
    // 1. Assert: the remainder after dividing firstDescriptor’s bytes filled by firstDescriptor’s element size is 0.
    VERIFY(first_descriptor.bytes_filled % first_descriptor.element_size == 0);

    // 2. If firstDescriptor’s reader type is "none", perform ! ReadableByteStreamControllerShiftPendingPullInto(controller).
    if (first_descriptor.reader_type == ReaderType::None)
        readable_byte_stream_controller_shift_pending_pull_into(controller);

    // 3. Let stream be controller.[[stream]].
    auto& stream = *controller.stream();

    // 4. If ! ReadableStreamHasBYOBReader(stream) is true,
    if (readable_stream_has_default_reader(stream)) {
        // 1. While ! ReadableStreamGetNumReadIntoRequests(stream) > 0,
        while (readable_stream_get_num_read_requests(stream) > 0) {
            // 1. Let pullIntoDescriptor be ! ReadableByteStreamControllerShiftPendingPullInto(controller).
            auto pull_into_descriptor = readable_byte_stream_controller_shift_pending_pull_into(controller);

            // 2. Perform ! ReadableByteStreamControllerCommitPullIntoDescriptor(stream, pullIntoDescriptor).
            readable_byte_stream_controller_commit_pull_into_descriptor(stream, pull_into_descriptor);
        }
    }
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-respond-internal
WebIDL::ExceptionOr<void> readable_byte_stream_controller_respond_internal(ReadableByteStreamController& controller, u64 bytes_written)
{
    // 1. Let firstDescriptor be controller.[[pendingPullIntos]][0].
    auto& first_descriptor = controller.pending_pull_intos().first();

    // 2. Assert: ! CanTransferArrayBuffer(firstDescriptor’s buffer) is true.
    VERIFY(can_transfer_array_buffer(*first_descriptor.buffer));

    // 3. Perform ! ReadableByteStreamControllerInvalidateBYOBRequest(controller).
    readable_byte_stream_controller_invalidate_byob_request(controller);

    // 4. Let state be controller.[[stream]].[[state]].
    auto state = controller.stream()->state();

    // 5. If state is "closed",
    if (state == ReadableStream::State::Closed) {
        // 1. Assert: bytesWritten is 0.
        VERIFY(bytes_written == 0);

        // 2. Perform ! ReadableByteStreamControllerRespondInClosedState(controller, firstDescriptor).
        readable_byte_stream_controller_respond_in_closed_state(controller, first_descriptor);
    }

    // 6. Otherwise,
    else {
        // 1. Assert: state is "readable".
        VERIFY(state == ReadableStream::State::Readable);

        // 2. Assert: bytesWritten > 0.
        VERIFY(bytes_written > 0);

        // 3. Perform ? ReadableByteStreamControllerRespondInReadableState(controller, bytesWritten, firstDescriptor).
        TRY(readable_byte_stream_controller_respond_in_readable_state(controller, bytes_written, first_descriptor));
    }

    // 7. Perform ! ReadableByteStreamControllerCallPullIfNeeded(controller).
    MUST(readable_byte_stream_controller_call_pull_if_needed(controller));
    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-respond
WebIDL::ExceptionOr<void> readable_byte_stream_controller_respond(ReadableByteStreamController& controller, u64 bytes_written)
{
    auto& realm = controller.realm();

    // 1. Assert: controller.[[pendingPullIntos]] is not empty.
    VERIFY(!controller.pending_pull_intos().is_empty());

    // 2. Let firstDescriptor be controller.[[pendingPullIntos]][0].
    auto& first_descriptor = controller.pending_pull_intos().first();

    // 3. Let state be controller.[[stream]].[[state]].
    auto state = controller.stream()->state();

    // 4. If state is "closed",
    if (state == ReadableStream::State::Closed) {
        // 1. If bytesWritten is not 0, throw a TypeError exception.
        if (bytes_written != 0)
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Bytes written is not zero for closed stream"sv };
    }
    // 5. Otherwise,
    else {
        // 1. Assert: state is "readable".
        VERIFY(state == ReadableStream::State::Readable);

        // 2. If bytesWritten is 0, throw a TypeError exception.
        if (bytes_written == 0)
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Bytes written is zero for stream which is not closed"sv };

        // 3. If firstDescriptor’s bytes filled + bytesWritten > firstDescriptor’s byte length, throw a RangeError exception.
        if (first_descriptor.bytes_filled + bytes_written > first_descriptor.byte_length)
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Bytes written is greater than the pull requests byte length"sv };
    }

    // 6. Set firstDescriptor’s buffer to ! TransferArrayBuffer(firstDescriptor’s buffer).
    first_descriptor.buffer = MUST(transfer_array_buffer(realm, *first_descriptor.buffer));

    // 7. Perform ? ReadableByteStreamControllerRespondInternal(controller, bytesWritten).
    return readable_byte_stream_controller_respond_internal(controller, bytes_written);
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
Optional<double> readable_stream_default_controller_get_desired_size(ReadableStreamDefaultController& controller)
{
    auto stream = controller.stream();

    // 1. Let state be controller.[[stream]].[[state]].

    // 2. If state is "errored", return null.
    if (stream->is_errored())
        return {};

    // 3. If state is "closed", return 0.
    if (stream->is_closed())
        return 0.0;

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

// https://streams.spec.whatwg.org/#set-up-readable-stream-default-controller
WebIDL::ExceptionOr<void> set_up_readable_stream_default_controller(ReadableStream& stream, ReadableStreamDefaultController& controller, JS::NonnullGCPtr<StartAlgorithm> start_algorithm, JS::NonnullGCPtr<PullAlgorithm> pull_algorithm, JS::NonnullGCPtr<CancelAlgorithm> cancel_algorithm, double high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> size_algorithm)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[controller]] is undefined.
    VERIFY(!stream.controller().has_value());

    // 2. Set controller.[[stream]] to stream.
    controller.set_stream(stream);

    // 3. Perform ! ResetQueue(controller).
    reset_queue(controller);

    // 4. Set controller.[[started]], controller.[[closeRequested]], controller.[[pullAgain]], and controller.[[pulling]] to false.
    controller.set_started(false);
    controller.set_close_requested(false);
    controller.set_pull_again(false);
    controller.set_pulling(false);

    // 5. Set controller.[[strategySizeAlgorithm]] to sizeAlgorithm and controller.[[strategyHWM]] to highWaterMark.
    controller.set_strategy_size_algorithm(size_algorithm);
    controller.set_strategy_hwm(high_water_mark);

    // 6. Set controller.[[pullAlgorithm]] to pullAlgorithm.
    controller.set_pull_algorithm(pull_algorithm);

    // 7. Set controller.[[cancelAlgorithm]] to cancelAlgorithm.
    controller.set_cancel_algorithm(cancel_algorithm);

    // 8. Set stream.[[controller]] to controller.
    stream.set_controller(ReadableStreamController { controller });

    // 9. Let startResult be the result of performing startAlgorithm. (This might throw an exception.)
    auto start_result = TRY(start_algorithm->function()());

    // 10. Let startPromise be a promise resolved with startResult.
    auto start_promise = WebIDL::create_resolved_promise(realm, start_result);

    // 11. Upon fulfillment of startPromise,
    WebIDL::upon_fulfillment(start_promise, [&](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Set controller.[[started]] to true.
        controller.set_started(true);

        // 2. Assert: controller.[[pulling]] is false.
        VERIFY(!controller.pulling());

        // 3. Assert: controller.[[pullAgain]] is false.
        VERIFY(!controller.pull_again());

        // 4. Perform ! ReadableStreamDefaultControllerCallPullIfNeeded(controller).
        TRY(readable_stream_default_controller_can_pull_if_needed(controller));

        return JS::js_undefined();
    });

    // 12. Upon rejection of startPromise with reason r,
    WebIDL::upon_rejection(start_promise, [&](auto const& r) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! ReadableStreamDefaultControllerError(controller, r).
        readable_stream_default_controller_error(controller, r);

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#set-up-readable-stream-default-controller-from-underlying-source
WebIDL::ExceptionOr<void> set_up_readable_stream_default_controller_from_underlying_source(ReadableStream& stream, JS::Value underlying_source_value, UnderlyingSource underlying_source, double high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> size_algorithm)
{
    auto& realm = stream.realm();

    // 1. Let controller be a new ReadableStreamDefaultController.
    auto controller = stream.heap().allocate<ReadableStreamDefaultController>(realm, realm);

    // 2. Let startAlgorithm be an algorithm that returns undefined.
    auto start_algorithm = JS::create_heap_function(realm.heap(), []() -> WebIDL::ExceptionOr<JS::Value> {
        return JS::js_undefined();
    });

    // 3. Let pullAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto pull_algorithm = JS::create_heap_function(realm.heap(), [&realm]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 4. Let cancelAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto cancel_algorithm = JS::create_heap_function(realm.heap(), [&realm](JS::Value) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 5. If underlyingSourceDict["start"] exists, then set startAlgorithm to an algorithm which returns the result of invoking underlyingSourceDict["start"] with argument list « controller » and callback this value underlyingSource.
    if (underlying_source.start) {
        start_algorithm = JS::create_heap_function(realm.heap(), [controller, underlying_source_value, callback = underlying_source.start]() -> WebIDL::ExceptionOr<JS::Value> {
            // Note: callback does not return a promise, so invoke_callback may return an abrupt completion
            return TRY(WebIDL::invoke_callback(*callback, underlying_source_value, controller)).release_value();
        });
    }

    // 6. If underlyingSourceDict["pull"] exists, then set pullAlgorithm to an algorithm which returns the result of invoking underlyingSourceDict["pull"] with argument list « controller » and callback this value underlyingSource.
    if (underlying_source.pull) {
        pull_algorithm = JS::create_heap_function(realm.heap(), [&realm, controller, underlying_source_value, callback = underlying_source.pull]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_source_value, controller)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 7. If underlyingSourceDict["cancel"] exists, then set cancelAlgorithm to an algorithm which takes an argument reason and returns the result of invoking underlyingSourceDict["cancel"] with argument list « reason » and callback this value underlyingSource.
    if (underlying_source.cancel) {
        cancel_algorithm = JS::create_heap_function(realm.heap(), [&realm, underlying_source_value, callback = underlying_source.cancel](JS::Value reason) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_source_value, reason)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 8. Perform ? SetUpReadableStreamDefaultController(stream, controller, startAlgorithm, pullAlgorithm, cancelAlgorithm, highWaterMark, sizeAlgorithm).
    return set_up_readable_stream_default_controller(stream, controller, start_algorithm, pull_algorithm, cancel_algorithm, high_water_mark, size_algorithm);
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-call-pull-if-needed
WebIDL::ExceptionOr<void> readable_byte_stream_controller_call_pull_if_needed(ReadableByteStreamController& controller)
{
    // 1. Let shouldPull be ! ReadableByteStreamControllerShouldCallPull(controller).
    auto should_pull = readable_byte_stream_controller_should_call_pull(controller);

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
    auto pull_promise = TRY(controller.pull_algorithm()->function()());

    // 7. Upon fulfillment of pullPromise,
    WebIDL::upon_fulfillment(*pull_promise, [&](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Set controller.[[pulling]] to false.
        controller.set_pulling(false);

        // 2. If controller.[[pullAgain]] is true,
        if (controller.pull_again()) {
            // 1. Set controller.[[pullAgain]] to false.
            controller.set_pull_again(false);

            // 2. Perform ! ReadableByteStreamControllerCallPullIfNeeded(controller).
            TRY(readable_byte_stream_controller_call_pull_if_needed(controller));
        }

        return JS::js_undefined();
    });

    // 8. Upon rejection of pullPromise with reason e,
    WebIDL::upon_rejection(*pull_promise, [&](auto const& error) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! ReadableByteStreamControllerError(controller, e).
        readable_byte_stream_controller_error(controller, error);

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-clear-algorithms
void readable_byte_stream_controller_clear_algorithms(ReadableByteStreamController& controller)
{
    // 1. Set controller.[[pullAlgorithm]] to undefined.
    controller.set_pull_algorithm({});

    // 2. Set controller.[[cancelAlgorithm]] to undefined.
    controller.set_cancel_algorithm({});
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-clear-pending-pull-intos
void readable_byte_stream_controller_clear_pending_pull_intos(ReadableByteStreamController& controller)
{
    // 1. Perform ! ReadableByteStreamControllerInvalidateBYOBRequest(controller).
    readable_byte_stream_controller_invalidate_byob_request(controller);

    // 2. Set controller.[[pendingPullIntos]] to a new empty list.
    controller.pending_pull_intos().clear();
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-close
WebIDL::ExceptionOr<void> readable_byte_stream_controller_close(ReadableByteStreamController& controller)
{
    auto& realm = controller.realm();

    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If controller.[[closeRequested]] is true or stream.[[state]] is not "readable", return.
    if (controller.close_requested() || stream->state() != ReadableStream::State::Readable)
        return {};

    // 3. If controller.[[queueTotalSize]] > 0,
    if (controller.queue_total_size() > 0.0) {
        // 1. Set controller.[[closeRequested]] to true.
        controller.set_close_requested(true);

        // 2. Return.
        return {};
    }

    // 4. If controller.[[pendingPullIntos]] is not empty,
    if (!controller.pending_pull_intos().is_empty()) {
        // 1. Let firstPendingPullInto be controller.[[pendingPullIntos]][0].
        auto& first_pending_pull_into = controller.pending_pull_intos().first();

        // 2. If firstPendingPullInto’s bytes filled > 0,
        if (first_pending_pull_into.bytes_filled > 0) {
            // 1. Let e be a new TypeError exception.
            auto error = JS::TypeError::create(realm, "Cannot close controller in the middle of processing a write request"sv);

            // 2. Perform ! ReadableByteStreamControllerError(controller, e).
            readable_byte_stream_controller_error(controller, error);

            // 3. Throw e.
            return JS::throw_completion(error);
        }
    }

    // 5. Perform ! ReadableByteStreamControllerClearAlgorithms(controller).
    readable_byte_stream_controller_clear_algorithms(controller);

    // 6. Perform ! ReadableStreamClose(stream).
    readable_stream_close(*stream);

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-error
void readable_byte_stream_controller_error(ReadableByteStreamController& controller, JS::Value error)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If stream.[[state]] is not "readable", return.
    if (stream->state() != ReadableStream::State::Readable)
        return;

    // 3. Perform ! ReadableByteStreamControllerClearPendingPullIntos(controller).
    readable_byte_stream_controller_clear_pending_pull_intos(controller);

    // 4. Perform ! ResetQueue(controller).
    reset_queue(controller);

    // 5. Perform ! ReadableByteStreamControllerClearAlgorithms(controller).
    readable_byte_stream_controller_clear_algorithms(controller);

    // 6. Perform ! ReadableStreamError(stream, e).
    readable_stream_error(*stream, error);
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablebytestreamcontrollerfillreadrequestfromqueue
WebIDL::ExceptionOr<void> readable_byte_stream_controller_fill_read_request_from_queue(ReadableByteStreamController& controller, JS::NonnullGCPtr<ReadRequest> read_request)
{
    auto& vm = controller.vm();
    auto& realm = controller.realm();

    // 1. Assert: controller.[[queueTotalSize]] > 0.
    VERIFY(controller.queue_total_size() > 0.0);

    // 2. Let entry be controller.[[queue]][0].
    // 3. Remove entry from controller.[[queue]].
    auto entry = controller.queue().take_first();

    // 4. Set controller.[[queueTotalSize]] to controller.[[queueTotalSize]] − entry’s byte length.
    controller.set_queue_total_size(controller.queue_total_size() - entry.byte_length);

    // 5. Perform ! ReadableByteStreamControllerHandleQueueDrain(controller).
    TRY(readable_byte_stream_controller_handle_queue_drain(controller));

    // 6. Let view be ! Construct(%Uint8Array%, « entry’s buffer, entry’s byte offset, entry’s byte length »).
    auto view = MUST_OR_THROW_OOM(JS::construct(vm, *realm.intrinsics().uint8_array_constructor(), entry.buffer, JS::Value(entry.byte_offset), JS::Value(entry.byte_length)));

    // 7. Perform readRequest’s chunk steps, given view.
    read_request->on_chunk(view);

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-get-desired-size
Optional<double> readable_byte_stream_controller_get_desired_size(ReadableByteStreamController const& controller)
{
    auto stream = controller.stream();

    // 1. Let state be controller.[[stream]].[[state]].
    // 2. If state is "errored", return null.
    if (stream->is_errored())
        return {};

    // 3. If state is "closed", return 0.
    if (stream->is_closed())
        return 0.0;

    // 4. Return controller.[[strategyHWM]] − controller.[[queueTotalSize]].
    return controller.strategy_hwm() - controller.queue_total_size();
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-handle-queue-drain
WebIDL::ExceptionOr<void> readable_byte_stream_controller_handle_queue_drain(ReadableByteStreamController& controller)
{
    // 1. Assert: controller.[[stream]].[[state]] is "readable".
    VERIFY(controller.stream()->state() == ReadableStream::State::Readable);

    // 2. If controller.[[queueTotalSize]] is 0 and controller.[[closeRequested]] is true,
    if (controller.queue_total_size() == 0.0 && controller.close_requested()) {
        // 1. Perform ! ReadableByteStreamControllerClearAlgorithms(controller).
        readable_byte_stream_controller_clear_algorithms(controller);

        // 2. Perform ! ReadableStreamClose(controller.[[stream]]).
        readable_stream_close(*controller.stream());
    }
    // 3. Otherwise,
    else {
        // 1. Perform ! ReadableByteStreamControllerCallPullIfNeeded(controller).
        TRY(readable_byte_stream_controller_call_pull_if_needed(controller));
    }

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-invalidate-byob-request
void readable_byte_stream_controller_invalidate_byob_request(ReadableByteStreamController& controller)
{
    // 1. If controller.[[byobRequest]] is null, return.
    if (!controller.byob_request())
        return;

    // 2. Set controller.[[byobRequest]].[[controller]] to undefined.
    controller.byob_request()->set_controller({});

    // 3. Set controller.[[byobRequest]].[[view]] to null.
    controller.byob_request()->set_view({});

    // 4. Set controller.[[byobRequest]] to null.
    controller.set_byob_request({});
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-error
bool readable_byte_stream_controller_should_call_pull(ReadableByteStreamController const& controller)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If stream.[[state]] is not "readable", return false.
    if (stream->state() != ReadableStream::State::Readable)
        return false;

    // 3. If controller.[[closeRequested]] is true, return false.
    if (controller.close_requested())
        return false;

    // 4. If controller.[[started]] is false, return false.
    if (!controller.started())
        return false;

    // 5. If ! ReadableStreamHasDefaultReader(stream) is true and ! ReadableStreamGetNumReadRequests(stream) > 0, return true.
    if (readable_stream_has_default_reader(*stream) && readable_stream_get_num_read_requests(*stream) > 0)
        return true;

    // 6. If ! ReadableStreamHasBYOBReader(stream) is true and ! ReadableStreamGetNumReadIntoRequests(stream) > 0, return true.
    if (readable_stream_has_byob_reader(*stream) && readable_stream_get_num_read_into_requests(*stream) > 0)
        return true;

    // 7. Let desiredSize be ! ReadableByteStreamControllerGetDesiredSize(controller).
    auto desired_size = readable_byte_stream_controller_get_desired_size(controller);

    // 8. Assert: desiredSize is not null.
    VERIFY(desired_size.has_value());

    // 9. If desiredSize > 0, return true.
    if (*desired_size > 0.0)
        return true;

    // 10. Return false.
    return false;
}

// https://streams.spec.whatwg.org/#create-readable-stream
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStream>> create_readable_stream(JS::Realm& realm, JS::NonnullGCPtr<StartAlgorithm> start_algorithm, JS::NonnullGCPtr<PullAlgorithm> pull_algorithm, JS::NonnullGCPtr<CancelAlgorithm> cancel_algorithm, Optional<double> high_water_mark, JS::GCPtr<SizeAlgorithm> size_algorithm)
{
    // 1. If highWaterMark was not passed, set it to 1.
    if (!high_water_mark.has_value())
        high_water_mark = 1.0;

    // 2. If sizeAlgorithm was not passed, set it to an algorithm that returns 1.
    if (!size_algorithm)
        size_algorithm = JS::create_heap_function(realm.heap(), [](JS::Value) { return JS::normal_completion(JS::Value(1)); });

    // 3. Assert: ! IsNonNegativeNumber(highWaterMark) is true.
    VERIFY(is_non_negative_number(JS::Value { *high_water_mark }));

    // 4. Let stream be a new ReadableStream.
    auto stream = realm.heap().allocate<ReadableStream>(realm, realm);

    // 5. Perform ! InitializeReadableStream(stream).
    initialize_readable_stream(*stream);

    // 6. Let controller be a new ReadableStreamDefaultController.
    auto controller = realm.heap().allocate<ReadableStreamDefaultController>(realm, realm);

    // 7. Perform ? SetUpReadableStreamDefaultController(stream, controller, startAlgorithm, pullAlgorithm, cancelAlgorithm, highWaterMark, sizeAlgorithm).
    TRY(set_up_readable_stream_default_controller(*stream, *controller, start_algorithm, pull_algorithm, cancel_algorithm, *high_water_mark, *size_algorithm));

    // 8. Return stream.
    return stream;
}

// https://streams.spec.whatwg.org/#abstract-opdef-createreadablebytestream
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStream>> create_readable_byte_stream(JS::Realm& realm, JS::NonnullGCPtr<StartAlgorithm> start_algorithm, JS::NonnullGCPtr<PullAlgorithm> pull_algorithm, JS::NonnullGCPtr<CancelAlgorithm> cancel_algorithm)
{
    // 1. Let stream be a new ReadableStream.
    auto stream = realm.heap().allocate<ReadableStream>(realm, realm);

    // 2. Perform ! InitializeReadableStream(stream).
    initialize_readable_stream(*stream);

    // 3. Let controller be a new ReadableByteStreamController.
    auto controller = realm.heap().allocate<ReadableByteStreamController>(realm, realm);

    // 4. Perform ? SetUpReadableByteStreamController(stream, controller, startAlgorithm, pullAlgorithm, cancelAlgorithm, 0, undefined).
    TRY(set_up_readable_byte_stream_controller(stream, controller, start_algorithm, pull_algorithm, cancel_algorithm, 0, JS::js_undefined()));

    // 5. Return stream.
    return stream;
}

// https://streams.spec.whatwg.org/#create-writable-stream
WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStream>> create_writable_stream(JS::Realm& realm, JS::NonnullGCPtr<StartAlgorithm> start_algorithm, JS::NonnullGCPtr<WriteAlgorithm> write_algorithm, JS::NonnullGCPtr<CloseAlgorithm> close_algorithm, JS::NonnullGCPtr<AbortAlgorithm> abort_algorithm, double high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> size_algorithm)
{
    // 1. Assert: ! IsNonNegativeNumber(highWaterMark) is true.
    VERIFY(is_non_negative_number(JS::Value { high_water_mark }));

    // 2. Let stream be a new WritableStream.
    auto stream = realm.heap().allocate<WritableStream>(realm, realm);

    // 3. Perform ! InitializeWritableStream(stream).
    initialize_writable_stream(*stream);

    // 4. Let controller be a new WritableStreamDefaultController.
    auto controller = realm.heap().allocate<WritableStreamDefaultController>(realm, realm);

    // 5. Perform ? SetUpWritableStreamDefaultController(stream, controller, startAlgorithm, writeAlgorithm, closeAlgorithm, abortAlgorithm, highWaterMark, sizeAlgorithm).
    TRY(set_up_writable_stream_default_controller(*stream, *controller, move(start_algorithm), move(write_algorithm), move(close_algorithm), move(abort_algorithm), high_water_mark, move(size_algorithm)));

    // 6. Return stream.
    return stream;
}

// https://streams.spec.whatwg.org/#initialize-readable-stream
void initialize_readable_stream(ReadableStream& stream)
{
    // 1. Set stream.[[state]] to "readable".
    stream.set_state(ReadableStream::State::Readable);

    // 2. Set stream.[[reader]] and stream.[[storedError]] to undefined.
    stream.set_reader({});
    stream.set_stored_error({});

    // 3. Set stream.[[disturbed]] to false.
    stream.set_disturbed(false);
}

// https://streams.spec.whatwg.org/#initialize-writable-stream
void initialize_writable_stream(WritableStream& stream)
{
    // 1. Set stream.[[state]] to "writable".
    stream.set_state(WritableStream::State::Writable);

    // 2. Set stream.[[storedError]], stream.[[writer]], stream.[[controller]], stream.[[inFlightWriteRequest]],
    //    stream.[[closeRequest]], stream.[[inFlightCloseRequest]], and stream.[[pendingAbortRequest]] to undefined.
    stream.set_stored_error(JS::js_undefined());
    stream.set_writer({});
    stream.set_controller({});
    stream.set_in_flight_write_request({});
    stream.set_close_request({});
    stream.set_in_flight_close_request({});
    stream.set_pending_abort_request({});

    // 3. Set stream.[[writeRequests]] to a new empty list.
    stream.write_requests().clear();

    // 4. Set stream.[[backpressure]] to false.
    stream.set_backpressure(false);
}

// https://streams.spec.whatwg.org/#acquire-writable-stream-default-writer
WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStreamDefaultWriter>> acquire_writable_stream_default_writer(WritableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Let writer be a new WritableStreamDefaultWriter.
    auto writer = stream.heap().allocate<WritableStreamDefaultWriter>(realm, realm);

    // 2. Perform ? SetUpWritableStreamDefaultWriter(writer, stream).
    TRY(set_up_writable_stream_default_writer(*writer, stream));

    // 3. Return writer.
    return writer;
}

// https://streams.spec.whatwg.org/#is-writable-stream-locked
bool is_writable_stream_locked(WritableStream const& stream)
{
    // 1. If stream.[[writer]] is undefined, return false.
    if (!stream.writer())
        return false;

    // 2. Return true.
    return true;
}

// https://streams.spec.whatwg.org/#set-up-writable-stream-default-writer
WebIDL::ExceptionOr<void> set_up_writable_stream_default_writer(WritableStreamDefaultWriter& writer, WritableStream& stream)
{
    auto& realm = writer.realm();

    // 1. If ! IsWritableStreamLocked(stream) is true, throw a TypeError exception.
    if (is_writable_stream_locked(stream))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Stream is locked"sv };

    // 2. Set writer.[[stream]] to stream.
    writer.set_stream(stream);

    // 3. Set stream.[[writer]] to writer.
    stream.set_writer(writer);

    // 4. Let state be stream.[[state]].
    auto state = stream.state();

    // 5. If state is "writable",
    if (state == WritableStream::State::Writable) {
        // 1. If ! WritableStreamCloseQueuedOrInFlight(stream) is false and stream.[[backpressure]] is true, set writer.[[readyPromise]] to a new promise.
        if (!writable_stream_close_queued_or_in_flight(stream) && stream.backpressure()) {
            writer.set_ready_promise(WebIDL::create_promise(realm));
        }
        // 2. Otherwise, set writer.[[readyPromise]] to a promise resolved with undefined.
        else {
            writer.set_ready_promise(WebIDL::create_resolved_promise(realm, JS::js_undefined()));
        }

        // 3. Set writer.[[closedPromise]] to a new promise.
        writer.set_closed_promise(WebIDL::create_promise(realm));
    }
    // 6. Otherwise, if state is "erroring",
    else if (state == WritableStream::State::Erroring) {
        // 1. Set writer.[[readyPromise]] to a promise rejected with stream.[[storedError]].
        writer.set_ready_promise(WebIDL::create_rejected_promise(realm, stream.stored_error()));

        // 2. Set writer.[[readyPromise]].[[PromiseIsHandled]] to true.
        WebIDL::mark_promise_as_handled(*writer.ready_promise());

        // 3. Set writer.[[closedPromise]] to a new promise.
        writer.set_closed_promise(WebIDL::create_promise(realm));
    }
    // 7. Otherwise, if state is "closed",
    else if (state == WritableStream::State::Closed) {
        // 1. Set writer.[[readyPromise]] to a promise resolved with undefined.
        writer.set_ready_promise(WebIDL::create_resolved_promise(realm, JS::js_undefined()));

        // 2. Set writer.[[closedPromise]] to a promise resolved with undefined.
        writer.set_closed_promise(WebIDL::create_resolved_promise(realm, JS::js_undefined()));
    }
    // 8. Otherwise,
    else {
        // 1. Assert: state is "errored".
        VERIFY(state == WritableStream::State::Errored);

        // 2. Let storedError be stream.[[storedError]].
        auto stored_error = stream.stored_error();

        // 3. Set writer.[[readyPromise]] to a promise rejected with storedError.
        writer.set_ready_promise(WebIDL::create_rejected_promise(realm, stored_error));

        // 4. Set writer.[[readyPromise]].[[PromiseIsHandled]] to true.
        WebIDL::mark_promise_as_handled(*writer.ready_promise());

        // 5. Set writer.[[closedPromise]] to a promise rejected with storedError.
        writer.set_closed_promise(WebIDL::create_rejected_promise(realm, stored_error));

        // 6. Set writer.[[closedPromise]].[[PromiseIsHandled]] to true.
        WebIDL::mark_promise_as_handled(*writer.closed_promise());
    }

    return {};
}

// https://streams.spec.whatwg.org/#set-up-readable-byte-stream-controller
WebIDL::ExceptionOr<void> set_up_readable_byte_stream_controller(ReadableStream& stream, ReadableByteStreamController& controller, JS::NonnullGCPtr<StartAlgorithm> start_algorithm, JS::NonnullGCPtr<PullAlgorithm> pull_algorithm, JS::NonnullGCPtr<CancelAlgorithm> cancel_algorithm, double high_water_mark, JS::Value auto_allocate_chunk_size)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[controller]] is undefined.
    VERIFY(!stream.controller().has_value());

    // 2. If autoAllocateChunkSize is not undefined,
    if (!auto_allocate_chunk_size.is_undefined()) {
        // 1. Assert: ! IsInteger(autoAllocateChunkSize) is true.
        VERIFY(auto_allocate_chunk_size.is_integral_number());

        // 2. Assert: autoAllocateChunkSize is positive.
        VERIFY(auto_allocate_chunk_size.as_double() > 0);
    }

    // 3. Set controller.[[stream]] to stream.
    controller.set_stream(stream);

    // 4. Set controller.[[pullAgain]] and controller.[[pulling]] to false.
    controller.set_pull_again(false);
    controller.set_pulling(false);

    // 5. Set controller.[[byobRequest]] to null.
    controller.set_byob_request({});

    // 6. Perform ! ResetQueue(controller).
    reset_queue(controller);

    // 7. Set controller.[[closeRequested]] and controller.[[started]] to false.
    controller.set_close_requested(false);
    controller.set_started(false);

    // 8. Set controller.[[strategyHWM]] to highWaterMark.
    controller.set_strategy_hwm(high_water_mark);

    // 9. Set controller.[[pullAlgorithm]] to pullAlgorithm.
    controller.set_pull_algorithm(pull_algorithm);

    // 10. Set controller.[[cancelAlgorithm]] to cancelAlgorithm.
    controller.set_cancel_algorithm(cancel_algorithm);

    // 11. Set controller.[[autoAllocateChunkSize]] to autoAllocateChunkSize.
    if (auto_allocate_chunk_size.is_integral_number())
        controller.set_auto_allocate_chunk_size(auto_allocate_chunk_size.as_double());

    // 12. Set controller.[[pendingPullIntos]] to a new empty list.
    controller.pending_pull_intos().clear();

    // 13. Set stream.[[controller]] to controller.
    stream.set_controller(ReadableStreamController { controller });

    // 14. Let startResult be the result of performing startAlgorithm.
    auto start_result = TRY(start_algorithm->function()());

    // 15. Let startPromise be a promise resolved with startResult.
    auto start_promise = WebIDL::create_resolved_promise(realm, start_result);

    // 16. Upon fulfillment of startPromise,
    WebIDL::upon_fulfillment(start_promise, [&](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Set controller.[[started]] to true.
        controller.set_started(true);

        // 2. Assert: controller.[[pulling]] is false.
        VERIFY(!controller.pulling());

        // 3. Assert: controller.[[pullAgain]] is false.
        VERIFY(!controller.pull_again());

        // 4. Perform ! ReadableByteStreamControllerCallPullIfNeeded(controller).
        TRY(readable_byte_stream_controller_call_pull_if_needed(controller));

        return JS::js_undefined();
    });

    // 17. Upon rejection of startPromise with reason r,
    WebIDL::upon_rejection(start_promise, [&](auto const& r) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! ReadableByteStreamControllerError(controller, r).
        readable_byte_stream_controller_error(controller, r);

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#readablestream-enqueue
WebIDL::ExceptionOr<void> readable_stream_enqueue(ReadableStreamController& controller, JS::Value chunk)
{
    // 1. If stream.[[controller]] implements ReadableStreamDefaultController,
    if (controller.has<JS::NonnullGCPtr<ReadableStreamDefaultController>>()) {
        // 1. Perform ! ReadableStreamDefaultControllerEnqueue(stream.[[controller]], chunk).
        return readable_stream_default_controller_enqueue(controller.get<JS::NonnullGCPtr<ReadableStreamDefaultController>>(), chunk);
    }
    // 2. Otherwise,
    else {
        // 1. Assert: stream.[[controller]] implements ReadableByteStreamController.
        VERIFY(controller.has<JS::NonnullGCPtr<ReadableByteStreamController>>());
        auto readable_byte_controller = controller.get<JS::NonnullGCPtr<ReadableByteStreamController>>();

        // FIXME: 2. Assert: chunk is an ArrayBufferView.

        // 3. Let byobView be the current BYOB request view for stream.
        // FIXME: This is not what the spec means by 'current BYOB request view'
        auto byob_view = readable_byte_controller->raw_byob_request();

        // 4. If byobView is non-null, and chunk.[[ViewedArrayBuffer]] is byobView.[[ViewedArrayBuffer]], then:
        if (byob_view) {
            // FIXME: 1. Assert: chunk.[[ByteOffset]] is byobView.[[ByteOffset]].
            // FIXME: 2. Assert: chunk.[[ByteLength]] ≤ byobView.[[ByteLength]].
            // FIXME: 3. Perform ? ReadableByteStreamControllerRespond(stream.[[controller]], chunk.[[ByteLength]]).
            TODO();
        }

        // 5. Otherwise, perform ? ReadableByteStreamControllerEnqueue(stream.[[controller]], chunk).
        return readable_byte_stream_controller_enqueue(readable_byte_controller, chunk);
    }
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-enqueue
WebIDL::ExceptionOr<void> readable_byte_stream_controller_enqueue(ReadableByteStreamController& controller, JS::Value chunk)
{
    auto& vm = controller.vm();
    auto& realm = controller.realm();

    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If controller.[[closeRequested]] is true or stream.[[state]] is not "readable", return.
    if (controller.close_requested() || stream->state() != ReadableStream ::State::Readable)
        return {};

    // 3. Let buffer be chunk.[[ViewedArrayBuffer]].
    auto* typed_array = TRY(JS::typed_array_from(vm, chunk));
    auto* buffer = typed_array->viewed_array_buffer();

    // 4. Let byteOffset be chunk.[[ByteOffset]].
    auto byte_offset = typed_array->byte_offset();

    // 6. If ! IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    // FIXME: The streams spec has not been updated for resizable ArrayBuffer objects. We must perform step 6 before
    //        invoking TypedArrayByteLength in step 5. We also must check if the array is out-of-bounds, rather than
    //        just detached.
    auto typed_array_record = JS::make_typed_array_with_buffer_witness_record(*typed_array, JS::ArrayBuffer::Order::SeqCst);

    if (JS::is_typed_array_out_of_bounds(typed_array_record))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::BufferOutOfBounds, "TypedArray"sv);

    // 5. Let byteLength be chunk.[[ByteLength]].
    auto byte_length = JS::typed_array_byte_length(typed_array_record);

    // 7. Let transferredBuffer be ? TransferArrayBuffer(buffer).
    auto transferred_buffer = TRY(transfer_array_buffer(realm, *buffer));

    // 8. If controller.[[pendingPullIntos]] is not empty,
    if (!controller.pending_pull_intos().is_empty()) {
        // 1. Let firstPendingPullInto be controller.[[pendingPullIntos]][0].
        auto& first_pending_pull_into = controller.pending_pull_intos().first();

        // 2. If ! IsDetachedBuffer(firstPendingPullInto’s buffer) is true, throw a TypeError exception.
        if (first_pending_pull_into.buffer->is_detached()) {
            auto error = JS::TypeError::create(realm, "Buffer is detached"sv);
            return JS::throw_completion(error);
        }

        // 3. Perform ! ReadableByteStreamControllerInvalidateBYOBRequest(controller).
        readable_byte_stream_controller_invalidate_byob_request(controller);

        // 4. Set firstPendingPullInto’s buffer to ! TransferArrayBuffer(firstPendingPullInto’s buffer).
        first_pending_pull_into.buffer = TRY(transfer_array_buffer(realm, first_pending_pull_into.buffer));

        // 5. If firstPendingPullInto’s reader type is "none", perform ? ReadableByteStreamControllerEnqueueDetachedPullIntoToQueue(controller, firstPendingPullInto).
        if (first_pending_pull_into.reader_type == ReaderType::None)
            TRY(readable_byte_stream_controller_enqueue_detached_pull_into_queue(controller, first_pending_pull_into));
    }

    // 9. If ! ReadableStreamHasDefaultReader(stream) is true,
    if (readable_stream_has_default_reader(*stream)) {
        // 1. Perform ! ReadableByteStreamControllerProcessReadRequestsUsingQueue(controller).
        TRY(readable_byte_stream_controller_process_read_requests_using_queue(controller));

        // 2. If ! ReadableStreamGetNumReadRequests(stream) is 0,
        if (readable_stream_get_num_read_requests(*stream) == 0) {
            // 1. Assert: controller.[[pendingPullIntos]] is empty.
            VERIFY(controller.pending_pull_intos().is_empty());

            // 2. Perform ! ReadableByteStreamControllerEnqueueChunkToQueue(controller, transferredBuffer, byteOffset, byteLength).
            readable_byte_stream_controller_enqueue_chunk_to_queue(controller, transferred_buffer, byte_offset, byte_length);
        }
        // 3. Otherwise.
        else {
            // 1. Assert: controller.[[queue]] is empty.
            VERIFY(controller.queue().is_empty());

            // 2. If controller.[[pendingPullIntos]] is not empty,
            if (!controller.pending_pull_intos().is_empty()) {
                // 1. Assert: controller.[[pendingPullIntos]][0]'s reader type is "default".
                VERIFY(controller.pending_pull_intos().first().reader_type == ReaderType::Default);

                // 2. Perform ! ReadableByteStreamControllerShiftPendingPullInto(controller).
                readable_byte_stream_controller_shift_pending_pull_into(controller);
            }

            // 3. Let transferredView be ! Construct(%Uint8Array%, « transferredBuffer, byteOffset, byteLength »).
            auto transferred_view = MUST_OR_THROW_OOM(JS::construct(vm, *realm.intrinsics().uint8_array_constructor(), transferred_buffer, JS::Value(byte_offset), JS::Value(byte_length)));

            // 4. Perform ! ReadableStreamFulfillReadRequest(stream, transferredView, false).
            readable_stream_fulfill_read_request(*stream, transferred_view, false);
        }
    }
    // 10. Otherwise, if ! ReadableStreamHasBYOBReader(stream) is true,
    else if (readable_stream_has_byob_reader(*stream)) {
        // 1. Perform ! ReadableByteStreamControllerEnqueueChunkToQueue(controller, transferredBuffer, byteOffset, byteLength).
        readable_byte_stream_controller_enqueue_chunk_to_queue(controller, transferred_buffer, byte_offset, byte_length);

        // 2. Perform ! ReadableByteStreamControllerProcessPullIntoDescriptorsUsingQueue(controller).
        readable_byte_stream_controller_process_pull_into_descriptors_using_queue(controller);
    }
    // 11. Otherwise,
    else {
        // 1. Assert: ! IsReadableStreamLocked(stream) is false.
        VERIFY(!is_readable_stream_locked(*stream));

        // 2. Perform ! ReadableByteStreamControllerEnqueueChunkToQueue(controller, transferredBuffer, byteOffset, byteLength).
        readable_byte_stream_controller_enqueue_chunk_to_queue(controller, transferred_buffer, byte_offset, byte_length);
    }

    // 12. Perform ! ReadableByteStreamControllerCallPullIfNeeded(controller).
    TRY(readable_byte_stream_controller_call_pull_if_needed(controller));

    return {};
}

// https://streams.spec.whatwg.org/#transfer-array-buffer
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> transfer_array_buffer(JS::Realm& realm, JS::ArrayBuffer& buffer)
{
    auto& vm = realm.vm();

    // 1. Assert: ! IsDetachedBuffer(O) is false.
    VERIFY(!buffer.is_detached());

    // 2. Let arrayBufferData be O.[[ArrayBufferData]].
    // 3. Let arrayBufferByteLength be O.[[ArrayBufferByteLength]].
    auto array_buffer = buffer.buffer();

    // 4. Perform ? DetachArrayBuffer(O).
    TRY(JS::detach_array_buffer(vm, buffer));

    // 5. Return a new ArrayBuffer object, created in the current Realm, whose [[ArrayBufferData]] internal slot value is arrayBufferData and whose [[ArrayBufferByteLength]] internal slot value is arrayBufferByteLength.
    return JS::ArrayBuffer::create(realm, move(array_buffer));
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablebytestreamcontrollerenqueuedetachedpullintotoqueue
WebIDL::ExceptionOr<void> readable_byte_stream_controller_enqueue_detached_pull_into_queue(ReadableByteStreamController& controller, PullIntoDescriptor& pull_into_descriptor)
{
    // 1. Assert: pullIntoDescriptor’s reader type is "none".
    VERIFY(pull_into_descriptor.reader_type == ReaderType::None);

    // 2. If pullIntoDescriptor’s bytes filled > 0, perform ? ReadableByteStreamControllerEnqueueClonedChunkToQueue(controller, pullIntoDescriptor’s buffer, pullIntoDescriptor’s byte offset, pullIntoDescriptor’s bytes filled).
    if (pull_into_descriptor.bytes_filled > 0)
        TRY(readable_byte_stream_controller_enqueue_cloned_chunk_to_queue(controller, pull_into_descriptor.buffer, pull_into_descriptor.byte_offset, pull_into_descriptor.bytes_filled));

    // 3. Perform ! ReadableByteStreamControllerShiftPendingPullInto(controller).
    readable_byte_stream_controller_shift_pending_pull_into(controller);
    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-commit-pull-into-descriptor
void readable_byte_stream_controller_commit_pull_into_descriptor(ReadableStream& stream, PullIntoDescriptor const& pull_into_descriptor)
{
    // 1. Assert: stream.[[state]] is not "errored".
    VERIFY(!stream.is_errored());

    // 2. Assert: pullIntoDescriptor.reader type is not "none".
    VERIFY(pull_into_descriptor.reader_type != ReaderType::None);

    // 3. Let done be false.
    bool done = false;

    // 4. If stream.[[state]] is "closed",
    if (stream.is_closed()) {
        // 1. Assert: pullIntoDescriptor’s bytes filled is 0.
        VERIFY(pull_into_descriptor.bytes_filled == 0);

        // 2. Set done to true.
        done = true;
    }

    // 5. Let filledView be ! ReadableByteStreamControllerConvertPullIntoDescriptor(pullIntoDescriptor).
    auto filled_view = readable_byte_stream_controller_convert_pull_into_descriptor(stream.realm(), pull_into_descriptor);

    // 6. If pullIntoDescriptor’s reader type is "default",
    if (pull_into_descriptor.reader_type == ReaderType::Default) {
        // 1. Perform ! ReadableStreamFulfillReadRequest(stream, filledView, done).
        readable_stream_fulfill_read_request(stream, filled_view, done);
    }
    // 7. Otherwise,
    else {
        // 1. Assert: pullIntoDescriptor’s reader type is "byob".
        VERIFY(pull_into_descriptor.reader_type == ReaderType::Byob);

        // 2. Perform ! ReadableStreamFulfillReadIntoRequest(stream, filledView, done).
        readable_stream_fulfill_read_into_request(stream, filled_view, done);
    }
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-process-pull-into-descriptors-using-queue
void readable_byte_stream_controller_process_pull_into_descriptors_using_queue(ReadableByteStreamController& controller)
{
    // 1. Assert: controller.[[closeRequested]] is false.
    VERIFY(!controller.close_requested());

    // 2. While controller.[[pendingPullIntos]] is not empty,
    while (!controller.pending_pull_intos().is_empty()) {
        // 1. If controller.[[queueTotalSize]] is 0, return.
        if (controller.queue_total_size() == 0)
            return;

        // 2. Let pullIntoDescriptor be controller.[[pendingPullIntos]][0].
        auto& pull_into_descriptor = controller.pending_pull_intos().first();

        // 3. If ! ReadableByteStreamControllerFillPullIntoDescriptorFromQueue(controller, pullIntoDescriptor) is true,
        if (readable_byte_stream_controller_fill_pull_into_descriptor_from_queue(controller, pull_into_descriptor)) {
            // NOTE: We store the returned pull into descriptor here as the 'shift pending pull into' will remove
            //       the first entry into the list which we have a reference to above.

            // 1. Perform ! ReadableByteStreamControllerShiftPendingPullInto(controller).
            auto descriptor = readable_byte_stream_controller_shift_pending_pull_into(controller);

            // 2. Perform ! ReadableByteStreamControllerCommitPullIntoDescriptor(controller.[[stream]], pullIntoDescriptor).
            readable_byte_stream_controller_commit_pull_into_descriptor(*controller.stream(), descriptor);
        }
    }
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablebytestreamcontrollerprocessreadrequestsusingqueue
WebIDL::ExceptionOr<void> readable_byte_stream_controller_process_read_requests_using_queue(ReadableByteStreamController& controller)
{
    // 1. Let reader be controller.[[stream]].[[reader]].
    auto reader = controller.stream()->reader();

    // 2. Assert: reader implements ReadableStreamDefaultReader.
    VERIFY(reader->has<JS::NonnullGCPtr<ReadableStreamDefaultReader>>());

    // 3. While reader.[[readRequests]] is not empty,
    auto readable_stream_default_reader = reader->get<JS::NonnullGCPtr<ReadableStreamDefaultReader>>();
    while (!readable_stream_default_reader->read_requests().is_empty()) {
        // 1. If controller.[[queueTotalSize]] is 0, return.
        if (controller.queue_total_size() == 0.0)
            return {};

        // 2. Let readRequest be reader.[[readRequests]][0].
        // 3. Remove readRequest from reader.[[readRequests]].
        auto read_request = readable_stream_default_reader->read_requests().take_first();

        // 4. Perform ! ReadableByteStreamControllerFillReadRequestFromQueue(controller, readRequest).
        TRY(readable_byte_stream_controller_fill_read_request_from_queue(controller, read_request));
    }

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-enqueue-chunk-to-queue
void readable_byte_stream_controller_enqueue_chunk_to_queue(ReadableByteStreamController& controller, JS::NonnullGCPtr<JS::ArrayBuffer> buffer, u32 byte_offset, u32 byte_length)
{
    // 1. Append a new readable byte stream queue entry with buffer buffer, byte offset byteOffset, and byte length byteLength to controller.[[queue]].
    controller.queue().append(ReadableByteStreamQueueEntry {
        .buffer = buffer,
        .byte_offset = byte_offset,
        .byte_length = byte_length,
    });

    // 2. Set controller.[[queueTotalSize]] to controller.[[queueTotalSize]] + byteLength.
    controller.set_queue_total_size(controller.queue_total_size() + byte_length);
}

// https://streams.spec.whatwg.org/#abstract-opdef-readablebytestreamcontrollerenqueueclonedchunktoqueue
WebIDL::ExceptionOr<void> readable_byte_stream_controller_enqueue_cloned_chunk_to_queue(ReadableByteStreamController& controller, JS::ArrayBuffer& buffer, u64 byte_offset, u64 byte_length)
{
    auto& vm = controller.vm();

    // 1. Let cloneResult be CloneArrayBuffer(buffer, byteOffset, byteLength, %ArrayBuffer%).
    auto clone_result = JS::clone_array_buffer(vm, buffer, byte_offset, byte_length);

    // 2. If cloneResult is an abrupt completion,
    if (clone_result.is_throw_completion()) {
        auto throw_completion = Bindings::throw_dom_exception_if_needed(vm, [&] { return clone_result; }).throw_completion();

        // 1. Perform ! ReadableByteStreamControllerError(controller, cloneResult.[[Value]]).
        readable_byte_stream_controller_error(controller, throw_completion.value().value());

        // 2. Return cloneResult.
        // Note: We need to return the throw_completion object here, as enqueue needs to throw the same object that the controller is errored with
        return throw_completion;
    }

    // 3. Perform ! ReadableByteStreamControllerEnqueueChunkToQueue(controller, cloneResult.[[Value]], 0, byteLength).
    readable_byte_stream_controller_enqueue_chunk_to_queue(controller, *clone_result.release_value(), 0, byte_length);

    return {};
}

// https://streams.spec.whatwg.org/#readable-byte-stream-controller-shift-pending-pull-into
PullIntoDescriptor readable_byte_stream_controller_shift_pending_pull_into(ReadableByteStreamController& controller)
{
    // 1. Assert: controller.[[byobRequest]] is null.
    VERIFY(!controller.raw_byob_request());

    // 2. Let descriptor be controller.[[pendingPullIntos]][0].
    // 3. Remove descriptor from controller.[[pendingPullIntos]].
    auto descriptor = controller.pending_pull_intos().take_first();

    // 4. Return descriptor.
    return descriptor;
}

// https://streams.spec.whatwg.org/#readablestream-set-up-with-byte-reading-support
WebIDL::ExceptionOr<void> set_up_readable_stream_controller_with_byte_reading_support(ReadableStream& stream, JS::GCPtr<PullAlgorithm> pull_algorithm, JS::GCPtr<CancelAlgorithm> cancel_algorithm, double high_water_mark)
{
    auto& realm = stream.realm();

    // 1. Let startAlgorithm be an algorithm that returns undefined.
    auto start_algorithm = JS::create_heap_function(realm.heap(), []() -> WebIDL::ExceptionOr<JS::Value> { return JS::js_undefined(); });

    // 2. Let pullAlgorithmWrapper be an algorithm that runs these steps:
    auto pull_algorithm_wrapper = JS::create_heap_function(realm.heap(), [&realm, pull_algorithm]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Let result be the result of running pullAlgorithm, if pullAlgorithm was given, or null otherwise. If this throws an exception e, return a promise rejected with e.
        JS::GCPtr<JS::PromiseCapability> result = nullptr;
        if (pull_algorithm)
            result = TRY(pull_algorithm->function()());

        // 2. If result is a Promise, then return result.
        if (result != nullptr)
            return JS::NonnullGCPtr(*result);

        // 3. Return a promise resolved with undefined.
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 3. Let cancelAlgorithmWrapper be an algorithm that runs these steps:
    auto cancel_algorithm_wrapper = JS::create_heap_function(realm.heap(), [&realm, cancel_algorithm](JS::Value c) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Let result be the result of running cancelAlgorithm, if cancelAlgorithm was given, or null otherwise. If this throws an exception e, return a promise rejected with e.
        JS::GCPtr<JS::PromiseCapability> result = nullptr;
        if (cancel_algorithm)
            result = TRY(cancel_algorithm->function()(c));

        // 2. If result is a Promise, then return result.
        if (result != nullptr)
            return JS::NonnullGCPtr(*result);

        // 3. Return a promise resolved with undefined.
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 4. Perform ! InitializeReadableStream(stream).
    // 5. Let controller be a new ReadableByteStreamController.
    auto controller = stream.heap().allocate<ReadableByteStreamController>(realm, realm);

    // 6. Perform ! SetUpReadableByteStreamController(stream, controller, startAlgorithm, pullAlgorithmWrapper, cancelAlgorithmWrapper, highWaterMark, undefined).
    TRY(set_up_readable_byte_stream_controller(stream, controller, start_algorithm, pull_algorithm_wrapper, cancel_algorithm_wrapper, high_water_mark, JS::js_undefined()));

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-abort
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_abort(WritableStream& stream, JS::Value reason)
{
    auto& realm = stream.realm();

    // 1. If stream.[[state]] is "closed" or "errored", return a promise resolved with undefined.
    auto state = stream.state();
    if (state == WritableStream::State::Closed || state == WritableStream::State::Errored)
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());

    // 2. Signal abort on stream.[[controller]].[[signal]] with reason.
    stream.controller()->signal()->signal_abort(reason);

    // 3. Let state be stream.[[state]].
    state = stream.state();

    // 4. If state is "closed" or "errored", return a promise resolved with undefined.
    if (state == WritableStream::State::Closed || state == WritableStream::State::Errored)
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());

    // 5. If stream.[[pendingAbortRequest]] is not undefined, return stream.[[pendingAbortRequest]]'s promise.
    if (stream.pending_abort_request().has_value())
        return stream.pending_abort_request()->promise;

    // 6. Assert: state is "writable" or "erroring".
    VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

    // 7. Let wasAlreadyErroring be false.
    auto was_already_erroring = false;

    // 8. If state is "erroring",
    if (state == WritableStream::State::Erroring) {
        // 1. Set wasAlreadyErroring to true.
        was_already_erroring = true;

        // 2. Set reason to undefined.
        reason = JS::js_undefined();
    }

    // 9. Let promise be a new promise.
    auto promise = WebIDL::create_promise(realm);

    // 10. Set stream.[[pendingAbortRequest]] to a new pending abort request whose promise is promise, reason is reason, and was already erroring is wasAlreadyErroring.
    stream.set_pending_abort_request(PendingAbortRequest { promise, reason, was_already_erroring });

    // 11. If wasAlreadyErroring is false, perform ! WritableStreamStartErroring(stream, reason).
    if (!was_already_erroring)
        TRY(writable_stream_start_erroring(stream, reason));

    // 12. Return promise.
    return promise;
}

// https://streams.spec.whatwg.org/#writable-stream-close
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_close(WritableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Let state be stream.[[state]].
    auto state = stream.state();

    // 2. If state is "closed" or "errored", return a promise rejected with a TypeError exception.
    if (state == WritableStream::State::Closed || state == WritableStream::State::Errored) {
        auto message = state == WritableStream::State::Closed ? "Cannot close a closed stream"sv : "Cannot close an errored stream"sv;
        auto exception = JS::TypeError::create(realm, message);
        return WebIDL::create_rejected_promise(realm, exception);
    }

    // 3. Assert: state is "writable" or "erroring".
    VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

    // 4. Assert: ! WritableStreamCloseQueuedOrInFlight(stream) is false.
    VERIFY(!writable_stream_close_queued_or_in_flight(stream));

    // 5. Let promise be a new promise.
    auto promise = WebIDL::create_promise(realm);

    // 6. Set stream.[[closeRequest]] to promise.
    stream.set_close_request(promise);

    // 7. Let writer be stream.[[writer]].
    auto writer = stream.writer();

    // 8. If writer is not undefined, and stream.[[backpressure]] is true, and state is "writable", resolve writer.[[readyPromise]] with undefined.
    if (writer && stream.backpressure() && state == WritableStream::State::Writable)
        WebIDL::resolve_promise(realm, *writer->ready_promise(), JS::js_undefined());

    // 9. Perform ! WritableStreamDefaultControllerClose(stream.[[controller]]).
    TRY(writable_stream_default_controller_close(*stream.controller()));

    // 10. Return promise.
    return promise;
}

// https://streams.spec.whatwg.org/#writable-stream-add-write-request
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_add_write_request(WritableStream& stream)
{
    auto& realm = stream.realm();
    auto& vm = stream.vm();

    // 1. Assert: ! IsWritableStreamLocked(stream) is true.
    VERIFY(is_writable_stream_locked(stream));

    // 2. Assert: stream.[[state]] is "writable".
    VERIFY(stream.state() == WritableStream::State::Writable);

    // 3. Let promise be a new promise.
    auto promise = WebIDL::create_promise(realm);

    // 4. Append promise to stream.[[writeRequests]].
    TRY_OR_THROW_OOM(vm, stream.write_requests().try_append(promise));

    // 5. Return promise.
    return promise;
}

// https://streams.spec.whatwg.org/#writable-stream-close-queued-or-in-flight
bool writable_stream_close_queued_or_in_flight(WritableStream const& stream)
{
    // 1. If stream.[[closeRequest]] is undefined and stream.[[inFlightCloseRequest]] is undefined, return false.
    if (!stream.close_request() && !stream.in_flight_write_request())
        return false;

    // 2. Return true.
    return true;
}

// https://streams.spec.whatwg.org/#writable-stream-deal-with-rejection
WebIDL::ExceptionOr<void> writable_stream_deal_with_rejection(WritableStream& stream, JS::Value error)
{
    // 1. Let state be stream.[[state]].
    auto state = stream.state();

    // 2. If state is "writable",
    if (state == WritableStream::State::Writable) {
        // 1. Perform ! WritableStreamStartErroring(stream, error).
        // 2. Return.
        return writable_stream_start_erroring(stream, error);
    }

    // 3. Assert: state is "erroring".
    VERIFY(state == WritableStream::State::Erroring);

    // 4. Perform ! WritableStreamFinishErroring(stream).
    return writable_stream_finish_erroring(stream);
}

// https://streams.spec.whatwg.org/#writable-stream-finish-erroring
WebIDL::ExceptionOr<void> writable_stream_finish_erroring(WritableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[state]] is "erroring".
    VERIFY(stream.state() == WritableStream::State::Erroring);

    // 2. Assert: ! WritableStreamHasOperationMarkedInFlight(stream) is false.
    VERIFY(!writable_stream_has_operation_marked_in_flight(stream));

    // 3. Set stream.[[state]] to "errored".
    stream.set_state(WritableStream::State::Errored);

    // 4. Perform ! stream.[[controller]].[[ErrorSteps]]().
    stream.controller()->error_steps();

    // 5. Let storedError be stream.[[storedError]].
    auto stored_error = stream.stored_error();

    // 6. For each writeRequest of stream.[[writeRequests]]:
    for (auto& write_request : stream.write_requests()) {
        // 1. Reject writeRequest with storedError.
        WebIDL::reject_promise(realm, *write_request, stored_error);
    }

    // 7. Set stream.[[writeRequests]] to an empty list.
    stream.write_requests().clear();

    // 8. If stream.[[pendingAbortRequest]] is undefined,
    if (!stream.pending_abort_request().has_value()) {
        // 1. Perform ! WritableStreamRejectCloseAndClosedPromiseIfNeeded(stream).
        writable_stream_reject_close_and_closed_promise_if_needed(stream);

        // 2. Return.
        return {};
    }

    // 9. Let abortRequest be stream.[[pendingAbortRequest]].
    // 10. Set stream.[[pendingAbortRequest]] to undefined.
    auto abort_request = stream.pending_abort_request().release_value();

    // 11. If abortRequest’s was already erroring is true,
    if (abort_request.was_already_erroring) {
        // 1. Reject abortRequest’s promise with storedError.
        WebIDL::reject_promise(realm, abort_request.promise, stored_error);

        // 2. Perform ! WritableStreamRejectCloseAndClosedPromiseIfNeeded(stream).
        writable_stream_reject_close_and_closed_promise_if_needed(stream);

        // 3. Return.
        return {};
    }

    // 12. Let promise be ! stream.[[controller]].[[AbortSteps]](abortRequest’s reason).
    auto promise = TRY(stream.controller()->abort_steps(abort_request.reason));

    // 13. Upon fulfillment of promise,
    WebIDL::upon_fulfillment(*promise, [&, abort_promise = abort_request.promise](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Resolve abortRequest’s promise with undefined.
        WebIDL::resolve_promise(realm, abort_promise, JS::js_undefined());

        // 2. Perform ! WritableStreamRejectCloseAndClosedPromiseIfNeeded(stream).
        writable_stream_reject_close_and_closed_promise_if_needed(stream);

        return JS::js_undefined();
    });

    // 14. Upon rejection of promise with reason reason,
    WebIDL::upon_rejection(*promise, [&, abort_promise = abort_request.promise](auto const& reason) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Reject abortRequest’s promise with reason.
        WebIDL::reject_promise(realm, abort_promise, reason);

        // 2. Perform ! WritableStreamRejectCloseAndClosedPromiseIfNeeded(stream).
        writable_stream_reject_close_and_closed_promise_if_needed(stream);

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-finish-in-flight-close
void writable_stream_finish_in_flight_close(WritableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[inFlightCloseRequest]] is not undefined.
    VERIFY(stream.in_flight_close_request());

    // 2. Resolve stream.[[inFlightCloseRequest]] with undefined.
    WebIDL::resolve_promise(realm, *stream.in_flight_close_request(), JS::js_undefined());

    // 3. Set stream.[[inFlightCloseRequest]] to undefined.
    stream.set_in_flight_close_request({});

    // 4. Let state be stream.[[state]].
    auto state = stream.state();

    // 5. Assert: stream.[[state]] is "writable" or "erroring".
    VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

    // 6. If state is "erroring",
    if (state == WritableStream::State::Erroring) {
        // 1. Set stream.[[storedError]] to undefined.
        stream.set_stored_error(JS::js_undefined());

        // 2. If stream.[[pendingAbortRequest]] is not undefined,
        if (stream.pending_abort_request().has_value()) {
            // 1. Resolve stream.[[pendingAbortRequest]]'s promise with undefined.
            // 2. Set stream.[[pendingAbortRequest]] to undefined.
            WebIDL::resolve_promise(realm, stream.pending_abort_request().release_value().promise, JS::js_undefined());
        }
    }

    // 7. Set stream.[[state]] to "closed".
    stream.set_state(WritableStream::State::Closed);

    // 8. Let writer be stream.[[writer]].
    auto writer = stream.writer();

    // 9. If writer is not undefined, resolve writer.[[closedPromise]] with undefined.
    if (writer)
        WebIDL::resolve_promise(realm, *writer->closed_promise(), JS::js_undefined());

    // 10. Assert: stream.[[pendingAbortRequest]] is undefined.
    VERIFY(!stream.pending_abort_request().has_value());

    // 11. Assert: stream.[[storedError]] is undefined.
    VERIFY(stream.stored_error().is_undefined());
}

// https://streams.spec.whatwg.org/#writable-stream-finish-in-flight-close-with-error
WebIDL::ExceptionOr<void> writable_stream_finish_in_flight_close_with_error(WritableStream& stream, JS::Value error)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[inFlightCloseRequest]] is not undefined.
    VERIFY(stream.in_flight_close_request());

    // 2. Reject stream.[[inFlightCloseRequest]] with error.
    WebIDL::reject_promise(realm, *stream.in_flight_close_request(), error);

    // 3. Set stream.[[inFlightCloseRequest]] to undefined.
    stream.set_in_flight_close_request({});

    // 4. Assert: stream.[[state]] is "writable" or "erroring".
    auto state = stream.state();
    VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

    // 5. If stream.[[pendingAbortRequest]] is not undefined,
    if (stream.pending_abort_request().has_value()) {
        // 1. Reject stream.[[pendingAbortRequest]]'s promise with error.
        // 2. Set stream.[[pendingAbortRequest]] to undefined.
        WebIDL::reject_promise(realm, stream.pending_abort_request().release_value().promise, error);
    }

    // 6. Perform ! WritableStreamDealWithRejection(stream, error).
    return writable_stream_deal_with_rejection(stream, error);
}

// https://streams.spec.whatwg.org/#writable-stream-finish-in-flight-write
void writable_stream_finish_in_flight_write(WritableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[inFlightWriteRequest]] is not undefined.
    VERIFY(stream.in_flight_write_request());

    // 2. Resolve stream.[[inFlightWriteRequest]] with undefined.
    WebIDL::resolve_promise(realm, *stream.in_flight_write_request(), JS::js_undefined());

    // 3. Set stream.[[inFlightWriteRequest]] to undefined.
    stream.set_in_flight_write_request({});
}

// https://streams.spec.whatwg.org/#writable-stream-finish-in-flight-write-with-error
WebIDL::ExceptionOr<void> writable_stream_finish_in_flight_write_with_error(WritableStream& stream, JS::Value error)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[inFlightWriteRequest]] is not undefined.
    VERIFY(stream.in_flight_write_request());

    // 2. Reject stream.[[inFlightWriteRequest]] with error.
    WebIDL::reject_promise(realm, *stream.in_flight_write_request(), error);

    // 3. Set stream.[[inFlightWriteRequest]] to undefined.
    stream.set_in_flight_write_request({});

    // 4. Assert: stream.[[state]] is "writable" or "erroring".
    auto state = stream.state();
    VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

    // 5. Perform ! WritableStreamDealWithRejection(stream, error).
    return writable_stream_deal_with_rejection(stream, error);
}

// https://streams.spec.whatwg.org/#writable-stream-has-operation-marked-in-flight
bool writable_stream_has_operation_marked_in_flight(WritableStream const& stream)
{
    // 1. If stream.[[inFlightWriteRequest]] is undefined and stream.[[inFlightCloseRequest]] is undefined, return false.
    if (!stream.in_flight_write_request() && !stream.in_flight_close_request())
        return false;

    // 2. Return true.
    return true;
}

// https://streams.spec.whatwg.org/#writable-stream-mark-close-request-in-flight
void writable_stream_mark_close_request_in_flight(WritableStream& stream)
{
    // 1. Assert: stream.[[inFlightCloseRequest]] is undefined.
    VERIFY(!stream.in_flight_close_request());

    // 2. Assert: stream.[[closeRequest]] is not undefined.
    VERIFY(stream.close_request());

    // 3. Set stream.[[inFlightCloseRequest]] to stream.[[closeRequest]].
    stream.set_in_flight_close_request(stream.close_request());

    // 4. Set stream.[[closeRequest]] to undefined.
    stream.set_close_request({});
}

// https://streams.spec.whatwg.org/#writable-stream-mark-first-write-request-in-flight
void writable_stream_mark_first_write_request_in_flight(WritableStream& stream)
{
    // 1. Assert: stream.[[inFlightWriteRequest]] is undefined.
    VERIFY(!stream.in_flight_write_request());

    // 2. Assert: stream.[[writeRequests]] is not empty.
    VERIFY(!stream.write_requests().is_empty());

    // 3. Let writeRequest be stream.[[writeRequests]][0].
    // 4. Remove writeRequest from stream.[[writeRequests]].
    auto write_request = stream.write_requests().take_first();

    // 5. Set stream.[[inFlightWriteRequest]] to writeRequest.
    stream.set_in_flight_write_request(write_request);
}

// https://streams.spec.whatwg.org/#writable-stream-reject-close-and-closed-promise-if-needed
void writable_stream_reject_close_and_closed_promise_if_needed(WritableStream& stream)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[state]] is "errored".
    VERIFY(stream.state() == WritableStream::State::Errored);

    // 2. If stream.[[closeRequest]] is not undefined,
    if (stream.close_request()) {
        // 1. Assert: stream.[[inFlightCloseRequest]] is undefined.
        VERIFY(!stream.in_flight_close_request());

        // 2. Reject stream.[[closeRequest]] with stream.[[storedError]].
        WebIDL::reject_promise(realm, *stream.close_request(), stream.stored_error());

        // 3. Set stream.[[closeRequest]] to undefined.
        stream.set_close_request({});
    }

    // 3. Let writer be stream.[[writer]].
    auto writer = stream.writer();

    // 4. If writer is not undefined,
    if (writer) {
        // 1. Reject writer.[[closedPromise]] with stream.[[storedError]].
        WebIDL::reject_promise(realm, *writer->closed_promise(), stream.stored_error());

        // 2. Set writer.[[closedPromise]].[[PromiseIsHandled]] to true.
        WebIDL::mark_promise_as_handled(*writer->closed_promise());
    }
}

// https://streams.spec.whatwg.org/#writable-stream-start-erroring
WebIDL::ExceptionOr<void> writable_stream_start_erroring(WritableStream& stream, JS::Value reason)
{
    // 1. Assert: stream.[[storedError]] is undefined.
    VERIFY(stream.stored_error().is_undefined());

    // 2. Assert: stream.[[state]] is "writable".
    VERIFY(stream.state() == WritableStream::State::Writable);

    // 3. Let controller be stream.[[controller]].
    auto controller = stream.controller();

    // 4. Assert: controller is not undefined.
    VERIFY(controller);

    // 5. Set stream.[[state]] to "erroring".
    stream.set_state(WritableStream::State::Erroring);

    // 6. Set stream.[[storedError]] to reason.
    stream.set_stored_error(reason);

    // 7. Let writer be stream.[[writer]].
    auto writer = stream.writer();

    // 8. If writer is not undefined, perform ! WritableStreamDefaultWriterEnsureReadyPromiseRejected(writer, reason).
    if (writer)
        writable_stream_default_writer_ensure_ready_promise_rejected(*writer, reason);

    // 9. If ! WritableStreamHasOperationMarkedInFlight(stream) is false and controller.[[started]] is true, perform ! WritableStreamFinishErroring(stream).
    if (!writable_stream_has_operation_marked_in_flight(stream) && controller->started())
        TRY(writable_stream_finish_erroring(stream));

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-update-backpressure
void writable_stream_update_backpressure(WritableStream& stream, bool backpressure)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[state]] is "writable".
    VERIFY(stream.state() == WritableStream::State::Writable);

    // 2. Assert: ! WritableStreamCloseQueuedOrInFlight(stream) is false.
    VERIFY(!writable_stream_close_queued_or_in_flight(stream));

    // 3. Let writer be stream.[[writer]].
    auto writer = stream.writer();

    // 4. If writer is not undefined and backpressure is not stream.[[backpressure]],
    if (writer && backpressure != stream.backpressure()) {
        // 1. If backpressure is true, set writer.[[readyPromise]] to a new promise.
        if (backpressure) {
            writer->set_ready_promise(WebIDL::create_promise(realm));
        }
        // 2. Otherwise,
        else {
            // 1. Assert: backpressure is false.

            // 2. Resolve writer.[[readyPromise]] with undefined.
            WebIDL::resolve_promise(realm, *writer->ready_promise(), JS::js_undefined());
        }
    }

    // 5. Set stream.[[backpressure]] to backpressure.
    stream.set_backpressure(backpressure);
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-abort
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_default_writer_abort(WritableStreamDefaultWriter& writer, JS::Value reason)
{
    // 1. Let stream be writer.[[stream]].
    auto stream = writer.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Return ! WritableStreamAbort(stream, reason).
    return writable_stream_abort(*stream, reason);
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-close
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_default_writer_close(WritableStreamDefaultWriter& writer)
{
    // 1. Let stream be writer.[[stream]].
    auto stream = writer.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Return ! WritableStreamClose(stream).
    return writable_stream_close(*stream);
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-ensure-closed-promise-rejected
void writable_stream_default_writer_ensure_closed_promise_rejected(WritableStreamDefaultWriter& writer, JS::Value error)
{
    auto& realm = writer.realm();

    // 1. If writer.[[closedPromise]].[[PromiseState]] is "pending", reject writer.[[closedPromise]] with error.
    auto& closed_promise = verify_cast<JS::Promise>(*writer.closed_promise()->promise());
    if (closed_promise.state() == JS::Promise::State::Pending) {
        WebIDL::reject_promise(realm, *writer.closed_promise(), error);
    }
    // 2. Otherwise, set writer.[[closedPromise]] to a promise rejected with error.
    else {
        writer.set_closed_promise(WebIDL::create_rejected_promise(realm, error));
    }

    // 3. Set writer.[[closedPromise]].[[PromiseIsHandled]] to true.
    WebIDL::mark_promise_as_handled(*writer.closed_promise());
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-ensure-ready-promise-rejected
void writable_stream_default_writer_ensure_ready_promise_rejected(WritableStreamDefaultWriter& writer, JS::Value error)
{
    auto& realm = writer.realm();

    // 1. If writer.[[readyPromise]].[[PromiseState]] is "pending", reject writer.[[readyPromise]] with error.
    auto& ready_promise = verify_cast<JS::Promise>(*writer.ready_promise()->promise());
    if (ready_promise.state() == JS::Promise::State::Pending) {
        WebIDL::reject_promise(realm, *writer.ready_promise(), error);
    }
    // 2. Otherwise, set writer.[[readyPromise]] to a promise rejected with error.
    else {
        writer.set_ready_promise(WebIDL::create_rejected_promise(realm, error));
    }

    // 3. Set writer.[[readyPromise]].[[PromiseIsHandled]] to true.
    WebIDL::mark_promise_as_handled(*writer.ready_promise());
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-get-desired-size
Optional<double> writable_stream_default_writer_get_desired_size(WritableStreamDefaultWriter const& writer)
{
    // 1. Let stream be writer.[[stream]].
    auto stream = writer.stream();

    // 2. Let state be stream.[[state]].
    auto state = stream->state();

    // 3. If state is "errored" or "erroring", return null.
    if (state == WritableStream::State::Errored || state == WritableStream::State::Erroring)
        return {};

    // 4. If state is "closed", return 0.
    if (state == WritableStream::State::Closed)
        return 0.0;

    // 5. Return ! WritableStreamDefaultControllerGetDesiredSize(stream.[[controller]]).
    return writable_stream_default_controller_get_desired_size(*stream->controller());
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-release
WebIDL::ExceptionOr<void> writable_stream_default_writer_release(WritableStreamDefaultWriter& writer)
{
    auto& realm = writer.realm();

    // 1. Let stream be writer.[[stream]].
    auto stream = writer.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Assert: stream.[[writer]] is writer.
    VERIFY(stream->writer().ptr() == &writer);

    // 4. Let releasedError be a new TypeError.
    auto released_error = JS::TypeError::create(realm, "Writer's stream lock has been released"sv);

    // 5. Perform ! WritableStreamDefaultWriterEnsureReadyPromiseRejected(writer, releasedError).
    writable_stream_default_writer_ensure_ready_promise_rejected(writer, released_error);

    // 6. Perform ! WritableStreamDefaultWriterEnsureClosedPromiseRejected(writer, releasedError).
    writable_stream_default_writer_ensure_closed_promise_rejected(writer, released_error);

    // 7. Set stream.[[writer]] to undefined.
    stream->set_writer({});

    // 8. Set writer.[[stream]] to undefined.
    writer.set_stream({});

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-writer-write
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_default_writer_write(WritableStreamDefaultWriter& writer, JS::Value chunk)
{
    auto& realm = writer.realm();

    // 1. Let stream be writer.[[stream]].
    auto stream = writer.stream();

    // 2. Assert: stream is not undefined.
    VERIFY(stream);

    // 3. Let controller be stream.[[controller]].
    auto controller = stream->controller();

    // 4. Let chunkSize be ! WritableStreamDefaultControllerGetChunkSize(controller, chunk).
    auto chunk_size = TRY(writable_stream_default_controller_get_chunk_size(*controller, chunk));

    // 5. If stream is not equal to writer.[[stream]], return a promise rejected with a TypeError exception.
    if (stream.ptr() != writer.stream().ptr()) {
        auto exception = JS::TypeError::create(realm, "Writer's locked stream changed during write"sv);
        return WebIDL::create_rejected_promise(realm, exception);
    }

    // 6. Let state be stream.[[state]].
    auto state = stream->state();

    // 7. If state is "errored", return a promise rejected with stream.[[storedError]].
    if (state == WritableStream::State::Errored)
        return WebIDL::create_rejected_promise(realm, stream->stored_error());

    // 8. If ! WritableStreamCloseQueuedOrInFlight(stream) is true or state is "closed", return a promise rejected with a TypeError exception indicating that the stream is closing or closed.
    if (writable_stream_close_queued_or_in_flight(*stream) || state == WritableStream::State::Closed) {
        auto exception = JS::TypeError::create(realm, "Cannot write to a writer whose stream is closing or already closed"sv);
        return WebIDL::create_rejected_promise(realm, exception);
    }

    // 9. If state is "erroring", return a promise rejected with stream.[[storedError]].
    if (state == WritableStream::State::Erroring)
        return WebIDL::create_rejected_promise(realm, stream->stored_error());

    // 10. Assert: state is "writable".
    VERIFY(state == WritableStream::State::Writable);

    // 11. Let promise be ! WritableStreamAddWriteRequest(stream).
    auto promise = writable_stream_add_write_request(*stream);

    // 12. Perform ! WritableStreamDefaultControllerWrite(controller, chunk, chunkSize).
    TRY(writable_stream_default_controller_write(*controller, chunk, chunk_size));

    // 13. Return promise.
    return promise;
}

// https://streams.spec.whatwg.org/#set-up-writable-stream-default-controller
WebIDL::ExceptionOr<void> set_up_writable_stream_default_controller(WritableStream& stream, WritableStreamDefaultController& controller, JS::NonnullGCPtr<StartAlgorithm> start_algorithm, JS::NonnullGCPtr<WriteAlgorithm> write_algorithm, JS::NonnullGCPtr<CloseAlgorithm> close_algorithm, JS::NonnullGCPtr<AbortAlgorithm> abort_algorithm, double high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> size_algorithm)
{
    auto& realm = stream.realm();

    // 1. Assert: stream implements WritableStream.

    // 2. Assert: stream.[[controller]] is undefined.
    VERIFY(!stream.controller());

    // 3. Set controller.[[stream]] to stream.
    controller.set_stream(stream);

    // 4. Set stream.[[controller]] to controller.
    stream.set_controller(controller);

    // 5. Perform ! ResetQueue(controller).
    reset_queue(controller);

    // 6. Set controller.[[signal]] to a new AbortSignal.
    controller.set_signal(realm.heap().allocate<DOM::AbortSignal>(realm, realm));

    // 7. Set controller.[[started]] to false.
    controller.set_started(false);

    // 8. Set controller.[[strategySizeAlgorithm]] to sizeAlgorithm.
    controller.set_strategy_size_algorithm(size_algorithm);

    // 9. Set controller.[[strategyHWM]] to highWaterMark.
    controller.set_strategy_hwm(high_water_mark);

    // 10. Set controller.[[writeAlgorithm]] to writeAlgorithm.
    controller.set_write_algorithm(write_algorithm);

    // 11. Set controller.[[closeAlgorithm]] to closeAlgorithm.
    controller.set_close_algorithm(close_algorithm);

    // 12. Set controller.[[abortAlgorithm]] to abortAlgorithm.
    controller.set_abort_algorithm(abort_algorithm);

    // 13. Let backpressure be ! WritableStreamDefaultControllerGetBackpressure(controller).
    auto backpressure = writable_stream_default_controller_get_backpressure(controller);

    // 14. Perform ! WritableStreamUpdateBackpressure(stream, backpressure).
    writable_stream_update_backpressure(stream, backpressure);

    // 15. Let startResult be the result of performing startAlgorithm. (This may throw an exception.)
    auto start_result = TRY(start_algorithm->function()());

    // 16. Let startPromise be a promise resolved with startResult.
    auto start_promise = WebIDL::create_resolved_promise(realm, start_result);

    // 17. Upon fulfillment of startPromise,
    WebIDL::upon_fulfillment(*start_promise, [&](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Assert: stream.[[state]] is "writable" or "erroring".
        auto state = stream.state();
        VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

        // 2. Set controller.[[started]] to true.
        controller.set_started(true);

        // 3. Perform ! WritableStreamDefaultControllerAdvanceQueueIfNeeded(controller).
        TRY(writable_stream_default_controller_advance_queue_if_needed(controller));

        return JS::js_undefined();
    });

    // 18. Upon rejection of startPromise with reason r,
    WebIDL::upon_rejection(*start_promise, [&](JS::Value reason) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Assert: stream.[[state]] is "writable" or "erroring".
        auto state = stream.state();
        VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

        // 2. Set controller.[[started]] to true.
        controller.set_started(true);

        // 3. Perform ! WritableStreamDealWithRejection(stream, r).
        TRY(writable_stream_deal_with_rejection(stream, reason));

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#set-up-writable-stream-default-controller-from-underlying-sink
WebIDL::ExceptionOr<void> set_up_writable_stream_default_controller_from_underlying_sink(WritableStream& stream, JS::Value underlying_sink_value, UnderlyingSink& underlying_sink, double high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> size_algorithm)
{
    auto& realm = stream.realm();

    // 1. Let controller be a new WritableStreamDefaultController.
    auto controller = realm.heap().allocate<WritableStreamDefaultController>(realm, realm);

    // 2. Let startAlgorithm be an algorithm that returns undefined.
    auto start_algorithm = JS::create_heap_function(realm.heap(), []() -> WebIDL::ExceptionOr<JS::Value> { return JS::js_undefined(); });

    // 3. Let writeAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto write_algorithm = JS::create_heap_function(realm.heap(), [&realm](JS::Value) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 4. Let closeAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto close_algorithm = JS::create_heap_function(realm.heap(), [&realm]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 5. Let abortAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto abort_algorithm = JS::create_heap_function(realm.heap(), [&realm](JS::Value) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 6. If underlyingSinkDict["start"] exists, then set startAlgorithm to an algorithm which returns the result of invoking underlyingSinkDict["start"] with argument list « controller » and callback this value underlyingSink.
    if (underlying_sink.start) {
        start_algorithm = JS::create_heap_function(realm.heap(), [controller, underlying_sink_value, callback = underlying_sink.start]() -> WebIDL::ExceptionOr<JS::Value> {
            // Note: callback does not return a promise, so invoke_callback may return an abrupt completion
            return TRY(WebIDL::invoke_callback(*callback, underlying_sink_value, controller)).release_value();
        });
    }

    // 7. If underlyingSinkDict["write"] exists, then set writeAlgorithm to an algorithm which takes an argument chunk and returns the result of invoking underlyingSinkDict["write"] with argument list « chunk, controller » and callback this value underlyingSink.
    if (underlying_sink.write) {
        write_algorithm = JS::create_heap_function(realm.heap(), [&realm, controller, underlying_sink_value, callback = underlying_sink.write](JS::Value chunk) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_sink_value, chunk, controller)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 8. If underlyingSinkDict["close"] exists, then set closeAlgorithm to an algorithm which returns the result of invoking underlyingSinkDict["close"] with argument list «» and callback this value underlyingSink.
    if (underlying_sink.close) {
        close_algorithm = JS::create_heap_function(realm.heap(), [&realm, underlying_sink_value, callback = underlying_sink.close]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_sink_value)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 9. If underlyingSinkDict["abort"] exists, then set abortAlgorithm to an algorithm which takes an argument reason and returns the result of invoking underlyingSinkDict["abort"] with argument list « reason » and callback this value underlyingSink.
    if (underlying_sink.abort) {
        abort_algorithm = JS::create_heap_function(realm.heap(), [&realm, underlying_sink_value, callback = underlying_sink.abort](JS::Value reason) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_sink_value, reason)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 10. Perform ? SetUpWritableStreamDefaultController(stream, controller, startAlgorithm, writeAlgorithm, closeAlgorithm, abortAlgorithm, highWaterMark, sizeAlgorithm).
    TRY(set_up_writable_stream_default_controller(stream, controller, start_algorithm, write_algorithm, close_algorithm, abort_algorithm, high_water_mark, size_algorithm));

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-advance-queue-if-needed
WebIDL::ExceptionOr<void> writable_stream_default_controller_advance_queue_if_needed(WritableStreamDefaultController& controller)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. If controller.[[started]] is false, return.
    if (!controller.started())
        return {};

    // 3. If stream.[[inFlightWriteRequest]] is not undefined, return.
    if (stream->in_flight_write_request())
        return {};

    // 4. Let state be stream.[[state]].
    auto state = stream->state();

    // 5. Assert: state is not "closed" or "errored".
    VERIFY(state != WritableStream::State::Closed && state != WritableStream::State::Errored);

    // 6. If state is "erroring",
    if (state == WritableStream::State::Erroring) {
        // 1. Perform ! WritableStreamFinishErroring(stream).
        // 2. Return.
        return writable_stream_finish_erroring(*stream);
    }

    // 7. If controller.[[queue]] is empty, return.
    if (controller.queue().is_empty())
        return {};

    // 8. Let value be ! PeekQueueValue(controller).
    auto value = peek_queue_value(controller);

    // 9. If value is the close sentinel, perform ! WritableStreamDefaultControllerProcessClose(controller).
    if (is_close_sentinel(value)) {
        TRY(writable_stream_default_controller_process_close(controller));
    }
    // 10. Otherwise, perform ! WritableStreamDefaultControllerProcessWrite(controller, value).
    else {
        TRY(writable_stream_default_controller_process_write(controller, value));
    }

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-clear-algorithms
void writable_stream_default_controller_clear_algorithms(WritableStreamDefaultController& controller)
{
    // 1. Set controller.[[writeAlgorithm]] to undefined.
    controller.set_write_algorithm({});

    // 2. Set controller.[[closeAlgorithm]] to undefined.
    controller.set_close_algorithm({});

    // 3. Set controller.[[abortAlgorithm]] to undefined.
    controller.set_abort_algorithm({});

    // 4. Set controller.[[strategySizeAlgorithm]] to undefined.
    controller.set_strategy_size_algorithm({});
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-close
WebIDL::ExceptionOr<void> writable_stream_default_controller_close(WritableStreamDefaultController& controller)
{
    // 1. Perform ! EnqueueValueWithSize(controller, close sentinel, 0).
    TRY(enqueue_value_with_size(controller, create_close_sentinel(), JS::Value(0.0)));

    // 2. Perform ! WritableStreamDefaultControllerAdvanceQueueIfNeeded(controller).
    TRY(writable_stream_default_controller_advance_queue_if_needed(controller));

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-error
WebIDL::ExceptionOr<void> writable_stream_default_controller_error(WritableStreamDefaultController& controller, JS::Value error)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. Assert: stream.[[state]] is "writable".
    VERIFY(stream->state() == WritableStream::State::Writable);

    // 3. Perform ! WritableStreamDefaultControllerClearAlgorithms(controller).
    writable_stream_default_controller_clear_algorithms(controller);

    // 4. Perform ! WritableStreamStartErroring(stream, error).
    return writable_stream_start_erroring(stream, error);
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-error-if-needed
WebIDL::ExceptionOr<void> writable_stream_default_controller_error_if_needed(WritableStreamDefaultController& controller, JS::Value error)
{
    // 1. If controller.[[stream]].[[state]] is "writable", perform ! WritableStreamDefaultControllerError(controller, error).
    if (controller.stream()->state() == WritableStream::State::Writable)
        TRY(writable_stream_default_controller_error(controller, error));

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-get-backpressure
bool writable_stream_default_controller_get_backpressure(WritableStreamDefaultController const& controller)
{
    // 1. Let desiredSize be ! WritableStreamDefaultControllerGetDesiredSize(controller).
    auto desired_size = writable_stream_default_controller_get_desired_size(controller);

    // 2. Return true if desiredSize ≤ 0, or false otherwise.
    return desired_size <= 0.0;
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-get-chunk-size
WebIDL::ExceptionOr<JS::Value> writable_stream_default_controller_get_chunk_size(WritableStreamDefaultController& controller, JS::Value chunk)
{
    // 1. Let returnValue be the result of performing controller.[[strategySizeAlgorithm]], passing in chunk, and interpreting the result as a completion record.
    auto return_value = controller.strategy_size_algorithm()->function()(chunk);

    // 2. If returnValue is an abrupt completion,
    if (return_value.is_abrupt()) {
        // 1. Perform ! WritableStreamDefaultControllerErrorIfNeeded(controller, returnValue.[[Value]]).
        TRY(writable_stream_default_controller_error_if_needed(controller, *return_value.release_value()));

        // 2. Return 1.
        return 1.0;
    }

    // 3. Return returnValue.[[Value]].
    return *return_value.release_value();
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-get-desired-size
double writable_stream_default_controller_get_desired_size(WritableStreamDefaultController const& controller)
{
    // 1. Return controller.[[strategyHWM]] − controller.[[queueTotalSize]].
    return controller.strategy_hwm() - controller.queue_total_size();
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-process-close
WebIDL::ExceptionOr<void> writable_stream_default_controller_process_close(WritableStreamDefaultController& controller)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. Perform ! WritableStreamMarkCloseRequestInFlight(stream).
    writable_stream_mark_close_request_in_flight(*stream);

    // 3. Perform ! DequeueValue(controller).
    dequeue_value(controller);

    // 4. Assert: controller.[[queue]] is empty.
    VERIFY(controller.queue().is_empty());

    // 5. Let sinkClosePromise be the result of performing controller.[[closeAlgorithm]].
    auto sink_close_promise = TRY(controller.close_algorithm()->function()());

    // 6. Perform ! WritableStreamDefaultControllerClearAlgorithms(controller).
    writable_stream_default_controller_clear_algorithms(controller);

    // 7. Upon fulfillment of sinkClosePromise,
    WebIDL::upon_fulfillment(*sink_close_promise, [&, stream = stream](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! WritableStreamFinishInFlightClose(stream).
        writable_stream_finish_in_flight_close(*stream);

        return JS::js_undefined();
    });

    // 8. Upon rejection of sinkClosePromise with reason reason,
    WebIDL::upon_rejection(*sink_close_promise, [&, stream = stream](auto const& reason) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! WritableStreamFinishInFlightCloseWithError(stream, reason).
        TRY(writable_stream_finish_in_flight_close_with_error(*stream, reason));

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-process-write
WebIDL::ExceptionOr<void> writable_stream_default_controller_process_write(WritableStreamDefaultController& controller, JS::Value chunk)
{
    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. Perform ! WritableStreamMarkFirstWriteRequestInFlight(stream).
    writable_stream_mark_first_write_request_in_flight(*stream);

    // 3. Let sinkWritePromise be the result of performing controller.[[writeAlgorithm]], passing in chunk.
    auto sink_write_promise = TRY(controller.write_algorithm()->function()(chunk));

    // 4. Upon fulfillment of sinkWritePromise,
    WebIDL::upon_fulfillment(*sink_write_promise, [&, stream = stream](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. Perform ! WritableStreamFinishInFlightWrite(stream).
        writable_stream_finish_in_flight_write(*stream);

        // 2. Let state be stream.[[state]].
        auto state = stream->state();

        // 3. Assert: state is "writable" or "erroring".
        VERIFY(state == WritableStream::State::Writable || state == WritableStream::State::Erroring);

        // 4. Perform ! DequeueValue(controller).
        dequeue_value(controller);

        // 5. If ! WritableStreamCloseQueuedOrInFlight(stream) is false and state is "writable",
        if (!writable_stream_close_queued_or_in_flight(*stream) && state == WritableStream::State::Writable) {
            // 1. Let backpressure be ! WritableStreamDefaultControllerGetBackpressure(controller).
            auto backpressure = writable_stream_default_controller_get_backpressure(controller);

            // 2. Perform ! WritableStreamUpdateBackpressure(stream, backpressure).
            writable_stream_update_backpressure(*stream, backpressure);
        }

        // 6 .Perform ! WritableStreamDefaultControllerAdvanceQueueIfNeeded(controller).
        TRY(writable_stream_default_controller_advance_queue_if_needed(controller));

        return JS::js_undefined();
    });

    // 5. Upon rejection of sinkWritePromise with reason,
    WebIDL::upon_rejection(*sink_write_promise, [&, stream = stream](auto const& reason) -> WebIDL::ExceptionOr<JS::Value> {
        // 1. If stream.[[state]] is "writable", perform ! WritableStreamDefaultControllerClearAlgorithms(controller).
        if (stream->state() == WritableStream::State::Writable)
            writable_stream_default_controller_clear_algorithms(controller);

        // 2. Perform ! WritableStreamFinishInFlightWriteWithError(stream, reason).
        TRY(writable_stream_finish_in_flight_write_with_error(*stream, reason));

        return JS::js_undefined();
    });

    return {};
}

// https://streams.spec.whatwg.org/#writable-stream-default-controller-write
WebIDL::ExceptionOr<void> writable_stream_default_controller_write(WritableStreamDefaultController& controller, JS::Value chunk, JS::Value chunk_size)
{
    auto& vm = controller.vm();

    // 1. Let enqueueResult be EnqueueValueWithSize(controller, chunk, chunkSize).
    auto enqueue_result = enqueue_value_with_size(controller, chunk, chunk_size);

    // 2. If enqueueResult is an abrupt completion,
    if (enqueue_result.is_exception()) {
        auto throw_completion = Bindings::throw_dom_exception_if_needed(vm, [&] { return enqueue_result; }).throw_completion();

        // 1. Perform ! WritableStreamDefaultControllerErrorIfNeeded(controller, enqueueResult.[[Value]]).
        TRY(writable_stream_default_controller_error_if_needed(controller, *throw_completion.release_value()));

        // 2. Return.
        return {};
    }

    // 3. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 4. If ! WritableStreamCloseQueuedOrInFlight(stream) is false and stream.[[state]] is "writable",
    if (!writable_stream_close_queued_or_in_flight(*stream) && stream->state() == WritableStream::State::Writable) {
        // 1. Let backpressure be ! WritableStreamDefaultControllerGetBackpressure(controller).
        auto backpressure = writable_stream_default_controller_get_backpressure(controller);

        // 2. Perform ! WritableStreamUpdateBackpressure(stream, backpressure).
        writable_stream_update_backpressure(*stream, backpressure);
    }

    // 5. Perform ! WritableStreamDefaultControllerAdvanceQueueIfNeeded(controller).
    TRY(writable_stream_default_controller_advance_queue_if_needed(controller));

    return {};
}

// https://streams.spec.whatwg.org/#initialize-transform-stream
WebIDL::ExceptionOr<void> initialize_transform_stream(TransformStream& stream, JS::NonnullGCPtr<JS::PromiseCapability> start_promise, double writable_high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> writable_size_algorithm, double readable_high_water_mark, JS::NonnullGCPtr<SizeAlgorithm> readable_size_algorithm)
{
    auto& realm = stream.realm();

    // 1. Let startAlgorithm be an algorithm that returns startPromise.
    auto writable_start_algorithm = JS::create_heap_function(realm.heap(), [start_promise]() -> WebIDL::ExceptionOr<JS::Value> {
        return start_promise->promise();
    });

    auto readable_start_algorithm = JS::create_heap_function(realm.heap(), [start_promise]() -> WebIDL::ExceptionOr<JS::Value> {
        return start_promise->promise();
    });

    // 2. Let writeAlgorithm be the following steps, taking a chunk argument:
    auto write_algorithm = JS::create_heap_function(realm.heap(), [&stream](JS::Value chunk) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Return ! TransformStreamDefaultSinkWriteAlgorithm(stream, chunk).
        return transform_stream_default_sink_write_algorithm(stream, chunk);
    });

    // 3. Let abortAlgorithm be the following steps, taking a reason argument:
    auto abort_algorithm = JS::create_heap_function(realm.heap(), [&stream](JS::Value reason) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Return ! TransformStreamDefaultSinkAbortAlgorithm(stream, reason).
        return transform_stream_default_sink_abort_algorithm(stream, reason);
    });

    // 4. Let closeAlgorithm be the following steps:
    auto close_algorithm = JS::create_heap_function(realm.heap(), [&stream]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Return ! TransformStreamDefaultSinkCloseAlgorithm(stream).
        return transform_stream_default_sink_close_algorithm(stream);
    });

    // 5. Set stream.[[writable]] to ! CreateWritableStream(startAlgorithm, writeAlgorithm, closeAlgorithm, abortAlgorithm, writableHighWaterMark, writableSizeAlgorithm).
    stream.set_writable(TRY(create_writable_stream(realm, writable_start_algorithm, write_algorithm, close_algorithm, abort_algorithm, writable_high_water_mark, writable_size_algorithm)));

    // 6. Let pullAlgorithm be the following steps:
    auto pull_algorithm = JS::create_heap_function(realm.heap(), [&stream]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Return ! TransformStreamDefaultSourcePullAlgorithm(stream).
        return transform_stream_default_source_pull_algorithm(stream);
    });

    // 7. Let cancelAlgorithm be the following steps, taking a reason argument:
    auto cancel_algorithm = JS::create_heap_function(realm.heap(), [&stream, &realm](JS::Value reason) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Perform ! TransformStreamErrorWritableAndUnblockWrite(stream, reason).
        TRY(transform_stream_error_writable_and_unblock_write(stream, reason));

        // 2. Return a promise resolved with undefined.
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 8. Set stream.[[readable]] to ! CreateReadableStream(startAlgorithm, pullAlgorithm, cancelAlgorithm, readableHighWaterMark, readableSizeAlgorithm).
    stream.set_readable(TRY(create_readable_stream(realm, readable_start_algorithm, pull_algorithm, cancel_algorithm, readable_high_water_mark, readable_size_algorithm)));

    // 9. Set stream.[[backpressure]] and stream.[[backpressureChangePromise]] to undefined.
    stream.set_backpressure({});
    stream.set_backpressure_change_promise({});

    // 10. Perform ! TransformStreamSetBackpressure(stream, true).
    TRY(transform_stream_set_backpressure(stream, true));

    // 11. Set stream.[[controller]] to undefined.
    stream.set_controller({});

    return {};
}

// https://streams.spec.whatwg.org/#set-up-transform-stream-default-controller
void set_up_transform_stream_default_controller(TransformStream& stream, TransformStreamDefaultController& controller, JS::NonnullGCPtr<TransformAlgorithm> transform_algorithm, JS::NonnullGCPtr<FlushAlgorithm> flush_algorithm)
{
    // 1. Assert: stream implements TransformStream.
    // 2. Assert: stream.[[controller]] is undefined.
    VERIFY(!stream.controller());

    // 3. Set controller.[[stream]] to stream.
    controller.set_stream(stream);

    // 4. Set stream.[[controller]] to controller.
    stream.set_controller(controller);

    // 5. Set controller.[[transformAlgorithm]] to transformAlgorithm.
    controller.set_transform_algorithm(transform_algorithm);

    // 6. Set controller.[[flushAlgorithm]] to flushAlgorithm.
    controller.set_flush_algorithm(flush_algorithm);
}

// https://streams.spec.whatwg.org/#set-up-transform-stream-default-controller-from-transformer
WebIDL::ExceptionOr<void> set_up_transform_stream_default_controller_from_transformer(TransformStream& stream, JS::Value transformer, Transformer& transformer_dict)
{
    auto& realm = stream.realm();
    auto& vm = realm.vm();

    // 1. Let controller be a new TransformStreamDefaultController.
    auto controller = realm.heap().allocate<TransformStreamDefaultController>(realm, realm);

    // 2. Let transformAlgorithm be the following steps, taking a chunk argument:
    auto transform_algorithm = JS::create_heap_function(realm.heap(), [controller, &realm, &vm](JS::Value chunk) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        // 1. Let result be TransformStreamDefaultControllerEnqueue(controller, chunk).
        auto result = transform_stream_default_controller_enqueue(*controller, chunk);

        // 2. If result is an abrupt completion, return a promise rejected with result.[[Value]].
        if (result.is_error()) {
            auto throw_completion = Bindings::dom_exception_to_throw_completion(vm, result.exception());
            return WebIDL::create_rejected_promise(realm, *throw_completion.release_value());
        }

        // 3. Otherwise, return a promise resolved with undefined.
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 3. Let flushAlgorithm be an algorithm which returns a promise resolved with undefined.
    auto flush_algorithm = JS::create_heap_function(realm.heap(), [&realm]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 4. If transformerDict["transform"] exists, set transformAlgorithm to an algorithm which takes an argument chunk
    //    and returns the result of invoking transformerDict["transform"] with argument list « chunk, controller » and
    //    callback this value transformer.
    if (transformer_dict.transform) {
        transform_algorithm = JS::create_heap_function(realm.heap(), [controller, &realm, transformer, callback = transformer_dict.transform](JS::Value chunk) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback does not return a promise, so invoke_callback may return an abrupt completion
            auto result = WebIDL::invoke_callback(*callback, transformer, chunk, controller);
            if (result.is_error())
                return WebIDL::create_rejected_promise(realm, *result.release_value());

            return WebIDL::create_resolved_promise(realm, *result.release_value());
        });
    }
    // 5. If transformerDict["flush"] exists, set flushAlgorithm to an algorithm which returns the result of invoking
    //    transformerDict["flush"] with argument list « controller » and callback this value transformer.
    if (transformer_dict.flush) {
        flush_algorithm = JS::create_heap_function(realm.heap(), [&realm, transformer, callback = transformer_dict.flush, controller]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback does not return a promise, so invoke_callback may return an abrupt completion
            auto result = WebIDL::invoke_callback(*callback, transformer, controller);
            if (result.is_error()) {
                return WebIDL::create_rejected_promise(realm, *result.release_value());
            }
            return WebIDL::create_resolved_promise(realm, *result.release_value());
        });
    }

    // 6. Perform ! SetUpTransformStreamDefaultController(stream, controller, transformAlgorithm, flushAlgorithm).
    set_up_transform_stream_default_controller(stream, *controller, transform_algorithm, flush_algorithm);

    return {};
}

// https://streams.spec.whatwg.org/#transform-stream-default-controller-clear-algorithms
void transform_stream_default_controller_clear_algorithms(TransformStreamDefaultController& controller)
{
    // NOTE: This is observable using weak references. See tc39/proposal-weakrefs#31 for more detail.
    // 1. Set controller.[[transformAlgorithm]] to undefined.
    controller.set_transform_algorithm({});

    // 2. Set controller.[[flushAlgorithm]] to undefined.
    controller.set_flush_algorithm({});
}

// https://streams.spec.whatwg.org/#transform-stream-default-controller-enqueue
WebIDL::ExceptionOr<void> transform_stream_default_controller_enqueue(TransformStreamDefaultController& controller, JS::Value chunk)
{
    auto& vm = controller.vm();

    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. Let readableController be stream.[[readable]].[[controller]].
    VERIFY(stream->readable()->controller().has_value() && stream->readable()->controller()->has<JS::NonnullGCPtr<ReadableStreamDefaultController>>());
    auto& readable_controller = stream->readable()->controller()->get<JS::NonnullGCPtr<ReadableStreamDefaultController>>();

    // 3. If ! ReadableStreamDefaultControllerCanCloseOrEnqueue(readableController) is false, throw a TypeError exception.
    if (!readable_stream_default_controller_can_close_or_enqueue(readable_controller))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "ReadableController is either closed or not readable."sv };

    // 4. Let enqueueResult be ReadableStreamDefaultControllerEnqueue(readableController, chunk).
    auto enqueue_result = readable_stream_default_controller_enqueue(readable_controller, chunk);

    // 5. If enqueueResult is an abrupt completion,
    if (enqueue_result.is_error()) {
        auto throw_completion = Bindings::dom_exception_to_throw_completion(vm, enqueue_result.exception());

        // 1. Perform ! TransformStreamErrorWritableAndUnblockWrite(stream, enqueueResult.[[Value]]).
        TRY(transform_stream_error_writable_and_unblock_write(*stream, throw_completion.value().value()));

        // 2. Throw stream.[[readable]].[[storedError]].
        return JS::throw_completion(stream->readable()->stored_error());
    }

    // 6. Let backpressure be ! ReadableStreamDefaultControllerHasBackpressure(readableController).
    auto backpressure = readable_stream_default_controller_has_backpressure(readable_controller);

    // 7. If backpressure is not stream.[[backpressure]],
    if (backpressure != stream->backpressure()) {
        // 1. Assert: backpressure is true.
        VERIFY(backpressure);

        // 2. Perform ! TransformStreamSetBackpressure(stream, true).
        TRY(transform_stream_set_backpressure(*stream, true));
    }

    return {};
}

// https://streams.spec.whatwg.org/#transform-stream-default-controller-error
WebIDL::ExceptionOr<void> transform_stream_default_controller_error(TransformStreamDefaultController& controller, JS::Value error)
{
    // 1. Perform ! TransformStreamError(controller.[[stream]], e).
    TRY(transform_stream_error(*controller.stream(), error));

    return {};
}

// https://streams.spec.whatwg.org/#transform-stream-default-controller-terminate
WebIDL::ExceptionOr<void> transform_stream_default_controller_terminate(TransformStreamDefaultController& controller)
{
    auto& realm = controller.realm();

    // 1. Let stream be controller.[[stream]].
    auto stream = controller.stream();

    // 2. Let readableController be stream.[[readable]].[[controller]].
    VERIFY(stream->readable()->controller().has_value() && stream->readable()->controller()->has<JS::NonnullGCPtr<ReadableStreamDefaultController>>());
    auto readable_controller = stream->readable()->controller()->get<JS::NonnullGCPtr<ReadableStreamDefaultController>>();

    // 3. Perform ! ReadableStreamDefaultControllerClose(readableController).
    readable_stream_default_controller_close(readable_controller);

    // 4. Let error be a TypeError exception indicating that the stream has been terminated.
    auto error = JS::TypeError::create(realm, "Stream has been terminated."sv);

    // 5. Perform ! TransformStreamErrorWritableAndUnblockWrite(stream, error).
    TRY(transform_stream_error_writable_and_unblock_write(*stream, error));

    return {};
}

// https://streams.spec.whatwg.org/#transform-stream-default-controller-perform-transform
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_controller_perform_transform(TransformStreamDefaultController& controller, JS::Value chunk)
{
    auto& realm = controller.realm();

    // 1. Let transformPromise be the result of performing controller.[[transformAlgorithm]], passing chunk.
    auto transform_promise = TRY(controller.transform_algorithm()->function()(chunk));

    // 2. Return the result of reacting to transformPromise with the following rejection steps given the argument r:
    auto react_result = WebIDL::react_to_promise(*transform_promise,
        {},
        [&](auto const& reason) -> WebIDL::ExceptionOr<JS::Value> {
            // 1. Perform ! TransformStreamError(controller.[[stream]], r).
            TRY(transform_stream_error(*controller.stream(), reason));

            // 2. Throw r.
            return JS::throw_completion(reason);
        });

    return WebIDL::create_resolved_promise(realm, react_result);
}

// https://streams.spec.whatwg.org/#transform-stream-default-sink-abort-algorithm
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_sink_abort_algorithm(TransformStream& stream, JS::Value reason)
{
    auto& realm = stream.realm();

    // 1. Perform ! TransformStreamError(stream, reason).
    TRY(transform_stream_error(stream, reason));

    // 2. Return a promise resolved with undefined.
    return WebIDL::create_resolved_promise(realm, JS::js_undefined());
}

// https://streams.spec.whatwg.org/#transform-stream-default-sink-close-algorithm
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_sink_close_algorithm(TransformStream& stream)
{
    auto& realm = stream.realm();

    // 1. Let readable be stream.[[readable]].
    auto readable = stream.readable();

    // 2. Let controller be stream.[[controller]].
    auto controller = stream.controller();

    // 3. Let flushPromise be the result of performing controller.[[flushAlgorithm]].
    auto flush_promise = TRY(controller->flush_algorithm()->function()());

    // 4. Perform ! TransformStreamDefaultControllerClearAlgorithms(controller).
    transform_stream_default_controller_clear_algorithms(*controller);

    // 5. Return the result of reacting to flushPromise:
    auto react_result = WebIDL::react_to_promise(
        *flush_promise,
        // 1. If flushPromise was fulfilled, then:
        [readable](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
            // 1. If readable.[[state]] is "errored", throw readable.[[storedError]].
            if (readable->state() == ReadableStream::State::Errored)
                return JS::throw_completion(readable->stored_error());

            VERIFY(readable->controller().has_value() && readable->controller()->has<JS::NonnullGCPtr<ReadableStreamDefaultController>>());
            // 2. Perform ! ReadableStreamDefaultControllerClose(readable.[[controller]]).
            readable_stream_default_controller_close(readable->controller().value().get<JS::NonnullGCPtr<ReadableStreamDefaultController>>());

            return JS::js_undefined();
        },
        // 2. If flushPromise was rejected with reason r, then:
        [&stream, readable](auto const& reason) -> WebIDL::ExceptionOr<JS::Value> {
            // 1. Perform ! TransformStreamError(stream, r).
            TRY(transform_stream_error(stream, reason));

            // 2. Throw readable.[[storedError]].
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, readable->stored_error().as_string().utf8_string() };
        });

    return WebIDL::create_resolved_promise(realm, react_result);
}

// https://streams.spec.whatwg.org/#transform-stream-default-sink-write-algorithm
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_sink_write_algorithm(TransformStream& stream, JS::Value chunk)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[writable]].[[state]] is "writable".
    VERIFY(stream.writable()->state() == WritableStream::State::Writable);

    // 2. Let controller be stream.[[controller]].
    auto controller = stream.controller();

    // 3. If stream.[[backpressure]] is true,
    if (stream.backpressure().has_value() && *stream.backpressure()) {
        // 1. Let backpressureChangePromise be stream.[[backpressureChangePromise]].
        auto backpressure_change_promise = stream.backpressure_change_promise();

        // 2. Assert: backpressureChangePromise is not undefined.
        VERIFY(backpressure_change_promise);

        // 3. Return the result of reacting to backpressureChangePromise with the following fulfillment steps:
        auto react_result = WebIDL::react_to_promise(*backpressure_change_promise,
            [&stream, controller, chunk](auto const&) -> WebIDL::ExceptionOr<JS::Value> {
                // 1. Let writable be stream.[[writable]].
                auto writable = stream.writable();

                // 2. Let state be writable.[[state]].
                auto state = writable->state();

                // 3. If state is "erroring", throw writable.[[storedError]].
                if (state == WritableStream::State::Erroring)
                    return JS::throw_completion(writable->stored_error());

                // 4. Assert: state is "writable".
                VERIFY(state == WritableStream::State::Writable);

                // 5. Return ! TransformStreamDefaultControllerPerformTransform(controller, chunk).
                return TRY(transform_stream_default_controller_perform_transform(*controller, chunk))->promise();
            },
            {});

        return WebIDL::create_resolved_promise(realm, react_result);
    }

    // 4. Return ! TransformStreamDefaultControllerPerformTransform(controller, chunk).
    return transform_stream_default_controller_perform_transform(*controller, chunk);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_source_pull_algorithm(TransformStream& stream)
{
    // 1. Assert: stream.[[backpressure]] is true.
    VERIFY(stream.backpressure().has_value() && *stream.backpressure());

    // 2. Assert: stream.[[backpressureChangePromise]] is not undefined.
    VERIFY(stream.backpressure_change_promise());

    // 3. Perform ! TransformStreamSetBackpressure(stream, false).
    TRY(transform_stream_set_backpressure(stream, false));

    // 4. Return stream.[[backpressureChangePromise]].
    return JS::NonnullGCPtr { *stream.backpressure_change_promise() };
}

// https://streams.spec.whatwg.org/#transform-stream-error
WebIDL::ExceptionOr<void> transform_stream_error(TransformStream& stream, JS::Value error)
{
    VERIFY(stream.readable()->controller().has_value() && stream.readable()->controller()->has<JS::NonnullGCPtr<ReadableStreamDefaultController>>());

    auto readable_controller = stream.readable()->controller()->get<JS::NonnullGCPtr<ReadableStreamDefaultController>>();

    // 1. Perform ! ReadableStreamDefaultControllerError(stream.[[readable]].[[controller]], e).
    readable_stream_default_controller_error(*readable_controller, error);

    // 2. Perform ! TransformStreamErrorWritableAndUnblockWrite(stream, e).
    TRY(transform_stream_error_writable_and_unblock_write(stream, error));

    return {};
}

// https://streams.spec.whatwg.org/#transform-stream-error-writable-and-unblock-write
WebIDL::ExceptionOr<void> transform_stream_error_writable_and_unblock_write(TransformStream& stream, JS::Value error)
{
    // 1. Perform ! TransformStreamDefaultControllerClearAlgorithms(stream.[[controller]]).
    transform_stream_default_controller_clear_algorithms(*stream.controller());

    // 2. Perform ! WritableStreamDefaultControllerErrorIfNeeded(stream.[[writable]].[[controller]], e).
    TRY(writable_stream_default_controller_error_if_needed(*stream.writable()->controller(), error));

    // 3. If stream.[[backpressure]] is true, perform ! TransformStreamSetBackpressure(stream, false).
    if (stream.backpressure().has_value() && *stream.backpressure())
        TRY(transform_stream_set_backpressure(stream, false));

    return {};
}

//  https://streams.spec.whatwg.org/#transform-stream-set-backpressure
WebIDL::ExceptionOr<void> transform_stream_set_backpressure(TransformStream& stream, bool backpressure)
{
    auto& realm = stream.realm();

    // 1. Assert: stream.[[backpressure]] is not backpressure.
    VERIFY(stream.backpressure() != backpressure);

    // 2. If stream.[[backpressureChangePromise]] is not undefined, resolve stream.[[backpressureChangePromise]] with undefined.
    if (stream.backpressure_change_promise())
        WebIDL::resolve_promise(realm, *stream.backpressure_change_promise(), JS::js_undefined());

    // 3. Set stream.[[backpressureChangePromise]] to a new promise.
    stream.set_backpressure_change_promise(WebIDL::create_promise(realm));

    // 4. Set stream.[[backpressure]] to backpressure.
    stream.set_backpressure(backpressure);

    return {};
}

// https://streams.spec.whatwg.org/#is-non-negative-number
bool is_non_negative_number(JS::Value value)
{
    // 1. If Type(v) is not Number, return false.
    if (!value.is_number())
        return false;

    // 2. If v is NaN, return false.
    if (value.is_nan())
        return false;

    // 3. If v < 0, return false.
    if (value.as_double() < 0.0)
        return false;

    // 4. Return true.
    return true;
}

// https://streams.spec.whatwg.org/#can-transfer-array-buffer
bool can_transfer_array_buffer(JS::ArrayBuffer const& array_buffer)
{
    // 1. Assert: Type(O) is Object.
    // 2. Assert: O has an [[ArrayBufferData]] internal slot.

    // 3. If ! IsDetachedBuffer(O) is true, return false.
    if (array_buffer.is_detached())
        return false;

    // 4. If SameValue(O.[[ArrayBufferDetachKey]], undefined) is false, return false.
    if (!JS::same_value(array_buffer.detach_key(), JS::js_undefined()))
        return false;

    // 5. Return true.
    return true;
}

// https://streams.spec.whatwg.org/#close-sentinel
// Non-standard function that implements the "close sentinel" value.
JS::Value create_close_sentinel()
{
    // The close sentinel is a unique value enqueued into [[queue]], in lieu of a chunk, to signal that the stream is closed. It is only used internally, and is never exposed to web developers.
    // Note: We use the empty Value to signal this as, similarly to the note above, the empty value is not exposed to nor creatable by web developers.
    return {};
}

// https://streams.spec.whatwg.org/#close-sentinel
// Non-standard function that implements the "If value is a close sentinel" check.
bool is_close_sentinel(JS::Value value)
{
    return value.is_empty();
}

// Non-standard function to aid in converting a user-provided function into a WebIDL::Callback. This is essentially
// what the Bindings generator would do at compile time, but at runtime instead.
JS::ThrowCompletionOr<JS::Handle<WebIDL::CallbackType>> property_to_callback(JS::VM& vm, JS::Value value, JS::PropertyKey const& property_key, WebIDL::OperationReturnsPromise operation_returns_promise)
{
    auto property = TRY(value.get(vm, property_key));

    if (property.is_undefined())
        return JS::Handle<WebIDL::CallbackType> {};

    if (!property.is_function())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAFunction, property.to_string_without_side_effects());

    return vm.heap().allocate_without_realm<WebIDL::CallbackType>(property.as_object(), HTML::incumbent_settings_object(), operation_returns_promise);
}

// https://streams.spec.whatwg.org/#set-up-readable-byte-stream-controller-from-underlying-source
WebIDL::ExceptionOr<void> set_up_readable_byte_stream_controller_from_underlying_source(ReadableStream& stream, JS::Value underlying_source, UnderlyingSource const& underlying_source_dict, double high_water_mark)
{
    auto& realm = stream.realm();

    // 1. Let controller be a new ReadableByteStreamController.
    auto controller = stream.heap().allocate<ReadableByteStreamController>(realm, realm);

    // 2. Let startAlgorithm be an algorithm that returns undefined.
    auto start_algorithm = JS::create_heap_function(realm.heap(), []() -> WebIDL::ExceptionOr<JS::Value> { return JS::js_undefined(); });

    // 3. Let pullAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto pull_algorithm = JS::create_heap_function(realm.heap(), [&realm]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 4. Let cancelAlgorithm be an algorithm that returns a promise resolved with undefined.
    auto cancel_algorithm = JS::create_heap_function(realm.heap(), [&realm](JS::Value) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
        return WebIDL::create_resolved_promise(realm, JS::js_undefined());
    });

    // 5. If underlyingSourceDict["start"] exists, then set startAlgorithm to an algorithm which returns the result of invoking underlyingSourceDict["start"] with argument list « controller » and callback this value underlyingSource.
    if (underlying_source_dict.start) {
        start_algorithm = JS::create_heap_function(realm.heap(), [controller, underlying_source, callback = underlying_source_dict.start]() -> WebIDL::ExceptionOr<JS::Value> {
            // Note: callback does not return a promise, so invoke_callback may return an abrupt completion
            return TRY(WebIDL::invoke_callback(*callback, underlying_source, controller)).release_value();
        });
    }

    // 6. If underlyingSourceDict["pull"] exists, then set pullAlgorithm to an algorithm which returns the result of invoking underlyingSourceDict["pull"] with argument list « controller » and callback this value underlyingSource.
    if (underlying_source_dict.pull) {
        pull_algorithm = JS::create_heap_function(realm.heap(), [&realm, controller, underlying_source, callback = underlying_source_dict.pull]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_source, controller)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 7. If underlyingSourceDict["cancel"] exists, then set cancelAlgorithm to an algorithm which takes an argument reason and returns the result of invoking underlyingSourceDict["cancel"] with argument list « reason » and callback this value underlyingSource.
    if (underlying_source_dict.cancel) {
        cancel_algorithm = JS::create_heap_function(realm.heap(), [&realm, underlying_source, callback = underlying_source_dict.cancel](JS::Value reason) -> WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> {
            // Note: callback return a promise, so invoke_callback will never return an abrupt completion
            auto result = MUST_OR_THROW_OOM(WebIDL::invoke_callback(*callback, underlying_source, reason)).release_value();
            return WebIDL::create_resolved_promise(realm, result);
        });
    }

    // 8. Let autoAllocateChunkSize be underlyingSourceDict["autoAllocateChunkSize"], if it exists, or undefined otherwise.
    auto auto_allocate_chunk_size = underlying_source_dict.auto_allocate_chunk_size.has_value()
        ? JS::Value(underlying_source_dict.auto_allocate_chunk_size.value())
        : JS::js_undefined();

    // 9. If autoAllocateChunkSize is 0, then throw a TypeError exception.
    if (auto_allocate_chunk_size.is_integral_number() && auto_allocate_chunk_size.as_double() == 0)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot use an auto allocate chunk size of 0"sv };

    // 10. Perform ? SetUpReadableByteStreamController(stream, controller, startAlgorithm, pullAlgorithm, cancelAlgorithm, highWaterMark, autoAllocateChunkSize).
    return set_up_readable_byte_stream_controller(stream, controller, start_algorithm, pull_algorithm, cancel_algorithm, high_water_mark, auto_allocate_chunk_size);
}

}
