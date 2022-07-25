/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::WebGL {

struct WebGLContextEventInit final : public DOM::EventInit {
    String status_message { String::empty() };
};

class WebGLContextEvent final : public DOM::Event {
public:
    using WrapperType = Bindings::WebGLContextEventWrapper;

    static NonnullRefPtr<WebGLContextEvent> create(FlyString const& type, WebGLContextEventInit const& event_init)
    {
        return adopt_ref(*new WebGLContextEvent(type, event_init));
    }

    static NonnullRefPtr<WebGLContextEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& type, WebGLContextEventInit const& event_init)
    {
        return adopt_ref(*new WebGLContextEvent(type, event_init));
    }

    virtual ~WebGLContextEvent() override = default;

    String const& status_message() const { return m_status_message; }

private:
    WebGLContextEvent(FlyString const& type, WebGLContextEventInit const& event_init)
        : DOM::Event(type, event_init)
        , m_status_message(event_init.status_message)
    {
    }

    String m_status_message { String::empty() };
};

}
