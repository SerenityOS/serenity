/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Forward.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/Variant.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Fetch/Infrastructure/Task.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#concept-body
class Body final {
public:
    using SourceType = Variant<Empty, ByteBuffer, JS::Handle<FileAPI::Blob>>;
    // processBody must be an algorithm accepting a byte sequence.
    using ProcessBodyCallback = JS::SafeFunction<void(ByteBuffer)>;
    // processBodyError must be an algorithm optionally accepting an exception.
    using ProcessBodyErrorCallback = JS::SafeFunction<void(JS::GCPtr<WebIDL::DOMException>)>;

    explicit Body(JS::Handle<Streams::ReadableStream>);
    Body(JS::Handle<Streams::ReadableStream>, SourceType, Optional<u64>);

    [[nodiscard]] JS::NonnullGCPtr<Streams::ReadableStream> stream() const { return *m_stream; }
    [[nodiscard]] SourceType const& source() const { return m_source; }
    [[nodiscard]] Optional<u64> const& length() const { return m_length; }

    [[nodiscard]] Body clone(JS::Realm&) const;

    WebIDL::ExceptionOr<void> fully_read(JS::Realm&, ProcessBodyCallback process_body, ProcessBodyErrorCallback process_body_error, TaskDestination task_destination) const;

private:
    // https://fetch.spec.whatwg.org/#concept-body-stream
    // A stream (a ReadableStream object).
    JS::Handle<Streams::ReadableStream> m_stream;

    // https://fetch.spec.whatwg.org/#concept-body-source
    // A source (null, a byte sequence, a Blob object, or a FormData object), initially null.
    SourceType m_source;

    // https://fetch.spec.whatwg.org/#concept-body-total-bytes
    // A length (null or an integer), initially null.
    Optional<u64> m_length;
};

// https://fetch.spec.whatwg.org/#body-with-type
// A body with type is a tuple that consists of a body (a body) and a type (a header value or null).
struct BodyWithType {
    Body body;
    Optional<ByteBuffer> type;
};

WebIDL::ExceptionOr<Body> byte_sequence_as_body(JS::Realm&, ReadonlyBytes);

}
