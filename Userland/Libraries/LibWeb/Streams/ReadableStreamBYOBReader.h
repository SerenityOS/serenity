/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Streams/ReadableStreamGenericReader.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#read-into-request
class ReadIntoRequest : public RefCounted<ReadIntoRequest> {
public:
    virtual ~ReadIntoRequest() = default;

    // An algorithm taking a chunk, called when a chunk is available for reading
    virtual void on_chunk(JS::Value chunk) = 0;

    // An algorithm taking a chunk or undefined, called when no chunks are available because the stream is closed
    virtual void on_close(JS::Value chunk_or_undefined) = 0;

    // An algorithm taking a JavaScript value, called when no chunks are available because the stream is errored
    virtual void on_error(JS::Value error) = 0;
};

// https://streams.spec.whatwg.org/#readablestreambyobreader
class ReadableStreamBYOBReader final
    : public Bindings::PlatformObject
    , public ReadableStreamGenericReaderMixin {
    WEB_PLATFORM_OBJECT(ReadableStreamBYOBReader, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<ReadableStreamBYOBReader>> construct_impl(JS::Realm&, JS::NonnullGCPtr<ReadableStream>);

    virtual ~ReadableStreamBYOBReader() override = default;

    void release_lock();

    Vector<NonnullRefPtr<ReadIntoRequest>>& read_into_requests() { return m_read_into_requests; }

private:
    explicit ReadableStreamBYOBReader(JS::Realm&);

    virtual void visit_edges(Cell::Visitor&) override;

    // https://streams.spec.whatwg.org/#readablestreambyobreader-readintorequests
    // A list of read-into requests, used when a consumer requests chunks sooner than they are available
    Vector<NonnullRefPtr<ReadIntoRequest>> m_read_into_requests;
};

}
