/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/Window.h>

namespace Web::UIEvents {

struct UIEventInit : public DOM::EventInit {
    RefPtr<HTML::Window> view { nullptr };
    int detail { 0 };
};

class UIEvent : public DOM::Event {
    JS_OBJECT(UIEvent, DOM::Event);

public:
    static UIEvent* create(Bindings::WindowObject&, FlyString const& type);
    static UIEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, UIEventInit const& event_init);

    UIEvent(Bindings::WindowObject&, FlyString const& event_name);
    UIEvent(Bindings::WindowObject&, FlyString const& event_name, UIEventInit const& event_init);

    virtual ~UIEvent() override;

    UIEvent& impl() { return *this; }

    HTML::Window const* view() const { return m_view; }
    int detail() const { return m_detail; }
    virtual u32 which() const { return 0; }

    void init_ui_event(String const& type, bool bubbles, bool cancelable, HTML::Window* view, int detail)
    {
        init_event(type, bubbles, cancelable);
        m_view = view;
        m_detail = detail;
    }

protected:
    RefPtr<HTML::Window> m_view;
    int m_detail { 0 };
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::UIEvents::UIEvent& object) { return &object; }
using UIEventWrapper = Web::UIEvents::UIEvent;
}
