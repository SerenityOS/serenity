/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/MimeSniff/MimeType.h>
#include <LibWeb/XHR/XMLHttpRequestEventTarget.h>

namespace Web::XHR {

class XMLHttpRequest final
    : public RefCounted<XMLHttpRequest>
    , public Weakable<XMLHttpRequest>
    , public XMLHttpRequestEventTarget {
public:
    enum class ReadyState : u16 {
        Unsent = 0,
        Opened = 1,
        HeadersReceived = 2,
        Loading = 3,
        Done = 4,
    };

    using WrapperType = Bindings::XMLHttpRequestWrapper;

    static NonnullRefPtr<XMLHttpRequest> create(DOM::Window& window)
    {
        return adopt_ref(*new XMLHttpRequest(window));
    }
    static NonnullRefPtr<XMLHttpRequest> create_with_global_object(Bindings::WindowObject& window)
    {
        return XMLHttpRequest::create(window.impl());
    }

    virtual ~XMLHttpRequest() override;

    using RefCounted::ref;
    using RefCounted::unref;

    ReadyState ready_state() const { return m_ready_state; };
    unsigned status() const { return m_status; };
    DOM::ExceptionOr<String> response_text() const;
    DOM::ExceptionOr<JS::Value> response();
    Bindings::XMLHttpRequestResponseType response_type() const { return m_response_type; }

    DOM::ExceptionOr<void> open(const String& method, const String& url);
    DOM::ExceptionOr<void> send(String body);

    DOM::ExceptionOr<void> set_request_header(const String& header, const String& value);
    void set_response_type(Bindings::XMLHttpRequestResponseType type) { m_response_type = type; }

    String get_response_header(const String& name) { return m_response_headers.get(name).value_or({}); }
    String get_all_response_headers() const;

    Bindings::CallbackType* onreadystatechange();
    void set_onreadystatechange(Optional<Bindings::CallbackType>);

    DOM::ExceptionOr<void> override_mime_type(String const& mime);

private:
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    void set_ready_state(ReadyState);
    void set_status(unsigned status) { m_status = status; }
    void fire_progress_event(const String&, u64, u64);

    MimeSniff::MimeType get_response_mime_type() const;
    Optional<String> get_final_encoding() const;
    MimeSniff::MimeType get_final_mime_type() const;

    String get_text_response() const;

    Optional<Vector<String>> get_decode_and_split(String const& header_name, HashMap<String, String, CaseInsensitiveStringTraits> const& header_list) const;
    Optional<MimeSniff::MimeType> extract_mime_type(HashMap<String, String, CaseInsensitiveStringTraits> const& header_list) const;

    explicit XMLHttpRequest(DOM::Window&);

    NonnullRefPtr<DOM::Window> m_window;

    ReadyState m_ready_state { ReadyState::Unsent };
    unsigned m_status { 0 };
    bool m_send { false };

    String m_method;
    AK::URL m_url;

    Bindings::XMLHttpRequestResponseType m_response_type;

    HashMap<String, String, CaseInsensitiveStringTraits> m_request_headers;
    HashMap<String, String, CaseInsensitiveStringTraits> m_response_headers;

    bool m_synchronous { false };
    bool m_upload_complete { false };
    bool m_upload_listener { false };
    bool m_timed_out { false };

    ByteBuffer m_received_bytes;

    enum class Failure {
        /// ????
    };
    Variant<JS::Handle<JS::Value>, Failure, Empty> m_response_object;

    // https://xhr.spec.whatwg.org/#override-mime-type
    Optional<MimeSniff::MimeType> m_override_mime_type;
};

}
