/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
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
    JS_OBJECT(WebGLContextEvent, DOM::Event);

public:
    static WebGLContextEvent* create(Bindings::WindowObject&, FlyString const& type, WebGLContextEventInit const& event_init);
    static WebGLContextEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& type, WebGLContextEventInit const& event_init);

    WebGLContextEvent(Bindings::WindowObject&, FlyString const& type, WebGLContextEventInit const& event_init);

    virtual ~WebGLContextEvent() override;

    WebGLContextEvent& impl() { return *this; }

    String const& status_message() const { return m_status_message; }

private:
    String m_status_message { String::empty() };
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::WebGL::WebGLContextEvent& object) { return &object; }
using WebGLContextEventWrapper = Web::WebGL::WebGLContextEvent;
}
