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
    WEB_PLATFORM_OBJECT(WebGLContextEvent, DOM::Event);

public:
    static WebGLContextEvent* create(HTML::Window&, FlyString const& type, WebGLContextEventInit const& event_init);
    static WebGLContextEvent* create_with_global_object(HTML::Window&, FlyString const& type, WebGLContextEventInit const& event_init);

    WebGLContextEvent(HTML::Window&, FlyString const& type, WebGLContextEventInit const& event_init);

    virtual ~WebGLContextEvent() override;

    String const& status_message() const { return m_status_message; }

private:
    String m_status_message { String::empty() };
};

}
