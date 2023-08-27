/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/Streams/ReadableStreamBYOBReader.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

ReadableStreamBYOBReader::ReadableStreamBYOBReader(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
    , ReadableStreamGenericReaderMixin(realm)
{
}

// https://streams.spec.whatwg.org/#byob-reader-constructor
WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamBYOBReader>> ReadableStreamBYOBReader::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<ReadableStream> stream)
{
    auto reader = realm.heap().allocate<ReadableStreamBYOBReader>(realm, realm);

    // 1. Perform ? SetUpReadableStreamBYOBReader(this, stream).
    TRY(set_up_readable_stream_byob_reader(reader, *stream));

    return reader;
}

// https://streams.spec.whatwg.org/#byob-reader-release-lock
void ReadableStreamBYOBReader::release_lock()
{
    // 1. If this.[[stream]] is undefined, return.
    if (!m_stream)
        return;

    // 2. Perform ! ReadableStreamBYOBReaderRelease(this).
    readable_stream_byob_reader_release(*this);
}

void ReadableStreamBYOBReader::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    ReadableStreamGenericReaderMixin::visit_edges(visitor);
}

}
