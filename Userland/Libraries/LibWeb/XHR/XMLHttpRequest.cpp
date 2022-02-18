/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <AK/QuickSort.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Fetch/AbstractOperations.h>
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
    : XMLHttpRequestEventTarget()
    , m_window(window)
    , m_response_type(Bindings::XMLHttpRequestResponseType::Empty)
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

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-responsetext
DOM::ExceptionOr<String> XMLHttpRequest::response_text() const
{
    // 1. If this’s response type is not the empty string or "text", then throw an "InvalidStateError" DOMException.
    if (m_response_type != Bindings::XMLHttpRequestResponseType::Empty && m_response_type != Bindings::XMLHttpRequestResponseType::Text)
        return DOM::InvalidStateError::create("XHR responseText can only be used for responseType \"\" or \"text\"");

    // 2. If this’s state is not loading or done, then return the empty string.
    if (m_ready_state != ReadyState::Loading && m_ready_state != ReadyState::Done)
        return String::empty();

    return get_text_response();
}

// https://xhr.spec.whatwg.org/#response
DOM::ExceptionOr<JS::Value> XMLHttpRequest::response()
{
    auto& global_object = wrapper()->global_object();

    // 1. If this’s response type is the empty string or "text", then:
    if (m_response_type == Bindings::XMLHttpRequestResponseType::Empty || m_response_type == Bindings::XMLHttpRequestResponseType::Text) {
        // 1. If this’s state is not loading or done, then return the empty string.
        if (m_ready_state != ReadyState::Loading && m_ready_state != ReadyState::Done)
            return JS::Value(JS::js_string(global_object.heap(), ""));

        // 2. Return the result of getting a text response for this.
        return JS::Value(JS::js_string(global_object.heap(), get_text_response()));
    }
    // 2. If this’s state is not done, then return null.
    if (m_ready_state != ReadyState::Done)
        return JS::js_null();

    // 3. If this’s response object is failure, then return null.
    if (m_response_object.has<Failure>())
        return JS::js_null();

    // 4. If this’s response object is non-null, then return it.
    if (!m_response_object.has<Empty>())
        return m_response_object.get<JS::Handle<JS::Value>>().value();

    // 5. If this’s response type is "arraybuffer",
    if (m_response_type == Bindings::XMLHttpRequestResponseType::Arraybuffer) {
        // then set this’s response object to a new ArrayBuffer object representing this’s received bytes. If this throws an exception, then set this’s response object to failure and return null.
        auto buffer_result = JS::ArrayBuffer::create(global_object, m_received_bytes.size());
        if (buffer_result.is_error()) {
            m_response_object = Failure();
            return JS::js_null();
        }

        auto buffer = buffer_result.release_value();
        buffer->buffer().overwrite(0, m_received_bytes.data(), m_received_bytes.size());
        m_response_object = JS::make_handle(JS::Value(buffer));
    }
    // 6. Otherwise, if this’s response type is "blob", set this’s response object to a new Blob object representing this’s received bytes with type set to the result of get a final MIME type for this.
    else if (m_response_type == Bindings::XMLHttpRequestResponseType::Blob) {
        // FIXME: Implement this once we have 'Blob'.
        return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, "XHR Blob type not implemented" };
    }
    // 7. Otherwise, if this’s response type is "document", set a document response for this.
    else if (m_response_type == Bindings::XMLHttpRequestResponseType::Document) {
        // FIXME: Implement this.
        return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, "XHR Document type not implemented" };
    }
    // 8. Otherwise:
    else {
        // 1. Assert: this’s response type is "json".
        // Note: Automatically done by the layers above us.

        // 2. If this’s response’s body is null, then return null.
        // FIXME: Implement this once we have 'Response'.
        if (m_received_bytes.is_empty())
            return JS::Value(JS::js_null());

        // 3. Let jsonObject be the result of running parse JSON from bytes on this’s received bytes. If that threw an exception, then return null.
        TextCodec::UTF8Decoder decoder;

        auto json_object_result = JS::call(global_object, global_object.json_parse_function(), JS::js_undefined(), JS::js_string(global_object.heap(), decoder.to_utf8({ m_received_bytes.data(), m_received_bytes.size() })));
        if (json_object_result.is_error())
            return JS::Value(JS::js_null());

        // 4. Set this’s response object to jsonObject.
        m_response_object = JS::make_handle(json_object_result.release_value());
    }

    // 9. Return this’s response object.
    return m_response_object.get<JS::Handle<JS::Value>>().value();
}

// https://xhr.spec.whatwg.org/#text-response
String XMLHttpRequest::get_text_response() const
{
    // FIXME: 1. If xhr’s response’s body is null, then return the empty string.

    // 2. Let charset be the result of get a final encoding for xhr.
    auto charset = get_final_encoding();

    auto is_xml_mime_type = [](MimeSniff::MimeType const& mime_type) {
        // An XML MIME type is any MIME type whose subtype ends in "+xml" or whose essence is "text/xml" or "application/xml". [RFC7303]
        if (mime_type.essence().is_one_of("text/xml"sv, "application/xml"sv))
            return true;

        return mime_type.subtype().ends_with("+xml");
    };

    // 3. If xhr’s response type is the empty string, charset is null, and the result of get a final MIME type for xhr is an XML MIME type,
    if (m_response_type == Bindings::XMLHttpRequestResponseType::Empty && !charset.has_value() && is_xml_mime_type(get_final_mime_type())) {
        // FIXME: then use the rules set forth in the XML specifications to determine the encoding. Let charset be the determined encoding. [XML] [XML-NAMES]
    }

    // 4. If charset is null, then set charset to UTF-8.
    if (!charset.has_value())
        charset = "UTF-8";

    // 5. Return the result of running decode on xhr’s received bytes using fallback encoding charset.
    auto* decoder = TextCodec::decoder_for(charset.value());

    // If we don't support the decoder yet, let's crash instead of attempting to return something, as the result would be incorrect and create obscure bugs.
    VERIFY(decoder);

    return TextCodec::convert_input_to_utf8_using_given_decoder_unless_there_is_a_byte_order_mark(*decoder, m_received_bytes);
}

// https://xhr.spec.whatwg.org/#final-mime-type
MimeSniff::MimeType XMLHttpRequest::get_final_mime_type() const
{
    // 1. If xhr’s override MIME type is null, return the result of get a response MIME type for xhr.
    if (!m_override_mime_type.has_value())
        return get_response_mime_type();

    // 2. Return xhr’s override MIME type.
    return *m_override_mime_type;
}

// https://xhr.spec.whatwg.org/#response-mime-type
MimeSniff::MimeType XMLHttpRequest::get_response_mime_type() const
{
    // 1. Let mimeType be the result of extracting a MIME type from xhr’s response’s header list.
    auto mime_type = extract_mime_type(m_response_headers);

    // 2. If mimeType is failure, then set mimeType to text/xml.
    if (!mime_type.has_value())
        return MimeSniff::MimeType("text"sv, "xml"sv);

    // 3. Return mimeType.
    return mime_type.release_value();
}

// https://xhr.spec.whatwg.org/#final-charset
Optional<String> XMLHttpRequest::get_final_encoding() const
{
    // 1. Let label be null.
    Optional<String> label;

    // 2. Let responseMIME be the result of get a response MIME type for xhr.
    auto response_mime = get_response_mime_type();

    // 3. If responseMIME’s parameters["charset"] exists, then set label to it.
    auto response_mime_charset_it = response_mime.parameters().find("charset"sv);
    if (response_mime_charset_it != response_mime.parameters().end())
        label = response_mime_charset_it->value;

    // 4. If xhr’s override MIME type’s parameters["charset"] exists, then set label to it.
    if (m_override_mime_type.has_value()) {
        auto override_mime_charset_it = m_override_mime_type->parameters().find("charset"sv);
        if (override_mime_charset_it != m_override_mime_type->parameters().end())
            label = override_mime_charset_it->value;
    }

    // 5. If label is null, then return null.
    if (!label.has_value())
        return {};

    // 6. Let encoding be the result of getting an encoding from label.
    auto encoding = TextCodec::get_standardized_encoding(label.value());

    // 7. If encoding is failure, then return null.
    // 8. Return encoding.
    return encoding;
}

// https://fetch.spec.whatwg.org/#concept-header-list-get-decode-split
// FIXME: This is not only used by XHR, it is also used for multiple things in Fetch.
Optional<Vector<String>> XMLHttpRequest::get_decode_and_split(String const& header_name, HashMap<String, String, CaseInsensitiveStringTraits> const& header_list) const
{
    // 1. Let initialValue be the result of getting name from list.
    auto initial_value_iterator = header_list.find(header_name);

    // 2. If initialValue is null, then return null.
    if (initial_value_iterator == header_list.end())
        return {};

    auto& initial_value = initial_value_iterator->value;

    // FIXME: 3. Let input be the result of isomorphic decoding initialValue.
    // NOTE: We don't store raw byte sequences in the header list as per the spec, so we can't do this step.
    //       The spec no longer uses initialValue after this step. For our purposes, treat any reference to `input` in the spec comments to initial_value.

    // 4. Let position be a position variable for input, initially pointing at the start of input.
    GenericLexer lexer(initial_value);

    // 5. Let values be a list of strings, initially empty.
    Vector<String> values;

    // 6. Let value be the empty string.
    StringBuilder value;

    // 7. While position is not past the end of input:
    while (!lexer.is_eof()) {
        // 1. Append the result of collecting a sequence of code points that are not U+0022 (") or U+002C (,) from input, given position, to value.
        auto value_part = lexer.consume_until([](char ch) {
            return ch == '"' || ch == ',';
        });
        value.append(value_part);

        // 2. If position is not past the end of input, then:
        if (!lexer.is_eof()) {
            // 1. If the code point at position within input is U+0022 ("), then:
            if (lexer.peek() == '"') {
                // 1. Append the result of collecting an HTTP quoted string from input, given position, to value.
                auto quoted_value_part = Fetch::collect_an_http_quoted_string(lexer, Fetch::HttpQuotedStringExtractValue::No);
                value.append(quoted_value_part);

                // 2. If position is not past the end of input, then continue.
                if (!lexer.is_eof())
                    continue;
            }

            // 2. Otherwise:
            else {
                // 1. Assert: the code point at position within input is U+002C (,).
                VERIFY(lexer.peek() == ',');

                // 2. Advance position by 1.
                lexer.ignore(1);
            }
        }

        // 3. Remove all HTTP tab or space from the start and end of value.
        // https://fetch.spec.whatwg.org/#http-tab-or-space
        // An HTTP tab or space is U+0009 TAB or U+0020 SPACE.
        auto trimmed_value = value.to_string().trim("\t ", TrimMode::Both);

        // 4. Append value to values.
        values.append(move(trimmed_value));

        // 5. Set value to the empty string.
        value.clear();
    }

    // 8. Return values.
    return values;
}

// https://fetch.spec.whatwg.org/#concept-header-extract-mime-type
// FIXME: This is not only used by XHR, it is also used for multiple things in Fetch.
Optional<MimeSniff::MimeType> XMLHttpRequest::extract_mime_type(HashMap<String, String, CaseInsensitiveStringTraits> const& header_list) const
{
    // 1. Let charset be null.
    Optional<String> charset;

    // 2. Let essence be null.
    Optional<String> essence;

    // 3. Let mimeType be null.
    Optional<MimeSniff::MimeType> mime_type;

    // 4. Let values be the result of getting, decoding, and splitting `Content-Type` from headers.
    auto potentially_values = get_decode_and_split("Content-Type"sv, header_list);

    // 5. If values is null, then return failure.
    if (!potentially_values.has_value())
        return {};

    auto values = potentially_values.release_value();

    // 6. For each value of values:
    for (auto& value : values) {
        // 1. Let temporaryMimeType be the result of parsing value.
        auto temporary_mime_type = MimeSniff::MimeType::from_string(value);

        // 2. If temporaryMimeType is failure or its essence is "*/*", then continue.
        if (!temporary_mime_type.has_value() || temporary_mime_type->essence() == "*/*"sv)
            continue;

        // 3. Set mimeType to temporaryMimeType.
        mime_type = temporary_mime_type;

        // 4. If mimeType’s essence is not essence, then:
        if (mime_type->essence() != essence) {
            // 1. Set charset to null.
            charset = {};

            // 2. If mimeType’s parameters["charset"] exists, then set charset to mimeType’s parameters["charset"].
            auto charset_it = mime_type->parameters().find("charset"sv);
            if (charset_it != mime_type->parameters().end())
                charset = charset_it->value;

            // 3. Set essence to mimeType’s essence.
            essence = mime_type->essence();
        } else {
            // 5. Otherwise, if mimeType’s parameters["charset"] does not exist, and charset is non-null, set mimeType’s parameters["charset"] to charset.
            if (!mime_type->parameters().contains("charset"sv) && charset.has_value())
                mime_type->set_parameter("charset"sv, charset.value());
        }
    }

    // 7. If mimeType is null, then return failure.
    // 8. Return mimeType.
    return mime_type;
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

    if (should_enforce_same_origin_policy && !m_window->associated_document().origin().is_same_origin(request_url_origin)) {
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
                    xhr.m_received_bytes = response_data;
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

Bindings::CallbackType* XMLHttpRequest::onreadystatechange()
{
    return event_handler_attribute(Web::XHR::EventNames::readystatechange);
}

void XMLHttpRequest::set_onreadystatechange(Optional<Bindings::CallbackType> value)
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

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-overridemimetype
DOM::ExceptionOr<void> XMLHttpRequest::override_mime_type(String const& mime)
{
    // 1. If this’s state is loading or done, then throw an "InvalidStateError" DOMException.
    if (m_ready_state == ReadyState::Loading || m_ready_state == ReadyState::Done)
        return DOM::InvalidStateError::create("Cannot override MIME type when state is Loading or Done.");

    // 2. Set this’s override MIME type to the result of parsing mime.
    m_override_mime_type = MimeSniff::MimeType::from_string(mime);

    // 3. If this’s override MIME type is failure, then set this’s override MIME type to application/octet-stream.
    if (!m_override_mime_type.has_value())
        m_override_mime_type = MimeSniff::MimeType("application"sv, "octet-stream"sv);

    return {};
}
}
