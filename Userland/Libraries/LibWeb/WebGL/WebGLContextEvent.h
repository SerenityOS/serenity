/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/DOM/Event.h>

namespace Web::WebGL {

struct WebGLContextEventInit final : public DOM::EventInit {
    String status_message;
};

class WebGLContextEvent final : public DOM::Event {
    WEB_PLATFORM_OBJECT(WebGLContextEvent, DOM::Event);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<WebGLContextEvent>> create(JS::Realm&, FlyString const& type, WebGLContextEventInit const& event_init);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<WebGLContextEvent>> construct_impl(JS::Realm&, FlyString const& type, WebGLContextEventInit const& event_init);

    virtual ~WebGLContextEvent() override;

    String const& status_message() const { return m_status_message; }

private:
    WebGLContextEvent(JS::Realm&, FlyString const& type, WebGLContextEventInit const& event_init);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    String m_status_message;
};

}
