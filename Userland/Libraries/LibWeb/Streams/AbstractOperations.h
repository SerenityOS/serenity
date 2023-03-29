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

bool is_readable_stream_locked(ReadableStream const&);

void readable_stream_close(ReadableStream&);
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebIDL::Promise>> readable_stream_cancel(ReadableStream&, JS::Value reason);

JS::NonnullGCPtr<WebIDL::Promise> readable_stream_reader_generic_cancel(ReadableStreamGenericReaderMixin&, JS::Value reason);
void readable_stream_reader_generic_initialize(ReadableStreamGenericReaderMixin&, ReadableStream&);
WebIDL::ExceptionOr<void> readable_stream_reader_generic_release(ReadableStreamGenericReaderMixin&);

void readable_stream_default_reader_error_read_requests(ReadableStreamDefaultReader&, JS::Value error);
void readable_stream_default_reader_read(ReadableStreamDefaultReader&, ReadRequest&);
WebIDL::ExceptionOr<void> readable_stream_default_reader_release(ReadableStreamDefaultReader&);
WebIDL::ExceptionOr<void> set_up_readable_stream_default_reader(ReadableStreamDefaultReader&, ReadableStream&);

}
