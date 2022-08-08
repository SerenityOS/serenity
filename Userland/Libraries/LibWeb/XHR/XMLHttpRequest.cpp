/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/GenericLexer.h>
#include <AK/QuickSort.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/XMLHttpRequestWrapper.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/Fetch/Infrastructure/HTTP.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Methods.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/XHR/EventNames.h>
#include <LibWeb/XHR/ProgressEvent.h>
#include <LibWeb/XHR/XMLHttpRequest.h>

namespace Web::XHR {

XMLHttpRequest::XMLHttpRequest(HTML::Window& window)
    : XMLHttpRequestEventTarget()
    , m_window(window)
    , m_response_type(Bindings::XMLHttpRequestResponseType::Empty)
{
}

XMLHttpRequest::~XMLHttpRequest() = default;

void XMLHttpRequest::set_ready_state(ReadyState ready_state)
{
    m_ready_state = ready_state;
    dispatch_event(*DOM::Event::create(verify_cast<Bindings::WindowObject>(wrapper()->global_object()), EventNames::readystatechange));
}

void XMLHttpRequest::fire_progress_event(String const& event_name, u64 transmitted, u64 length)
{
    ProgressEventInit event_init {};
    event_init.length_computable = true;
    event_init.loaded = transmitted;
    event_init.total = length;
    dispatch_event(*ProgressEvent::create(verify_cast<Bindings::WindowObject>(wrapper()->global_object()), event_name, event_init));
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
    auto& vm = wrapper()->vm();
    auto& realm = *vm.current_realm();

    // 1. If this’s response type is the empty string or "text", then:
    if (m_response_type == Bindings::XMLHttpRequestResponseType::Empty || m_response_type == Bindings::XMLHttpRequestResponseType::Text) {
        // 1. If this’s state is not loading or done, then return the empty string.
        if (m_ready_state != ReadyState::Loading && m_ready_state != ReadyState::Done)
            return JS::Value(JS::js_string(vm, ""));

        // 2. Return the result of getting a text response for this.
        return JS::Value(JS::js_string(vm, get_text_response()));
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
        auto buffer_result = JS::ArrayBuffer::create(realm, m_received_bytes.size());
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
        auto blob_part = TRY_OR_RETURN_OOM(try_make_ref_counted<FileAPI::Blob>(m_received_bytes, get_final_mime_type().type()));
        auto blob = TRY(FileAPI::Blob::create(Vector<FileAPI::BlobPart> { move(blob_part) }));
        m_response_object = JS::make_handle(JS::Value(blob->create_wrapper(realm)));
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

        auto json_object_result = JS::call(vm, realm.intrinsics().json_parse_function(), JS::js_undefined(), JS::js_string(vm, decoder.to_utf8({ m_received_bytes.data(), m_received_bytes.size() })));
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

        return mime_type.subtype().ends_with("+xml"sv);
    };

    // 3. If xhr’s response type is the empty string, charset is null, and the result of get a final MIME type for xhr is an XML MIME type,
    if (m_response_type == Bindings::XMLHttpRequestResponseType::Empty && !charset.has_value() && is_xml_mime_type(get_final_mime_type())) {
        // FIXME: then use the rules set forth in the XML specifications to determine the encoding. Let charset be the determined encoding. [XML] [XML-NAMES]
    }

    // 4. If charset is null, then set charset to UTF-8.
    if (!charset.has_value())
        charset = "UTF-8"sv;

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
    // FIXME: Use an actual HeaderList for XHR headers.
    Fetch::Infrastructure::HeaderList header_list;
    for (auto const& entry : m_response_headers) {
        auto header = Fetch::Infrastructure::Header {
            .name = MUST(ByteBuffer::copy(entry.key.bytes())),
            .value = MUST(ByteBuffer::copy(entry.value.bytes())),
        };
        MUST(header_list.append(move(header)));
    }

    // 1. Let mimeType be the result of extracting a MIME type from xhr’s response’s header list.
    auto mime_type = header_list.extract_mime_type();

    // 2. If mimeType is failure, then set mimeType to text/xml.
    if (!mime_type.has_value())
        return MimeSniff::MimeType("text"sv, "xml"sv);

    // 3. Return mimeType.
    return mime_type.release_value();
}

// https://xhr.spec.whatwg.org/#final-charset
Optional<StringView> XMLHttpRequest::get_final_encoding() const
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

// https://fetch.spec.whatwg.org/#concept-bodyinit-extract
// FIXME: The parameter 'body_init' should be 'typedef (ReadableStream or XMLHttpRequestBodyInit) BodyInit'. For now we just let it be 'XMLHttpRequestBodyInit'.
static ErrorOr<Fetch::Infrastructure::BodyWithType> extract_body(XMLHttpRequestBodyInit const& body_init)
{
    // FIXME: 1. Let stream be object if object is a ReadableStream object. Otherwise, let stream be a new ReadableStream, and set up stream.
    Fetch::Infrastructure::Body::ReadableStreamDummy stream {};
    // FIXME: 2. Let action be null.
    // 3. Let source be null.
    Fetch::Infrastructure::Body::SourceType source {};
    // 4. Let length be null.
    Optional<u64> length {};
    // 5. Let type be null.
    Optional<ByteBuffer> type {};

    // 6. Switch on object.
    // FIXME: Still need to support BufferSource and FormData
    TRY(body_init.visit(
        [&](NonnullRefPtr<FileAPI::Blob> const& blob) -> ErrorOr<void> {
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
        [&](NonnullRefPtr<URL::URLSearchParams> const& url_search_params) -> ErrorOr<void> {
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
    auto body = Fetch::Infrastructure::Body { move(stream), move(source), move(length) };
    // 10. Return (body, type).
    return Fetch::Infrastructure::BodyWithType { .body = move(body), .type = move(type) };
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-setrequestheader
DOM::ExceptionOr<void> XMLHttpRequest::set_request_header(String const& name_string, String const& value_string)
{
    auto name = name_string.to_byte_buffer();
    auto value = value_string.to_byte_buffer();

    // 1. If this’s state is not opened, then throw an "InvalidStateError" DOMException.
    if (m_ready_state != ReadyState::Opened)
        return DOM::InvalidStateError::create("XHR readyState is not OPENED");

    // 2. If this’s send() flag is set, then throw an "InvalidStateError" DOMException.
    if (m_send)
        return DOM::InvalidStateError::create("XHR send() flag is already set");

    // 3. Normalize value.
    value = MUST(Fetch::Infrastructure::normalize_header_value(value));

    // 4. If name is not a header name or value is not a header value, then throw a "SyntaxError" DOMException.
    if (!Fetch::Infrastructure::is_header_name(name))
        return DOM::SyntaxError::create("Header name contains invalid characters.");
    if (!Fetch::Infrastructure::is_header_value(value))
        return DOM::SyntaxError::create("Header value contains invalid characters.");

    // 5. If name is a forbidden header name, then return.
    if (Fetch::Infrastructure::is_forbidden_header_name(name))
        return {};

    // 6. Combine (name, value) in this’s author request headers.
    // FIXME: The header name look-up should be case-insensitive.
    // FIXME: Headers should be stored as raw byte sequences, not Strings.
    if (m_request_headers.contains(StringView { name })) {
        // 1. If list contains name, then set the value of the first such header to its value,
        //    followed by 0x2C 0x20, followed by value.
        auto maybe_header_value = m_request_headers.get(StringView { name });
        m_request_headers.set(StringView { name }, String::formatted("{}, {}", maybe_header_value.release_value(), StringView { name }));
    } else {
        // 2. Otherwise, append (name, value) to list.
        m_request_headers.set(StringView { name }, StringView { value });
    }

    return {};
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-open
DOM::ExceptionOr<void> XMLHttpRequest::open(String const& method_string, String const& url)
{
    // 8. If the async argument is omitted, set async to true, and set username and password to null.
    return open(method_string, url, true, {}, {});
}

DOM::ExceptionOr<void> XMLHttpRequest::open(String const& method_string, String const& url, bool async, String const& username, String const& password)
{
    auto method = method_string.to_byte_buffer();

    // 1. Let settingsObject be this’s relevant settings object.
    auto& settings_object = m_window->associated_document().relevant_settings_object();

    // 2. If settingsObject has a responsible document and it is not fully active, then throw an "InvalidStateError" DOMException.
    if (!settings_object.responsible_document().is_null() && !settings_object.responsible_document()->is_active())
        return DOM::InvalidStateError::create("Invalid state: Responsible document is not fully active.");

    // 3. If method is not a method, then throw a "SyntaxError" DOMException.
    if (!Fetch::Infrastructure::is_method(method))
        return DOM::SyntaxError::create("An invalid or illegal string was specified.");

    // 4. If method is a forbidden method, then throw a "SecurityError" DOMException.
    if (Fetch::Infrastructure::is_forbidden_method(method))
        return DOM::SecurityError::create("Forbidden method, must not be 'CONNECT', 'TRACE', or 'TRACK'");

    // 5. Normalize method.
    method = MUST(Fetch::Infrastructure::normalize_method(method));

    // 6. Let parsedURL be the result of parsing url with settingsObject’s API base URL and settingsObject’s API URL character encoding.
    auto parsed_url = settings_object.api_base_url().complete_url(url);

    // 7. If parsedURL is failure, then throw a "SyntaxError" DOMException.
    if (!parsed_url.is_valid())
        return DOM::SyntaxError::create("Invalid URL");

    // 8. If the async argument is omitted, set async to true, and set username and password to null.
    // NOTE: This is handled in the overload lacking the async argument.

    // 9. If parsedURL’s host is non-null, then:
    if (!parsed_url.host().is_null()) {
        // 1. If the username argument is not null, set the username given parsedURL and username.
        if (!username.is_null())
            parsed_url.set_username(username);
        // 2. If the password argument is not null, set the password given parsedURL and password.
        if (!password.is_null())
            parsed_url.set_password(password);
    }

    // FIXME: 10. If async is false, the current global object is a Window object, and either this’s timeout is
    //        not 0 or this’s response type is not the empty string, then throw an "InvalidAccessError" DOMException.

    // FIXME: 11. Terminate the ongoing fetch operated by the XMLHttpRequest object.

    // 12. Set variables associated with the object as follows:
    // Unset this’s send() flag.
    m_send = false;
    // Unset this’s upload listener flag.
    m_upload_listener = false;
    // Set this’s request method to method.
    m_method = move(method);
    // Set this’s request URL to parsedURL.
    m_url = parsed_url;
    // Set this’s synchronous flag if async is false; otherwise unset this’s synchronous flag.
    m_synchronous = !async;
    // Empty this’s author request headers.
    m_request_headers.clear();
    // FIXME: Set this’s response to a network error.
    // Set this’s received bytes to the empty byte sequence.
    m_received_bytes = {};
    // Set this’s response object to null.
    m_response_object = {};

    // 13. If this’s state is not opened, then:
    if (m_ready_state != ReadyState::Opened) {
        // 1. Set this’s state to opened.
        // 2. Fire an event named readystatechange at this.
        set_ready_state(ReadyState::Opened);
    }

    return {};
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-send
DOM::ExceptionOr<void> XMLHttpRequest::send(Optional<XMLHttpRequestBodyInit> body)
{
    if (m_ready_state != ReadyState::Opened)
        return DOM::InvalidStateError::create("XHR readyState is not OPENED");

    if (m_send)
        return DOM::InvalidStateError::create("XHR send() flag is already set");

    // If this’s request method is `GET` or `HEAD`, then set body to null.
    if (m_method.is_one_of("GET"sv, "HEAD"sv))
        body = {};

    auto body_with_type = body.has_value() ? TRY_OR_RETURN_OOM(extract_body(body.value())) : Optional<Fetch::Infrastructure::BodyWithType> {};

    AK::URL request_url = m_window->associated_document().parse_url(m_url.to_string());
    dbgln("XHR send from {} to {}", m_window->associated_document().url(), request_url);

    // TODO: Add support for preflight requests to support CORS requests
    auto request_url_origin = HTML::Origin(request_url.protocol(), request_url.host(), request_url.port_or_default());

    bool should_enforce_same_origin_policy = true;
    if (auto* page = m_window->page())
        should_enforce_same_origin_policy = page->is_same_origin_policy_enabled();

    if (should_enforce_same_origin_policy && !m_window->associated_document().origin().is_same_origin(request_url_origin)) {
        dbgln("XHR failed to load: Same-Origin Policy violation: {} may not load {}", m_window->associated_document().url(), request_url);
        set_ready_state(ReadyState::Done);
        dispatch_event(*DOM::Event::create(verify_cast<Bindings::WindowObject>(wrapper()->global_object()), HTML::EventNames::error));
        return {};
    }

    auto request = LoadRequest::create_for_url_on_page(request_url, m_window->page());
    request.set_method(m_method);
    if (body_with_type.has_value()) {
        TRY_OR_RETURN_OOM(body_with_type->body.source().visit(
            [&](ByteBuffer const& buffer) -> ErrorOr<void> {
                request.set_body(buffer);
                return {};
            },
            [&](NonnullRefPtr<FileAPI::Blob> const& blob) -> ErrorOr<void> {
                auto byte_buffer = TRY(ByteBuffer::copy(blob->bytes()));
                request.set_body(byte_buffer);
                return {};
            },
            [](auto&) -> ErrorOr<void> {
                return {};
            }));
        if (body_with_type->type.has_value())
            request.set_header("Content-Type", String { body_with_type->type->span() });
    }
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
        // FIXME: In the Fetch spec, which XHR gets its definition of `status` from, the status code is 0-999.
        //        We could clamp, wrap around (current browser behavior!), or error out.
        //        See: https://github.com/whatwg/fetch/issues/1142
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
                xhr.dispatch_event(*DOM::Event::create(verify_cast<Bindings::WindowObject>(xhr.wrapper()->global_object()), EventNames::readystatechange));
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
                xhr.dispatch_event(*DOM::Event::create(verify_cast<Bindings::WindowObject>(xhr.wrapper()->global_object()), HTML::EventNames::error));
            },
            m_timeout,
            [weak_this = make_weak_ptr()] {
                auto strong_this = weak_this.strong_ref();
                if (!strong_this)
                    return;
                auto& xhr = const_cast<XMLHttpRequest&>(*strong_this);
                xhr.dispatch_event(*DOM::Event::create(verify_cast<Bindings::WindowObject>(xhr.wrapper()->global_object()), EventNames::timeout));
            });
    } else {
        TODO();
    }
    return {};
}

JS::Object* XMLHttpRequest::create_wrapper(JS::Realm& realm)
{
    return wrap(realm, *this);
}

Bindings::CallbackType* XMLHttpRequest::onreadystatechange()
{
    return event_handler_attribute(Web::XHR::EventNames::readystatechange);
}

void XMLHttpRequest::set_onreadystatechange(Bindings::CallbackType* value)
{
    set_event_handler_attribute(Web::XHR::EventNames::readystatechange, value);
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
        builder.append(": "sv);
        builder.append(m_response_headers.get(key).value());
        builder.append("\r\n"sv);
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

// https://xhr.spec.whatwg.org/#ref-for-dom-xmlhttprequest-timeout%E2%91%A2
DOM::ExceptionOr<void> XMLHttpRequest::set_timeout(u32 timeout)
{
    // 1. If the current global object is a Window object and this’s synchronous flag is set,
    //    then throw an "InvalidAccessError" DOMException.
    if (is<Bindings::WindowObject>(HTML::current_global_object()) && m_synchronous)
        return DOM::InvalidAccessError::create("Use of XMLHttpRequest's timeout attribute is not supported in the synchronous mode in window context.");

    // 2. Set this’s timeout to the given value.
    m_timeout = timeout;

    return {};
}

// https://xhr.spec.whatwg.org/#dom-xmlhttprequest-timeout
u32 XMLHttpRequest::timeout() const { return m_timeout; }

}
