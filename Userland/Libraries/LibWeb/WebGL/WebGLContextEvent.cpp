/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebGL/WebGLContextEvent.h>

namespace Web::WebGL {

WebGLContextEvent* WebGLContextEvent::create(JS::Realm& realm, FlyString const& event_name, WebGLContextEventInit const& event_init)
{
    return realm.heap().allocate<WebGLContextEvent>(realm, realm, event_name, event_init);
}

WebGLContextEvent* WebGLContextEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, WebGLContextEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

WebGLContextEvent::WebGLContextEvent(JS::Realm& realm, FlyString const& type, WebGLContextEventInit const& event_init)
    : DOM::Event(realm, type, event_init)
    , m_status_message(event_init.status_message)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "WebGLContextEvent"));
}

WebGLContextEvent::~WebGLContextEvent() = default;

}
