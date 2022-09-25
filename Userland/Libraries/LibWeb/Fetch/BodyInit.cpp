/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/URL/URLSearchParams.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#concept-bodyinit-extract
WebIDL::ExceptionOr<Infrastructure::BodyWithType> extract_body(JS::Realm& realm, BodyInit const& object, bool keepalive)
{
    auto& window = verify_cast<HTML::Window>(realm.global_object());

    // 1. Let stream be object if object is a ReadableStream object. Otherwise, let stream be a new ReadableStream, and set up stream.
    Streams::ReadableStream* stream;
    if (auto const* handle = object.get_pointer<JS::Handle<Streams::ReadableStream>>()) {
        stream = const_cast<Streams::ReadableStream*>(handle->cell());
    } else {
        stream = realm.heap().allocate<Streams::ReadableStream>(realm, window);
    }

    // FIXME: 2. Let action be null.
    // 3. Let source be null.
    Infrastructure::Body::SourceType source {};
    // 4. Let length be null.
    Optional<u64> length {};
    // 5. Let type be null.
    Optional<ByteBuffer> type {};

    // 6. Switch on object.
    // FIXME: Still need to support BufferSource and FormData
    TRY(object.visit(
        [&](JS::Handle<FileAPI::Blob> const& blob) -> WebIDL::ExceptionOr<void> {
            // FIXME: Set action to this step: read object.
            // Set source to object.
            source = blob;
            // Set length to object’s size.
            length = blob->size();
            // If object’s type attribute is not the empty byte sequence, set type to its value.
            if (!blob->type().is_empty())
                type = blob->type().to_byte_buffer();
            return {};
        },
        [&](JS::Handle<JS::Object> const& buffer_source) -> WebIDL::ExceptionOr<void> {
            // Set source to a copy of the bytes held by object.
            source = TRY_OR_RETURN_OOM(window, WebIDL::get_buffer_source_copy(*buffer_source.cell()));
            return {};
        },
        [&](JS::Handle<URL::URLSearchParams> const& url_search_params) -> WebIDL::ExceptionOr<void> {
            // Set source to the result of running the application/x-www-form-urlencoded serializer with object’s list.
            source = url_search_params->to_string().to_byte_buffer();
            // Set type to `application/x-www-form-urlencoded;charset=UTF-8`.
            type = TRY_OR_RETURN_OOM(window, ByteBuffer::copy("application/x-www-form-urlencoded;charset=UTF-8"sv.bytes()));
            return {};
        },
        [&](String const& scalar_value_string) -> WebIDL::ExceptionOr<void> {
            // NOTE: AK::String is always UTF-8.
            // Set source to the UTF-8 encoding of object.
            source = scalar_value_string.to_byte_buffer();
            // Set type to `text/plain;charset=UTF-8`.
            type = TRY_OR_RETURN_OOM(window, ByteBuffer::copy("text/plain;charset=UTF-8"sv.bytes()));
            return {};
        },
        [&](JS::Handle<Streams::ReadableStream> const& stream) -> WebIDL::ExceptionOr<void> {
            // If keepalive is true, then throw a TypeError.
            if (keepalive)
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot extract body from stream when keepalive is set" };

            // If object is disturbed or locked, then throw a TypeError.
            if (stream->is_disturbed() || stream->is_locked())
                return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Cannot extract body from disturbed or locked stream" };

            return {};
        }));

    // FIXME: 7. If source is a byte sequence, then set action to a step that returns source and length to source’s length.
    // FIXME: 8. If action is non-null, then run these steps in in parallel:

    // 9. Let body be a body whose stream is stream, source is source, and length is length.
    auto body = Infrastructure::Body { JS::make_handle(stream), move(source), move(length) };
    // 10. Return (body, type).
    return Infrastructure::BodyWithType { .body = move(body), .type = move(type) };
}

}
