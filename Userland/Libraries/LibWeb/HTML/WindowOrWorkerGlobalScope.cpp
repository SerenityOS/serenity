/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Fetch/FetchMethod.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/WindowOrWorkerGlobalScope.h>
#include <LibWeb/Infra/Base64.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

WindowOrWorkerGlobalScopeMixin::~WindowOrWorkerGlobalScopeMixin() = default;

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-origin
WebIDL::ExceptionOr<String> WindowOrWorkerGlobalScopeMixin::origin() const
{
    auto& vm = this_impl().vm();

    // The origin getter steps are to return this's relevant settings object's origin, serialized.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(relevant_settings_object(this_impl()).origin().serialize()));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-issecurecontext
bool WindowOrWorkerGlobalScopeMixin::is_secure_context() const
{
    // The isSecureContext getter steps are to return true if this's relevant settings object is a secure context, or false otherwise.
    return HTML::is_secure_context(relevant_settings_object(this_impl()));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-crossoriginisolated
bool WindowOrWorkerGlobalScopeMixin::cross_origin_isolated() const
{
    // The crossOriginIsolated getter steps are to return this's relevant settings object's cross-origin isolated capability.
    return relevant_settings_object(this_impl()).cross_origin_isolated_capability() == CanUseCrossOriginIsolatedAPIs::Yes;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-btoa
WebIDL::ExceptionOr<String> WindowOrWorkerGlobalScopeMixin::btoa(String const& data) const
{
    auto& vm = this_impl().vm();
    auto& realm = *vm.current_realm();

    // The btoa(data) method must throw an "InvalidCharacterError" DOMException if data contains any character whose code point is greater than U+00FF.
    Vector<u8> byte_string;
    byte_string.ensure_capacity(data.bytes().size());
    for (u32 code_point : Utf8View(data)) {
        if (code_point > 0xff)
            return WebIDL::InvalidCharacterError::create(realm, "Data contains characters outside the range U+0000 and U+00FF");
        byte_string.append(code_point);
    }

    // Otherwise, the user agent must convert data to a byte sequence whose nth byte is the eight-bit representation of the nth code point of data,
    // and then must apply forgiving-base64 encode to that byte sequence and return the result.
    return TRY_OR_THROW_OOM(vm, encode_base64(byte_string.span()));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-atob
WebIDL::ExceptionOr<String> WindowOrWorkerGlobalScopeMixin::atob(String const& data) const
{
    auto& vm = this_impl().vm();
    auto& realm = *vm.current_realm();

    // 1. Let decodedData be the result of running forgiving-base64 decode on data.
    auto decoded_data = Infra::decode_forgiving_base64(data.bytes_as_string_view());

    // 2. If decodedData is failure, then throw an "InvalidCharacterError" DOMException.
    if (decoded_data.is_error())
        return WebIDL::InvalidCharacterError::create(realm, "Input string is not valid base64 data");

    // 3. Return decodedData.
    // decode_base64() returns a byte string. LibJS uses UTF-8 for strings. Use Latin1Decoder to convert bytes 128-255 to UTF-8.
    auto decoder = TextCodec::decoder_for("windows-1252"sv);
    VERIFY(decoder.has_value());
    return TRY_OR_THROW_OOM(vm, decoder->to_utf8(decoded_data.value()));
}

JS::NonnullGCPtr<JS::Promise> WindowOrWorkerGlobalScopeMixin::fetch(Fetch::RequestInfo const& input, Fetch::RequestInit const& init) const
{
    auto& vm = this_impl().vm();
    return Fetch::fetch(vm, input, init);
}

}
