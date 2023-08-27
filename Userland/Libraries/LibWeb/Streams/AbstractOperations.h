/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/WebIDL/CallbackType.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

using SizeAlgorithm = JS::SafeFunction<JS::Completion(JS::Value)>;
using PullAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>()>;
using CancelAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>(JS::Value)>;
using StartAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::Value>()>;
using AbortAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>(JS::Value)>;
using CloseAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>()>;
using WriteAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>(JS::Value)>;
using FlushAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>()>;
using TransformAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>>(JS::Value)>;

WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamDefaultReader>> acquire_readable_stream_default_reader(ReadableStream&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamBYOBReader>> acquire_readable_stream_byob_reader(ReadableStream&);
bool is_readable_stream_locked(ReadableStream const&);

SizeAlgorithm extract_size_algorithm(QueuingStrategy const&);
WebIDL::ExceptionOr<double> extract_high_water_mark(QueuingStrategy const&, double default_hwm);

void readable_stream_close(ReadableStream&);
void readable_stream_error(ReadableStream&, JS::Value error);
void readable_stream_add_read_request(ReadableStream&, ReadRequest&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> readable_stream_cancel(ReadableStream&, JS::Value reason);
void readable_stream_fulfill_read_request(ReadableStream&, JS::Value chunk, bool done);
size_t readable_stream_get_num_read_into_requests(ReadableStream const&);
size_t readable_stream_get_num_read_requests(ReadableStream const&);
bool readable_stream_has_byob_reader(ReadableStream const&);
bool readable_stream_has_default_reader(ReadableStream const&);

WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> readable_stream_reader_generic_cancel(ReadableStreamGenericReaderMixin&, JS::Value reason);
void readable_stream_reader_generic_initialize(ReadableStreamReader, ReadableStream&);
WebIDL::ExceptionOr<void> readable_stream_reader_generic_release(ReadableStreamGenericReaderMixin&);

void readable_stream_default_reader_error_read_requests(ReadableStreamDefaultReader&, JS::Value error);
void readable_stream_byob_reader_error_read_into_requests(ReadableStreamBYOBReader&, JS::Value error);

WebIDL::ExceptionOr<void> readable_stream_default_reader_read(ReadableStreamDefaultReader&, ReadRequest&);
WebIDL::ExceptionOr<void> readable_stream_default_reader_release(ReadableStreamDefaultReader&);
void readable_stream_byob_reader_release(ReadableStreamBYOBReader&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_reader(ReadableStreamDefaultReader&, ReadableStream&);
WebIDL::ExceptionOr<void> set_up_readable_stream_byob_reader(ReadableStreamBYOBReader&, ReadableStream&);
void readable_stream_default_controller_close(ReadableStreamDefaultController&);
bool readable_stream_default_controller_has_backpressure(ReadableStreamDefaultController&);
WebIDL::ExceptionOr<void> readable_stream_default_controller_enqueue(ReadableStreamDefaultController&, JS::Value chunk);
WebIDL::ExceptionOr<void> readable_stream_default_controller_can_pull_if_needed(ReadableStreamDefaultController&);
bool readable_stream_default_controller_should_call_pull(ReadableStreamDefaultController&);
void readable_stream_default_controller_clear_algorithms(ReadableStreamDefaultController&);

void readable_stream_default_controller_error(ReadableStreamDefaultController&, JS::Value error);
Optional<double> readable_stream_default_controller_get_desired_size(ReadableStreamDefaultController&);
bool readable_stream_default_controller_can_close_or_enqueue(ReadableStreamDefaultController&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_controller(ReadableStream&, ReadableStreamDefaultController&, StartAlgorithm&&, PullAlgorithm&&, CancelAlgorithm&&, double high_water_mark, SizeAlgorithm&&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_controller_from_underlying_source(ReadableStream&, JS::Value underlying_source_value, UnderlyingSource, double high_water_mark, SizeAlgorithm&&);
WebIDL::ExceptionOr<void> set_up_readable_stream_controller_with_byte_reading_support(ReadableStream&, Optional<PullAlgorithm>&& = {}, Optional<CancelAlgorithm>&& = {}, double high_water_mark = 0);
WebIDL::ExceptionOr<void> set_up_readable_byte_stream_controller(ReadableStream&, ReadableByteStreamController&, StartAlgorithm&&, PullAlgorithm&&, CancelAlgorithm&&, double high_water_mark, JS::Value auto_allocate_chunk_size);
WebIDL::ExceptionOr<void> set_up_readable_byte_stream_controller_from_underlying_source(ReadableStream&, JS::Value underlying_source, UnderlyingSource const& underlying_source_dict, double high_water_mark);

WebIDL::ExceptionOr<void> readable_stream_enqueue(ReadableStreamController& controller, JS::Value chunk);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_enqueue(ReadableByteStreamController& controller, JS::Value chunk);
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::ArrayBuffer>> transfer_array_buffer(JS::Realm& realm, JS::ArrayBuffer& buffer);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_enqueue_detached_pull_into_queue(ReadableByteStreamController& controller, PullIntoDescriptor& pull_into_descriptor);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_process_read_requests_using_queue(ReadableByteStreamController& controller);
void readable_byte_stream_controller_enqueue_chunk_to_queue(ReadableByteStreamController& controller, JS::NonnullGCPtr<JS::ArrayBuffer> buffer, u32 byte_offset, u32 byte_length);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_enqueue_cloned_chunk_to_queue(ReadableByteStreamController& controller, JS::ArrayBuffer& buffer, u64 byte_offset, u64 byte_length);
PullIntoDescriptor readable_byte_stream_controller_shift_pending_pull_into(ReadableByteStreamController& controller);

WebIDL::ExceptionOr<void> readable_byte_stream_controller_call_pull_if_needed(ReadableByteStreamController&);
void readable_byte_stream_controller_clear_algorithms(ReadableByteStreamController&);
void readable_byte_stream_controller_clear_pending_pull_intos(ReadableByteStreamController&);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_close(ReadableByteStreamController&);
void readable_byte_stream_controller_error(ReadableByteStreamController&, JS::Value error);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_fill_read_request_from_queue(ReadableByteStreamController&, JS::NonnullGCPtr<ReadRequest>);
Optional<double> readable_byte_stream_controller_get_desired_size(ReadableByteStreamController const&);
WebIDL::ExceptionOr<void> readable_byte_stream_controller_handle_queue_drain(ReadableByteStreamController&);
void readable_byte_stream_controller_invalidate_byob_request(ReadableByteStreamController&);
bool readable_byte_stream_controller_should_call_pull(ReadableByteStreamController const&);

WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStream>> create_readable_stream(JS::Realm& realm, StartAlgorithm&& start_algorithm, PullAlgorithm&& pull_algorithm, CancelAlgorithm&& cancel_algorithm, Optional<double> high_water_mark = {}, Optional<SizeAlgorithm>&& size_algorithm = {});
WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStream>> create_writable_stream(JS::Realm& realm, StartAlgorithm&& start_algorithm, WriteAlgorithm&& write_algorithm, CloseAlgorithm&& close_algorithm, AbortAlgorithm&& abort_algorithm, double high_water_mark, SizeAlgorithm&& size_algorithm);
void initialize_readable_stream(ReadableStream&);
void initialize_writable_stream(WritableStream&);

WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStreamDefaultWriter>> acquire_writable_stream_default_writer(WritableStream&);
bool is_writable_stream_locked(WritableStream const&);
WebIDL::ExceptionOr<void> set_up_writable_stream_default_writer(WritableStreamDefaultWriter&, WritableStream&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_abort(WritableStream&, JS::Value reason);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_close(WritableStream&);

WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_add_write_request(WritableStream&);
bool writable_stream_close_queued_or_in_flight(WritableStream const&);
WebIDL::ExceptionOr<void> writable_stream_deal_with_rejection(WritableStream&, JS::Value error);
WebIDL::ExceptionOr<void> writable_stream_finish_erroring(WritableStream&);
void writable_stream_finish_in_flight_close(WritableStream&);
WebIDL::ExceptionOr<void> writable_stream_finish_in_flight_close_with_error(WritableStream&, JS::Value error);
void writable_stream_finish_in_flight_write(WritableStream&);
WebIDL::ExceptionOr<void> writable_stream_finish_in_flight_write_with_error(WritableStream&, JS::Value error);
bool writable_stream_has_operation_marked_in_flight(WritableStream const&);
void writable_stream_mark_close_request_in_flight(WritableStream&);
void writable_stream_mark_first_write_request_in_flight(WritableStream&);
void writable_stream_reject_close_and_closed_promise_if_needed(WritableStream&);
WebIDL::ExceptionOr<void> writable_stream_start_erroring(WritableStream&, JS::Value reason);
void writable_stream_update_backpressure(WritableStream&, bool backpressure);

WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_default_writer_abort(WritableStreamDefaultWriter&, JS::Value reason);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_default_writer_close(WritableStreamDefaultWriter&);
void writable_stream_default_writer_ensure_closed_promise_rejected(WritableStreamDefaultWriter&, JS::Value error);
void writable_stream_default_writer_ensure_ready_promise_rejected(WritableStreamDefaultWriter&, JS::Value error);
Optional<double> writable_stream_default_writer_get_desired_size(WritableStreamDefaultWriter const&);
WebIDL::ExceptionOr<void> writable_stream_default_writer_release(WritableStreamDefaultWriter&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_default_writer_write(WritableStreamDefaultWriter&, JS::Value chunk);

WebIDL::ExceptionOr<void> set_up_writable_stream_default_controller(WritableStream&, WritableStreamDefaultController&, StartAlgorithm&&, WriteAlgorithm&&, CloseAlgorithm&&, AbortAlgorithm&&, double high_water_mark, SizeAlgorithm&&);
WebIDL::ExceptionOr<void> set_up_writable_stream_default_controller_from_underlying_sink(WritableStream&, JS::Value underlying_sink_value, UnderlyingSink&, double high_water_mark, SizeAlgorithm&& size_algorithm);
WebIDL::ExceptionOr<void> writable_stream_default_controller_advance_queue_if_needed(WritableStreamDefaultController&);
void writable_stream_default_controller_clear_algorithms(WritableStreamDefaultController&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_close(WritableStreamDefaultController&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_error(WritableStreamDefaultController&, JS::Value error);
WebIDL::ExceptionOr<void> writable_stream_default_controller_error_if_needed(WritableStreamDefaultController&, JS::Value error);
bool writable_stream_default_controller_get_backpressure(WritableStreamDefaultController const&);
WebIDL::ExceptionOr<JS::Value> writable_stream_default_controller_get_chunk_size(WritableStreamDefaultController&, JS::Value chunk);
double writable_stream_default_controller_get_desired_size(WritableStreamDefaultController const&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_process_close(WritableStreamDefaultController&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_process_write(WritableStreamDefaultController&, JS::Value chunk);
WebIDL::ExceptionOr<void> writable_stream_default_controller_write(WritableStreamDefaultController&, JS::Value chunk, JS::Value chunk_size);

WebIDL::ExceptionOr<void> initialize_transform_stream(TransformStream&, JS::NonnullGCPtr<JS::PromiseCapability> start_promise, double writable_high_water_mark, SizeAlgorithm&& writable_size_algorithm, double readable_high_water_mark, SizeAlgorithm&& readable_size_algorithm);
void set_up_transform_stream_default_controller(TransformStream&, TransformStreamDefaultController&, TransformAlgorithm&&, FlushAlgorithm&&);
WebIDL::ExceptionOr<void> set_up_transform_stream_default_controller_from_transformer(TransformStream&, JS::Value transformer, Transformer&);
void transform_stream_default_controller_clear_algorithms(TransformStreamDefaultController&);
WebIDL::ExceptionOr<void> transform_stream_default_controller_enqueue(TransformStreamDefaultController&, JS::Value chunk);
WebIDL::ExceptionOr<void> transform_stream_default_controller_error(TransformStreamDefaultController&, JS::Value error);
WebIDL::ExceptionOr<void> transform_stream_default_controller_terminate(TransformStreamDefaultController&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_controller_perform_transform(TransformStreamDefaultController&, JS::Value chunk);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_sink_abort_algorithm(TransformStream&, JS::Value reason);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_sink_close_algorithm(TransformStream&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_sink_write_algorithm(TransformStream&, JS::Value chunk);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> transform_stream_default_source_pull_algorithm(TransformStream&);
WebIDL::ExceptionOr<void> transform_stream_error(TransformStream&, JS::Value error);
WebIDL::ExceptionOr<void> transform_stream_error_writable_and_unblock_write(TransformStream&, JS::Value error);
WebIDL::ExceptionOr<void> transform_stream_set_backpressure(TransformStream&, bool backpressure);

bool is_non_negative_number(JS::Value);

JS::Value create_close_sentinel();
bool is_close_sentinel(JS::Value);
JS::ThrowCompletionOr<JS::Handle<WebIDL::CallbackType>> property_to_callback(JS::VM& vm, JS::Value value, JS::PropertyKey const& property_key, WebIDL::OperationReturnsPromise);

// https://streams.spec.whatwg.org/#value-with-size
struct ValueWithSize {
    JS::Value value;
    double size;
};

// https://streams.spec.whatwg.org/#dequeue-value
template<typename T>
JS::Value dequeue_value(T& container)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. Assert: container.[[queue]] is not empty.
    VERIFY(!container.queue().is_empty());

    // 3. Let valueWithSize be container.[[queue]][0].
    // 4. Remove valueWithSize from container.[[queue]].
    auto value_with_size = container.queue().take_first();

    // 5. Set container.[[queueTotalSize]] to container.[[queueTotalSize]] − valueWithSize’s size.
    container.set_queue_total_size(container.queue_total_size() - value_with_size.size);

    // 6. If container.[[queueTotalSize]] < 0, set container.[[queueTotalSize]] to 0. (This can occur due to rounding errors.)
    if (container.queue_total_size() < 0.0)
        container.set_queue_total_size(0.0);

    // 7. Return valueWithSize’s value.
    return value_with_size.value;
}

// https://streams.spec.whatwg.org/#enqueue-value-with-size
template<typename T>
WebIDL::ExceptionOr<void> enqueue_value_with_size(T& container, JS::Value value, JS::Value size_value)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. If ! IsNonNegativeNumber(size) is false, throw a RangeError exception.
    if (!is_non_negative_number(size_value))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Chunk has non-positive size"sv };

    auto size = size_value.as_double();

    // 3. If size is +∞, throw a RangeError exception.
    if (size == HUGE_VAL)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Chunk has infinite size"sv };

    // 4. Append a new value-with-size with value value and size size to container.[[queue]].
    container.queue().append({ value, size });

    // 5. Set container.[[queueTotalSize]] to container.[[queueTotalSize]] + size.
    container.set_queue_total_size(container.queue_total_size() + size);

    return {};
}

// https://streams.spec.whatwg.org/#peek-queue-value
template<typename T>
JS::Value peek_queue_value(T& container)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. Assert: container.[[queue]] is not empty.
    VERIFY(!container.queue().is_empty());

    // 3. Let valueWithSize be container.[[queue]][0].
    auto& value_with_size = container.queue().first();

    // 4. Return valueWithSize’s value.
    return value_with_size.value;
}

// https://streams.spec.whatwg.org/#reset-queue
template<typename T>
void reset_queue(T& container)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. Set container.[[queue]] to a new empty list.
    container.queue().clear();

    // 3. Set container.[[queueTotalSize]] to 0.
    container.set_queue_total_size(0);
}

}
