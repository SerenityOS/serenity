/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/HostDefined.h>
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
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> BodyMixin::array_buffer() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The arrayBuffer() method steps are to return the result of running consume body with this and ArrayBuffer.
    return consume_body(realm, *this, PackageDataType::ArrayBuffer);
}

// https://fetch.spec.whatwg.org/#dom-body-blob
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> BodyMixin::blob() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The blob() method steps are to return the result of running consume body with this and Blob.
    return consume_body(realm, *this, PackageDataType::Blob);
}

// https://fetch.spec.whatwg.org/#dom-body-formdata
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> BodyMixin::form_data() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The formData() method steps are to return the result of running consume body with this and FormData.
    return consume_body(realm, *this, PackageDataType::FormData);
}

// https://fetch.spec.whatwg.org/#dom-body-json
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> BodyMixin::json() const
{
    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // The json() method steps are to return the result of running consume body with this and JSON.
    return consume_body(realm, *this, PackageDataType::JSON);
}

// https://fetch.spec.whatwg.org/#dom-body-text
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> BodyMixin::text() const
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
        auto mime_type_string = mime_type.has_value() ? TRY_OR_THROW_OOM(vm, mime_type->serialized()) : String {};
        return TRY(FileAPI::Blob::create(realm, move(bytes), move(mime_type_string)));
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
        return Infra::parse_json_bytes_to_javascript_value(realm, bytes);
    case PackageDataType::Text:
        // Return the result of running UTF-8 decode on bytes.
        return JS::PrimitiveString::create(vm, TRY_OR_THROW_OOM(vm, String::from_utf8(bytes)));
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://fetch.spec.whatwg.org/#concept-body-consume-body
WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> consume_body(JS::Realm& realm, BodyMixin const& object, PackageDataType type)
{
    // 1. If object is unusable, then return a promise rejected with a TypeError.
    if (object.is_unusable()) {
        auto exception = MUST_OR_THROW_OOM(JS::TypeError::create(realm, "Body is unusable"sv));
        auto promise_capability = WebIDL::create_rejected_promise(realm, exception);
        return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise_capability->promise().ptr()) };
    }

    // 2. Let promise be a new promise.
    auto promise = WebIDL::create_promise(realm);

    // 3. Let errorSteps given error be to reject promise with error.
    // NOTE: `promise` and `realm` is protected by JS::SafeFunction.
    auto error_steps = [promise, &realm](JS::Object& error) {
        // NOTE: Not part of the spec, but we need to have an execution context on the stack to call native functions.
        //       (In this case, Promise's reject function)
        auto& environment_settings_object = Bindings::host_defined_environment_settings_object(realm);
        environment_settings_object.prepare_to_run_script();

        WebIDL::reject_promise(realm, promise, &error);

        // See above NOTE.
        environment_settings_object.clean_up_after_running_script();
    };

    // 4. Let successSteps given a byte sequence data be to resolve promise with the result of running convertBytesToJSValue
    //    with data. If that threw an exception, then run errorSteps with that exception.
    // NOTE: `promise`, `realm` and `object` is protected by JS::SafeFunction.
    // FIXME: Refactor this to the new version of the spec introduced with https://github.com/whatwg/fetch/commit/464326e8eb6a602122c030cd40042480a3c0e265
    auto success_steps = [promise, &realm, &object, type](ByteBuffer const& data) {
        auto& vm = realm.vm();

        // NOTE: Not part of the spec, but we need to have an execution context on the stack to call native functions.
        //       (In this case, Promise's reject function and JSON.parse)
        auto& environment_settings_object = Bindings::host_defined_environment_settings_object(realm);
        environment_settings_object.prepare_to_run_script();

        ScopeGuard guard = [&]() {
            // See above NOTE.
            environment_settings_object.clean_up_after_running_script();
        };

        auto value_or_error = Bindings::throw_dom_exception_if_needed(vm, [&]() -> WebIDL::ExceptionOr<JS::Value> {
            return package_data(realm, data, type, TRY_OR_THROW_OOM(vm, object.mime_type_impl()));
        });

        if (value_or_error.is_error()) {
            // We can't call error_steps here without moving it into success_steps, causing a double move when we pause error_steps
            // to fully_read, so just reject the promise like error_steps does.
            WebIDL::reject_promise(realm, promise, value_or_error.release_error().value().value());
            return;
        }

        WebIDL::resolve_promise(realm, promise, value_or_error.release_value());
    };

    // 5. If object’s body is null, then run successSteps with an empty byte sequence.
    auto const& body = object.body_impl();
    if (!body.has_value()) {
        success_steps(ByteBuffer {});
    }
    // 6. Otherwise, fully read object’s body given successSteps, errorSteps, and object’s relevant global object.
    else {
        TRY(body->fully_read(realm, move(success_steps), move(error_steps), JS::NonnullGCPtr { HTML::relevant_global_object(object.as_platform_object()) }));
    }

    // 7. Return promise.
    return JS::NonnullGCPtr { verify_cast<JS::Promise>(*promise->promise().ptr()) };
}

}
