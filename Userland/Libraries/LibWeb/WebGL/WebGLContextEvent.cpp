/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebGL/WebGLContextEvent.h>

namespace Web::WebGL {

WebGLContextEvent* WebGLContextEvent::create(HTML::Window& window_object, FlyString const& event_name, WebGLContextEventInit const& event_init)
{
    return window_object.heap().allocate<WebGLContextEvent>(window_object.realm(), window_object, event_name, event_init);
}

WebGLContextEvent* WebGLContextEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, WebGLContextEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

WebGLContextEvent::WebGLContextEvent(HTML::Window& window_object, FlyString const& type, WebGLContextEventInit const& event_init)
    : DOM::Event(window_object, type, event_init)
    , m_status_message(event_init.status_message)
{
    set_prototype(&window_object.cached_web_prototype("WebGLContextEvent"));
}

WebGLContextEvent::~WebGLContextEvent() = default;

}
