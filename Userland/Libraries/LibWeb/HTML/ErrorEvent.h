/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/webappapis.html#erroreventinit
struct ErrorEventInit : public DOM::EventInit {
    String message { "" };
    String filename { "" }; // FIXME: This should be a USVString.
    u32 lineno { 0 };
    u32 colno { 0 };
    JS::Value error { JS::js_null() };
};

// https://html.spec.whatwg.org/multipage/webappapis.html#errorevent
class ErrorEvent final : public DOM::Event {
public:
    using WrapperType = Bindings::ErrorEventWrapper;

    static NonnullRefPtr<ErrorEvent> create(FlyString const& event_name, ErrorEventInit const& event_init = {})
    {
        return adopt_ref(*new ErrorEvent(event_name, event_init));
    }

    static NonnullRefPtr<ErrorEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, ErrorEventInit const& event_init)
    {
        return ErrorEvent::create(event_name, event_init);
    }

    virtual ~ErrorEvent() override = default;

    // https://html.spec.whatwg.org/multipage/webappapis.html#dom-errorevent-message
    String const& message() const { return m_message; }

    // https://html.spec.whatwg.org/multipage/webappapis.html#dom-errorevent-filename
    String const& filename() const { return m_filename; }

    // https://html.spec.whatwg.org/multipage/webappapis.html#dom-errorevent-lineno
    u32 lineno() const { return m_lineno; }

    // https://html.spec.whatwg.org/multipage/webappapis.html#dom-errorevent-colno
    u32 colno() const { return m_colno; }

    // https://html.spec.whatwg.org/multipage/webappapis.html#dom-errorevent-error
    JS::Value error() const { return m_error.value(); }

private:
    ErrorEvent(FlyString const& event_name, ErrorEventInit const& event_init)
        : DOM::Event(event_name)
        , m_message(event_init.message)
        , m_filename(event_init.filename)
        , m_lineno(event_init.lineno)
        , m_colno(event_init.colno)
        , m_error(JS::make_handle(event_init.error))
    {
    }

    String m_message { "" };
    String m_filename { "" }; // FIXME: This should be a USVString.
    u32 m_lineno { 0 };
    u32 m_colno { 0 };
    JS::Handle<JS::Value> m_error;
};

}
