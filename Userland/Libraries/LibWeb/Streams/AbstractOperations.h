/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Streams/QueueOperations.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Streams {

using SizeAlgorithm = JS::SafeFunction<JS::Completion(JS::Value)>;
using PullAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>>()>;
using CancelAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>>(JS::Value)>;
using StartAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>>()>;
using AbortAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>>(JS::Value)>;
using CloseAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>>()>;
using WriteAlgorithm = JS::SafeFunction<WebIDL::ExceptionOr<JS::GCPtr<WebIDL::Promise>>(JS::Value)>;

WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamDefaultReader>> acquire_readable_stream_default_reader(ReadableStream&);
bool is_readable_stream_locked(ReadableStream const&);

void readable_stream_close(ReadableStream&);
void readable_stream_error(ReadableStream&, JS::Value error);
void readable_stream_add_read_request(ReadableStream&, ReadRequest const&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> readable_stream_cancel(ReadableStream&, JS::Value reason);
void readable_stream_fulfill_read_request(ReadableStream&, JS::Value chunk, bool done);
size_t readable_stream_get_num_read_requests(ReadableStream&);
bool readable_stream_has_default_reader(ReadableStream&);

JS::NonnullGCPtr<WebIDL::Promise> readable_stream_reader_generic_cancel(ReadableStreamGenericReaderMixin&, JS::Value reason);
void readable_stream_reader_generic_initialize(ReadableStreamGenericReaderMixin&, ReadableStream&);
WebIDL::ExceptionOr<void> readable_stream_reader_generic_release(ReadableStreamGenericReaderMixin&);

void readable_stream_default_reader_error_read_requests(ReadableStreamDefaultReader&, JS::Value error);
void readable_stream_default_reader_read(ReadableStreamDefaultReader&, ReadRequest&);
WebIDL::ExceptionOr<void> readable_stream_default_reader_release(ReadableStreamDefaultReader&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_reader(ReadableStreamDefaultReader&, ReadableStream&);
void readable_stream_default_controller_close(ReadableStreamDefaultController&);
WebIDL::ExceptionOr<void> readable_stream_default_controller_enqueue(ReadableStreamDefaultController&, JS::Value chunk);
WebIDL::ExceptionOr<void> readable_stream_default_controller_can_pull_if_needed(ReadableStreamDefaultController&);
bool readable_stream_default_controller_should_call_pull(ReadableStreamDefaultController&);
void readable_stream_default_controller_clear_algorithms(ReadableStreamDefaultController&);

void readable_stream_default_controller_error(ReadableStreamDefaultController&, JS::Value error);
Optional<float> readable_stream_default_controller_get_desired_size(ReadableStreamDefaultController&);
bool readable_stream_default_controller_can_close_or_enqueue(ReadableStreamDefaultController&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_controller(ReadableStream&, ReadableStreamDefaultController&, StartAlgorithm&&, PullAlgorithm&&, CancelAlgorithm&&, double high_water_mark, SizeAlgorithm&&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_controller_from_underlying_source(ReadableStream&, JS::Value underlying_source_value, UnderlyingSource, double high_water_mark, SizeAlgorithm&&);

WebIDL::ExceptionOr<JS::NonnullGCPtr<WritableStreamDefaultWriter>> acquire_writable_stream_default_writer(WritableStream&);
bool is_writable_stream_locked(WritableStream const&);
WebIDL::ExceptionOr<void> set_up_writable_stream_default_writer(WritableStreamDefaultWriter&, WritableStream&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_abort(WritableStream&, JS::Value reason);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> writable_stream_close(WritableStream&);

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
void writable_stream_default_writer_ensure_ready_promise_rejected(WritableStreamDefaultWriter&, JS::Value error);
Optional<double> writable_stream_default_writer_get_desired_size(WritableStreamDefaultWriter const&);

WebIDL::ExceptionOr<void> writable_stream_default_controller_advance_queue_if_needed(WritableStreamDefaultController&);
void writable_stream_default_controller_clear_algorithms(WritableStreamDefaultController&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_close(WritableStreamDefaultController&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_error(WritableStreamDefaultController&, JS::Value error);
bool writable_stream_default_controller_get_backpressure(WritableStreamDefaultController const&);
double writable_stream_default_controller_get_desired_size(WritableStreamDefaultController const&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_process_close(WritableStreamDefaultController&);
WebIDL::ExceptionOr<void> writable_stream_default_controller_process_write(WritableStreamDefaultController&, JS::Value chunk);

JS::Value create_close_sentinel();
bool is_close_sentinel(JS::Value);
JS::ThrowCompletionOr<JS::Handle<WebIDL::CallbackType>> property_to_callback(JS::VM& vm, JS::Value value, JS::PropertyKey const& property_key);

}
