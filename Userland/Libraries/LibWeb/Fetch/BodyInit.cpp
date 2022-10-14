/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/URL/URLSearchParams.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#bodyinit-safely-extract
WebIDL::ExceptionOr<Infrastructure::BodyWithType> safely_extract_body(JS::Realm& realm, BodyInitOrReadableBytes const& object)
{
    // 1. If object is a ReadableStream object, then:
    if (auto const* stream = object.get_pointer<JS::Handle<Streams::ReadableStream>>()) {
        // 1. Assert: object is neither disturbed nor locked.
        VERIFY(!((*stream)->is_disturbed() || (*stream)->is_locked()));
    }

    // 2. Return the result of extracting object.
    return extract_body(realm, object);
}

// https://fetch.spec.whatwg.org/#concept-bodyinit-extract
WebIDL::ExceptionOr<Infrastructure::BodyWithType> extract_body(JS::Realm& realm, BodyInitOrReadableBytes const& object, bool keepalive)
{
    // 1. Let stream be null.
    JS::GCPtr<Streams::ReadableStream> stream;

    // 2. If object is a ReadableStream object, then set stream to object.
    if (auto const* stream_handle = object.get_pointer<JS::Handle<Streams::ReadableStream>>()) {
        stream = const_cast<Streams::ReadableStream*>(stream_handle->cell());
    }
    // 3. Otherwise, if object is a Blob object, set stream to the result of running object’s get stream.
    else if (auto const* blob_handle = object.get_pointer<JS::Handle<FileAPI::Blob>>()) {
        // FIXME: "set stream to the result of running object’s get stream"
        (void)blob_handle;
        stream = realm.heap().allocate<Streams::ReadableStream>(realm, realm);
    }
    // 4. Otherwise, set stream to a new ReadableStream object, and set up stream.
    else {
        // FIXME: "set up stream"
        stream = realm.heap().allocate<Streams::ReadableStream>(realm, realm);
    }

    // 5. Assert: stream is a ReadableStream object.
    VERIFY(stream);

    // FIXME: 6. Let action be null.

    // 7. Let source be null.
    Infrastructure::Body::SourceType source {};

    // 8. Let length be null.
    Optional<u64> length {};

    // 9. Let type be null.
    Optional<ByteBuffer> type {};

    // 10. Switch on object.
    // FIXME: Still need to support BufferSource and FormData
    TRY(object.visit(
        [&](JS::Handle<FileAPI::Blob> const& blob) -> WebIDL::ExceptionOr<void> {
            // Set source to object.
            source = blob;
            // Set length to object’s size.
            length = blob->size();
            // If object’s type attribute is not the empty byte sequence, set type to its value.
            if (!blob->type().is_empty())
                type = blob->type().to_byte_buffer();
            return {};
        },
        [&](ReadonlyBytes bytes) -> WebIDL::ExceptionOr<void> {
            // Set source to object.
            source = TRY_OR_RETURN_OOM(realm, ByteBuffer::copy(bytes));
            return {};
        },
        [&](JS::Handle<JS::Object> const& buffer_source) -> WebIDL::ExceptionOr<void> {
            // Set source to a copy of the bytes held by object.
            source = TRY_OR_RETURN_OOM(realm, WebIDL::get_buffer_source_copy(*buffer_source.cell()));
            return {};
        },
        [&](JS::Handle<URL::URLSearchParams> const& url_search_params) -> WebIDL::ExceptionOr<void> {
            // Set source to the result of running the application/x-www-form-urlencoded serializer with object’s list.
            source = url_search_params->to_deprecated_string().to_byte_buffer();
            // Set type to `application/x-www-form-urlencoded;charset=UTF-8`.
            type = TRY_OR_RETURN_OOM(realm, ByteBuffer::copy("application/x-www-form-urlencoded;charset=UTF-8"sv.bytes()));
            return {};
        },
        [&](DeprecatedString const& scalar_value_string) -> WebIDL::ExceptionOr<void> {
            // NOTE: AK::DeprecatedString is always UTF-8.
            // Set source to the UTF-8 encoding of object.
            source = scalar_value_string.to_byte_buffer();
            // Set type to `text/plain;charset=UTF-8`.
            type = TRY_OR_RETURN_OOM(realm, ByteBuffer::copy("text/plain;charset=UTF-8"sv.bytes()));
            return {};
        },
        [&](JS::Handle<Streams::ReadableStream> const& stream) -> WebIDL::ExceptionOr<void> {
            // If keepalive is true, then throw a TypeError.
            if (keepalive)
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot extract body from stream when keepalive is set"sv };

            // If object is disturbed or locked, then throw a TypeError.
            if (stream->is_disturbed() || stream->is_locked())
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot extract body from disturbed or locked stream"sv };

            return {};
        }));

    // FIXME: 11. If source is a byte sequence, then set action to a step that returns source and length to source’s length.
    // FIXME: 12. If action is non-null, then run these steps in parallel:

    // 13. Let body be a body whose stream is stream, source is source, and length is length.
    auto body = Infrastructure::Body { JS::make_handle(*stream), move(source), move(length) };

    // 14. Return (body, type).
    return Infrastructure::BodyWithType { .body = move(body), .type = move(type) };
}

}
