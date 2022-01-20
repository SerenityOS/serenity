/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/XHR/EventNames.h>
#include <LibWeb/XHR/ProgressEvent.h>
#include <LibWeb/XHR/XMLHttpRequest.h>

namespace Web::XHR {

XMLHttpRequest::XMLHttpRequest(DOM::Window& window)
    : XMLHttpRequestEventTarget(static_cast<Bindings::ScriptExecutionContext&>(window.associated_document()))
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
    ProgressEventInit event_init {};
    event_init.length_computable = true;
    event_init.loaded = transmitted;
    event_init.total = length;
    dispatch_event(ProgressEvent::create(event_name, event_init));
}

String XMLHttpRequest::response_text() const
{
    if (m_response_object.is_empty())
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
DOM::ExceptionOr<void> XMLHttpRequest::set_request_header(const String& header, const String& value)
{
    if (m_ready_state != ReadyState::Opened)
        return DOM::InvalidStateError::create("XHR readyState is not OPENED");

    if (m_send)
        return DOM::InvalidStateError::create("XHR send() flag is already set");

    // FIXME: Check if name matches the name production.
    // FIXME: Check if value matches the value production.

    if (is_forbidden_header_name(header))
        return {};

    // FIXME: Combine
    m_request_headers.set(header, normalize_header_value(value));
    return {};
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-open
DOM::ExceptionOr<void> XMLHttpRequest::open(const String& method, const String& url)
{
    // FIXME: Let settingsObject be this’s relevant settings object.

    // FIXME: If settingsObject has a responsible document and it is not fully active, then throw an "InvalidStateError" DOMException.

    // FIXME: Check that the method matches the method token production. https://tools.ietf.org/html/rfc7230#section-3.1.1

    if (is_forbidden_method(method))
        return DOM::SecurityError::create("Forbidden method, must not be 'CONNECT', 'TRACE', or 'TRACK'");

    auto normalized_method = normalize_method(method);

    auto parsed_url = m_window->associated_document().parse_url(url);
    if (!parsed_url.is_valid())
        return DOM::SyntaxError::create("Invalid URL");

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
    return {};
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-send
DOM::ExceptionOr<void> XMLHttpRequest::send(String body)
{
    if (m_ready_state != ReadyState::Opened)
        return DOM::InvalidStateError::create("XHR readyState is not OPENED");

    if (m_send)
        return DOM::InvalidStateError::create("XHR send() flag is already set");

    // If this’s request method is `GET` or `HEAD`, then set body to null.
    if (m_method.is_one_of("GET"sv, "HEAD"sv))
        body = {};

    AK::URL request_url = m_window->associated_document().parse_url(m_url.to_string());
    dbgln("XHR send from {} to {}", m_window->associated_document().url(), request_url);

    // TODO: Add support for preflight requests to support CORS requests
    Origin request_url_origin = Origin(request_url.protocol(), request_url.host(), request_url.port_or_default());

    bool should_enforce_same_origin_policy = true;
    if (auto* page = m_window->page())
        should_enforce_same_origin_policy = page->is_same_origin_policy_enabled();

    if (should_enforce_same_origin_policy && !m_window->associated_document().origin().is_same(request_url_origin)) {
        dbgln("XHR failed to load: Same-Origin Policy violation: {} may not load {}", m_window->associated_document().url(), request_url);
        set_ready_state(ReadyState::Done);
        dispatch_event(DOM::Event::create(HTML::EventNames::error));
        return {};
    }

    auto request = LoadRequest::create_for_url_on_page(request_url, m_window->page());
    request.set_method(m_method);
    if (!body.is_null())
        request.set_body(body.to_byte_buffer());
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
            return {};

        // FIXME: in order to properly set ReadyState::HeadersReceived and ReadyState::Loading,
        // we need to make ResourceLoader give us more detailed updates than just "done" and "error".
        ResourceLoader::the().load(
            request,
            [weak_this = make_weak_ptr()](auto data, auto& response_headers, auto status_code) {
                auto strong_this = weak_this.strong_ref();
                if (!strong_this)
                    return;
                auto& xhr = const_cast<XMLHttpRequest&>(*weak_this);
                // FIXME: Handle OOM failure.
                auto response_data = ByteBuffer::copy(data).release_value_but_fixme_should_propagate_errors();
                // FIXME: There's currently no difference between transmitted and length.
                u64 transmitted = response_data.size();
                u64 length = response_data.size();

                if (!xhr.m_synchronous) {
                    xhr.m_response_object = response_data;
                    xhr.fire_progress_event(EventNames::progress, transmitted, length);
                }

                xhr.m_ready_state = ReadyState::Done;
                xhr.m_status = status_code.value_or(0);
                xhr.m_response_headers = move(response_headers);
                xhr.m_send = false;
                xhr.dispatch_event(DOM::Event::create(EventNames::readystatechange));
                xhr.fire_progress_event(EventNames::load, transmitted, length);
                xhr.fire_progress_event(EventNames::loadend, transmitted, length);
            },
            [weak_this = make_weak_ptr()](auto& error, auto status_code) {
                dbgln("XHR failed to load: {}", error);
                auto strong_this = weak_this.strong_ref();
                if (!strong_this)
                    return;
                auto& xhr = const_cast<XMLHttpRequest&>(*strong_this);
                xhr.set_ready_state(ReadyState::Done);
                xhr.set_status(status_code.value_or(0));
                xhr.dispatch_event(DOM::Event::create(HTML::EventNames::error));
            });
    } else {
        TODO();
    }
    return {};
}

JS::Object* XMLHttpRequest::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

HTML::EventHandler XMLHttpRequest::onreadystatechange()
{
    return event_handler_attribute(Web::XHR::EventNames::readystatechange);
}

void XMLHttpRequest::set_onreadystatechange(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::readystatechange, move(value));
}

// https://xhr.spec.whatwg.org/#the-getallresponseheaders()-method
String XMLHttpRequest::get_all_response_headers() const
{
    // FIXME: Implement the spec-compliant sort order.

    StringBuilder builder;
    auto keys = m_response_headers.keys();
    quick_sort(keys);

    for (auto& key : keys) {
        builder.append(key);
        builder.append(": ");
        builder.append(m_response_headers.get(key).value());
        builder.append("\r\n");
    }
    return builder.to_string();
}

}
