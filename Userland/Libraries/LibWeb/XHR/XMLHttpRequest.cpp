/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Origin.h>
#include <LibWeb/XHR/EventNames.h>
#include <LibWeb/XHR/ProgressEvent.h>
#include <LibWeb/XHR/XMLHttpRequest.h>

namespace Web::XHR {

XMLHttpRequest::XMLHttpRequest(DOM::Window& window)
    : XMLHttpRequestEventTarget(static_cast<Bindings::ScriptExecutionContext&>(window.document()))
    , m_window(window)
{
}

XMLHttpRequest::~XMLHttpRequest()
{
}

void XMLHttpRequest::set_ready_state(ReadyState ready_state)
{
    m_ready_state = ready_state;
    dispatch_event(DOM::Event::create(EventNames::readystatechange));
}

void XMLHttpRequest::fire_progress_event(const String& event_name, u64 transmitted, u64 length)
{
    dispatch_event(ProgressEvent::create(event_name, transmitted, length));
}

String XMLHttpRequest::response_text() const
{
    if (m_response_object.is_null())
        return {};
    return String::copy(m_response_object);
}

// https://fetch.spec.whatwg.org/#forbidden-header-name
static bool is_forbidden_header_name(const String& header_name)
{
    if (header_name.starts_with("Proxy-", CaseSensitivity::CaseInsensitive) || header_name.starts_with("Sec-", CaseSensitivity::CaseInsensitive))
        return true;

    auto lowercase_header_name = header_name.to_lowercase();
    return lowercase_header_name.is_one_of("accept-charset", "accept-encoding", "access-control-request-headers", "access-control-request-method", "connection", "content-length", "cookie", "cookie2", "date", "dnt", "expect", "host", "keep-alive", "origin", "referer", "te", "trailer", "transfer-encoding", "upgrade", "via");
}

// https://fetch.spec.whatwg.org/#forbidden-method
static bool is_forbidden_method(const String& method)
{
    auto lowercase_method = method.to_lowercase();
    return lowercase_method.is_one_of("connect", "trace", "track");
}

// https://fetch.spec.whatwg.org/#concept-method-normalize
static String normalize_method(const String& method)
{
    auto lowercase_method = method.to_lowercase();
    if (lowercase_method.is_one_of("delete", "get", "head", "options", "post", "put"))
        return method.to_uppercase();
    return method;
}

// https://fetch.spec.whatwg.org/#concept-header-value-normalize
static String normalize_header_value(const String& header_value)
{
    // FIXME: I'm not sure if this is the right trim, it should only be HTML whitespace bytes.
    return header_value.trim_whitespace();
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-setrequestheader
void XMLHttpRequest::set_request_header(const String& header, const String& value)
{
    if (m_ready_state != ReadyState::Opened) {
        // FIXME: throw an "InvalidStateError" DOMException.
        return;
    }

    if (m_send) {
        // FIXME: throw an "InvalidStateError" DOMException.
        return;
    }

    // FIXME: Check if name matches the name production.
    // FIXME: Check if value matches the value production.

    if (is_forbidden_header_name(header))
        return;

    // FIXME: Combine
    m_request_headers.set(header, normalize_header_value(value));
}

void XMLHttpRequest::open(const String& method, const String& url)
{
    // FIXME: Let settingsObject be this’s relevant settings object.

    // FIXME: If settingsObject has a responsible document and it is not fully active, then throw an "InvalidStateError" DOMException.

    // FIXME: Check that the method matches the method token production. https://tools.ietf.org/html/rfc7230#section-3.1.1

    if (is_forbidden_method(method)) {
        // FIXME: Throw a "SecurityError" DOMException.
        return;
    }

    String normalized_method = normalize_method(method);

    // FIXME: Pass in settingObject's API base URL and API URL character encoding.
    URL parsed_url(url);
    if (!parsed_url.is_valid()) {
        // FIXME: Throw a "SyntaxError" DOMException.
        return;
    }

    if (!parsed_url.host().is_null()) {
        // FIXME: If the username argument is not null, set the username given parsedURL and username.
        // FIXME: If the password argument is not null, set the password given parsedURL and password.
    }

    // FIXME: If async is false, the current global object is a Window object, and either this’s timeout is
    //        not 0 or this’s response type is not the empty string, then throw an "InvalidAccessError" DOMException.

    // FIXME: If the async argument is omitted, set async to true, and set username and password to null.

    // FIXME: Terminate the ongoing fetch operated by the XMLHttpRequest object.

    m_send = false;
    m_upload_listener = false;
    m_method = normalized_method;
    m_url = parsed_url;
    // FIXME: Set this’s synchronous flag if async is false; otherwise unset this’s synchronous flag.
    //        (We're currently defaulting to async)
    m_synchronous = false;
    m_request_headers.clear();

    // FIXME: Set this’s response to a network error.
    // FIXME: Set this’s received bytes to the empty byte sequence.
    m_response_object = {};

    if (m_ready_state != ReadyState::Opened)
        set_ready_state(ReadyState::Opened);
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-send
void XMLHttpRequest::send()
{
    if (m_ready_state != ReadyState::Opened) {
        // FIXME: throw an "InvalidStateError" DOMException.
        return;
    }

    if (m_send) {
        // FIXME: throw an "InvalidStateError" DOMException.
        return;
    }

    // FIXME: If this’s request method is `GET` or `HEAD`, then set body to null.

    // FIXME: If body is not null, then:

    URL request_url = m_window->document().complete_url(m_url.to_string());
    dbgln("XHR send from {} to {}", m_window->document().url(), request_url);

    // TODO: Add support for preflight requests to support CORS requests
    Origin request_url_origin = Origin(request_url.protocol(), request_url.host(), request_url.port());

    if (!m_window->document().origin().is_same(request_url_origin)) {
        dbgln("XHR failed to load: Same-Origin Policy violation: {} may not load {}", m_window->document().url(), request_url);
        auto weak_this = make_weak_ptr();
        if (!weak_this)
            return;
        const_cast<XMLHttpRequest&>(*weak_this).set_ready_state(ReadyState::Done);
        const_cast<XMLHttpRequest&>(*weak_this).dispatch_event(DOM::Event::create(HTML::EventNames::error));
        return;
    }

    LoadRequest request;
    request.set_method(m_method);
    request.set_url(request_url);
    for (auto& it : m_request_headers)
        request.set_header(it.key, it.value);

    m_upload_complete = false;
    m_timed_out = false;

    // FIXME: If req’s body is null (which it always is currently)
    m_upload_complete = true;

    m_send = true;

    if (!m_synchronous) {
        fire_progress_event(EventNames::loadstart, 0, 0);

        // FIXME: If this’s upload complete flag is unset and this’s upload listener flag is set,
        //        then fire a progress event named loadstart at this’s upload object with 0 and req’s body’s total bytes.

        if (m_ready_state != ReadyState::Opened || !m_send)
            return;

        // FIXME: in order to properly set ReadyState::HeadersReceived and ReadyState::Loading,
        // we need to make ResourceLoader give us more detailed updates than just "done" and "error".
        ResourceLoader::the().load(
            request,
            [weak_this = make_weak_ptr()](auto data, auto&) {
                if (!weak_this)
                    return;
                auto& xhr = const_cast<XMLHttpRequest&>(*weak_this);
                auto response_data = ByteBuffer::copy(data);
                // FIXME: There's currently no difference between transmitted and length.
                u64 transmitted = response_data.size();
                u64 length = response_data.size();

                if (!xhr.m_synchronous) {
                    xhr.m_response_object = response_data;
                    xhr.fire_progress_event(EventNames::progress, transmitted, length);
                }

                xhr.m_ready_state = ReadyState::Done;
                xhr.m_send = false;
                xhr.dispatch_event(DOM::Event::create(EventNames::readystatechange));
                xhr.fire_progress_event(EventNames::load, transmitted, length);
                xhr.fire_progress_event(EventNames::loadend, transmitted, length);
            },
            [weak_this = make_weak_ptr()](auto& error) {
                if (!weak_this)
                    return;
                dbgln("XHR failed to load: {}", error);
                const_cast<XMLHttpRequest&>(*weak_this).set_ready_state(ReadyState::Done);
                const_cast<XMLHttpRequest&>(*weak_this).dispatch_event(DOM::Event::create(HTML::EventNames::error));
            });
    } else {
        TODO();
    }
}

bool XMLHttpRequest::dispatch_event(NonnullRefPtr<DOM::Event> event)
{
    return DOM::EventDispatcher::dispatch(*this, move(event));
}

JS::Object* XMLHttpRequest::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

}
