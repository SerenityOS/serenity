/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Fetch/Body.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/Infra/JSON.h>
#include <LibWeb/MimeSniff/MimeType.h>
#include <LibWeb/Streams/ReadableStream.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::Fetch {

BodyMixin::~BodyMixin() = default;

// https://fetch.spec.whatwg.org/#body-unusable
bool BodyMixin::is_unusable() const
{
    // An object including the Body interface mixin is said to be unusable if its body is non-null and its body’s stream is disturbed or locked.
    auto const& body = body_impl();
    return body.has_value() && (body->stream()->is_disturbed() || body->stream()->is_locked());
}

// https://fetch.spec.whatwg.org/#dom-body-body
JS::GCPtr<Streams::ReadableStream> BodyMixin::body() const
{
    // The body getter steps are to return null if this’s body is null; otherwise this’s body’s stream.
    auto const& body = body_impl();
    return body.has_value() ? body->stream().ptr() : nullptr;
}

// https://fetch.spec.whatwg.org/#dom-body-bodyused
bool BodyMixin::body_used() const
{
    // The bodyUsed getter steps are to return true if this’s body is non-null and this’s body’s stream is disturbed; otherwise false.
    auto const& body = body_impl();
    return body.has_value() && body->stream()->is_disturbed();
}

// https://fetch.spec.whatwg.org/#dom-body-arraybuffer
JS::NonnullGCPtr<JS::Promise> BodyMixin::array_buffer() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The arrayBuffer() method steps are to return the result of running consume body with this and ArrayBuffer.
    return consume_body(realm, *this, PackageDataType::ArrayBuffer);
}

// https://fetch.spec.whatwg.org/#dom-body-blob
JS::NonnullGCPtr<JS::Promise> BodyMixin::blob() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The blob() method steps are to return the result of running consume body with this and Blob.
    return consume_body(realm, *this, PackageDataType::Blob);
}

// https://fetch.spec.whatwg.org/#dom-body-formdata
JS::NonnullGCPtr<JS::Promise> BodyMixin::form_data() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The formData() method steps are to return the result of running consume body with this and FormData.
    return consume_body(realm, *this, PackageDataType::FormData);
}

// https://fetch.spec.whatwg.org/#dom-body-json
JS::NonnullGCPtr<JS::Promise> BodyMixin::json() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The json() method steps are to return the result of running consume body with this and JSON.
    return consume_body(realm, *this, PackageDataType::JSON);
}

// https://fetch.spec.whatwg.org/#dom-body-text
JS::NonnullGCPtr<JS::Promise> BodyMixin::text() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The text() method steps are to return the result of running consume body with this and text.
    return consume_body(realm, *this, PackageDataType::Text);
}

// https://fetch.spec.whatwg.org/#concept-body-package-data
WebIDL::ExceptionOr<JS::Value> package_data(JS::Realm& realm, ByteBuffer bytes, PackageDataType type, Optional<MimeSniff::MimeType> const& mime_type)
{
    auto& vm = realm.vm();

    switch (type) {
    case PackageDataType::ArrayBuffer:
        // Return a new ArrayBuffer whose contents are bytes.
        return JS::ArrayBuffer::create(realm, move(bytes));
    case PackageDataType::Blob: {
        // Return a Blob whose contents are bytes and type attribute is mimeType.
        // NOTE: If extracting the mime type returns failure, other browsers set it to an empty string - not sure if that's spec'd.
        auto mime_type_string = mime_type.has_value() ? mime_type->serialized() : String::empty();
        return FileAPI::Blob::create(realm, move(bytes), move(mime_type_string));
    }
    case PackageDataType::FormData:
        // If mimeType’s essence is "multipart/form-data", then:
        if (mime_type.has_value() && mime_type->essence() == "multipart/form-data"sv) {
            // FIXME: 1. Parse bytes, using the value of the `boundary` parameter from mimeType, per the rules set forth in Returning Values from Forms: multipart/form-data. [RFC7578]
            // FIXME: 2. If that fails for some reason, then throw a TypeError.
            // FIXME: 3. Return a new FormData object, appending each entry, resulting from the parsing operation, to its entry list.
            return JS::js_null();
        }
        // Otherwise, if mimeType’s essence is "application/x-www-form-urlencoded", then:
        else if (mime_type.has_value() && mime_type->essence() == "application/x-www-form-urlencoded"sv) {
            // FIXME: 1. Let entries be the result of parsing bytes.
            // FIXME: 2. If entries is failure, then throw a TypeError.
            // FIXME: 3. Return a new FormData object whose entry list is entries.
            return JS::js_null();
        }
        // Otherwise, throw a TypeError.
        else {
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Mime type must be 'multipart/form-data' or 'application/x-www-form-urlencoded'"sv };
        }
    case PackageDataType::JSON:
        // Return the result of running parse JSON from bytes on bytes.
        return Infra::parse_json_bytes_to_javascript_value(vm, bytes);
    case PackageDataType::Text:
        // Return the result of running UTF-8 decode on bytes.
        return JS::js_string(vm, String::copy(bytes));
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://fetch.spec.whatwg.org/#concept-body-consume-body
JS::NonnullGCPtr<JS::Promise> consume_body(JS::Realm& realm, BodyMixin const& object, PackageDataType type)
{
    auto& vm = realm.vm();

    // 1. If object is unusable, then return a promise rejected with a TypeError.
    if (object.is_unusable()) {
        auto promise_capability = WebIDL::create_rejected_promise(realm, JS::TypeError::create(realm, "Body is unusable"sv));
        return verify_cast<JS::Promise>(*promise_capability->promise().ptr());
    }

    // 2. Let promise be a promise resolved with an empty byte sequence.
    auto promise = WebIDL::create_resolved_promise(realm, JS::js_string(vm, String::empty()));

    // 3. If object’s body is non-null, then set promise to the result of fully reading body as promise given object’s body.
    auto const& body = object.body_impl();
    if (body.has_value())
        promise = body->fully_read_as_promise();

    // 4. Let steps be to return the result of package data with the first argument given, type, and object’s MIME type.
    auto steps = [&realm, &object, type](JS::Value value) -> WebIDL::ExceptionOr<JS::Value> {
        VERIFY(value.is_string());
        auto bytes = TRY_OR_RETURN_OOM(realm, ByteBuffer::copy(value.as_string().string().bytes()));
        return package_data(realm, move(bytes), type, object.mime_type_impl());
    };

    // 5. Return the result of upon fulfillment of promise given steps.
    return WebIDL::upon_fulfillment(promise, move(steps));
}

}
