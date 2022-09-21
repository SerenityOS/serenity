/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/URL/URLSearchParams.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#concept-bodyinit-extract
// FIXME: The parameter 'body_init' should be 'typedef (ReadableStream or XMLHttpRequestBodyInit) BodyInit'. For now we just let it be 'XMLHttpRequestBodyInit'.
ErrorOr<Infrastructure::BodyWithType> extract_body(JS::Realm& realm, XMLHttpRequestBodyInit const& body_init)
{
    auto& window = verify_cast<HTML::Window>(realm.global_object());

    // FIXME: 1. Let stream be object if object is a ReadableStream object. Otherwise, let stream be a new ReadableStream, and set up stream.
    auto* stream = realm.heap().allocate<Streams::ReadableStream>(realm, window);
    // FIXME: 2. Let action be null.
    // 3. Let source be null.
    Infrastructure::Body::SourceType source {};
    // 4. Let length be null.
    Optional<u64> length {};
    // 5. Let type be null.
    Optional<ByteBuffer> type {};

    // 6. Switch on object.
    // FIXME: Still need to support BufferSource and FormData
    TRY(body_init.visit(
        [&](JS::Handle<FileAPI::Blob> const& blob) -> ErrorOr<void> {
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
        [&](JS::Handle<JS::Object> const& buffer_source) -> ErrorOr<void> {
            // Set source to a copy of the bytes held by object.
            source = TRY(Bindings::IDL::get_buffer_source_copy(*buffer_source.cell()));
            return {};
        },
        [&](JS::Handle<URL::URLSearchParams> const& url_search_params) -> ErrorOr<void> {
            // Set source to the result of running the application/x-www-form-urlencoded serializer with object’s list.
            source = url_search_params->to_string().to_byte_buffer();
            // Set type to `application/x-www-form-urlencoded;charset=UTF-8`.
            type = TRY(ByteBuffer::copy("application/x-www-form-urlencoded;charset=UTF-8"sv.bytes()));
            return {};
        },
        [&](String const& scalar_value_string) -> ErrorOr<void> {
            // NOTE: AK::String is always UTF-8.
            // Set source to the UTF-8 encoding of object.
            source = scalar_value_string.to_byte_buffer();
            // Set type to `text/plain;charset=UTF-8`.
            type = TRY(ByteBuffer::copy("text/plain;charset=UTF-8"sv.bytes()));
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
