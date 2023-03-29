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

}
