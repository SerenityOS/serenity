/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/WorkerGlobalScopeWrapper.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>
#include <LibWeb/HTML/WorkerLocation.h>
#include <LibWeb/HTML/WorkerNavigator.h>

namespace Web::HTML {

WorkerGlobalScope::WorkerGlobalScope()
    : m_navigator(make_ref_counted<WorkerNavigator>())
{
}

WorkerGlobalScope::~WorkerGlobalScope() { }

// https://html.spec.whatwg.org/multipage/workers.html#importing-scripts-and-libraries
DOM::ExceptionOr<void> WorkerGlobalScope::import_scripts(Vector<String> urls)
{
    // The algorithm may optionally be customized by supplying custom perform the fetch hooks,
    // which if provided will be used when invoking fetch a classic worker-imported script.
    // NOTE: Service Workers is an example of a specification that runs this algorithm with its own options for the perform the fetch hook.

    // 1. FIXME: If worker global scope's type is "module", throw a TypeError exception.
    // 2. FIXME: Let settings object be the current settings object.

    // 3. If urls is empty, return.
    if (urls.is_empty())
        return {};

    // 4. FIXME: Parse each value in urls relative to settings object. If any fail, throw a "SyntaxError" DOMException.
    // 5. FIXME: For each url in the resulting URL records, run these substeps:
    //     1. Fetch a classic worker-imported script given url and settings object, passing along any custom perform the fetch steps provided.
    //        If this succeeds, let script be the result. Otherwise, rethrow the exception.
    //     2. Run the classic script script, with the rethrow errors argument set to true.
    //        NOTE: script will run until it either returns, fails to parse, fails to catch an exception,
    //              or gets prematurely aborted by the terminate a worker algorithm defined above.
    //        If an exception was thrown or if the script was prematurely aborted, then abort all these steps,
    //        letting the exception or aborting continue to be processed by the calling script.

    return {};
}

JS::Object* WorkerGlobalScope::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-workerglobalscope-location
NonnullRefPtr<WorkerLocation const> WorkerGlobalScope::location() const
{
    // The location attribute must return the WorkerLocation object whose associated WorkerGlobalScope object is the WorkerGlobalScope object.
    return *m_location;
}

// https://html.spec.whatwg.org/multipage/workers.html#dom-worker-navigator
NonnullRefPtr<WorkerNavigator const> WorkerGlobalScope::navigator() const
{
    // The navigator attribute of the WorkerGlobalScope interface must return an instance of the WorkerNavigator interface,
    // which represents the identity and state of the user agent (the client).
    return *m_navigator;
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                                          \
    void WorkerGlobalScope::set_##attribute_name(Optional<Bindings::CallbackType> value) \
    {                                                                                    \
        set_event_handler_attribute(event_name, move(value));                            \
    }                                                                                    \
    Bindings::CallbackType* WorkerGlobalScope::attribute_name()                          \
    {                                                                                    \
        return event_handler_attribute(event_name);                                      \
    }
ENUMERATE_WORKER_GLOBAL_SCOPE_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-origin
String WorkerGlobalScope::origin() const
{
    // FIXME: The origin getter steps are to return this's relevant settings object's origin, serialized.
    return {};
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-issecurecontext
bool WorkerGlobalScope::is_secure_context() const
{
    // FIXME: The isSecureContext getter steps are to return true if this's relevant settings object is a secure context, or false otherwise.
    return false;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-crossoriginisolated
bool WorkerGlobalScope::cross_origin_isolated() const
{
    // The crossOriginIsolated getter steps are to return this's relevant settings object's cross-origin isolated capability.
    // FIXME: Is this the same thing as https://html.spec.whatwg.org/multipage/workers.html#concept-workerglobalscope-cross-origin-isolated-capability?
    //        "A WorkerGlobalScope object has an associated cross-origin isolated capability boolean. It is initially false."
    return m_cross_origin_isolated_capability;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-btoa
DOM::ExceptionOr<String> WorkerGlobalScope::btoa(String const& data) const
{
    // FIXME: This is the same as the implementation in Bindings/WindowObject.cpp
    //     Find a way to share this implementation, since they come from the same mixin.

    // The btoa(data) method must throw an "InvalidCharacterError" DOMException if data contains any character whose code point is greater than U+00FF.
    Vector<u8> byte_string;
    byte_string.ensure_capacity(data.length());
    for (u32 code_point : Utf8View(data)) {
        if (code_point > 0xff)
            return DOM::InvalidCharacterError::create("Data contains characters outside the range U+0000 and U+00FF");
        byte_string.append(code_point);
    }

    // Otherwise, the user agent must convert data to a byte sequence whose nth byte is the eight-bit representation of the nth code point of data,
    // and then must apply forgiving-base64 encode to that byte sequence and return the result.
    return encode_base64(byte_string.span());
}

// https://html.spec.whatwg.org/multipage/webappapis.html#dom-atob
DOM::ExceptionOr<String> WorkerGlobalScope::atob(String const& data) const
{
    // FIXME: This is the same as the implementation in Bindings/WindowObject.cpp
    //     Find a way to share this implementation, since they come from the same mixin.

    // 1. Let decodedData be the result of running forgiving-base64 decode on data.
    auto decoded_data = decode_base64(data.view());

    // 2. If decodedData is failure, then throw an "InvalidCharacterError" DOMException.
    if (decoded_data.is_error())
        return DOM::InvalidCharacterError::create("Input string is not valid base64 data");

    // 3. Return decodedData.
    // decode_base64() returns a byte string. LibJS uses UTF-8 for strings. Use Latin1Decoder to convert bytes 128-255 to UTF-8.
    auto* decoder = TextCodec::decoder_for("windows-1252");
    VERIFY(decoder);
    return decoder->to_utf8(decoded_data.value());
}

}
