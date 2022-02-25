/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Window.h>

namespace Web::UIEvents {

struct UIEventInit : public DOM::EventInit {
    RefPtr<DOM::Window> view { nullptr };
    int detail { 0 };
};

class UIEvent : public DOM::Event {
public:
    using WrapperType = Bindings::UIEventWrapper;

    static NonnullRefPtr<UIEvent> create(FlyString const& type)
    {
        return adopt_ref(*new UIEvent(type));
    }

    static NonnullRefPtr<UIEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, UIEventInit const& event_init)
    {
        return adopt_ref(*new UIEvent(event_name, event_init));
    }

    virtual ~UIEvent() override { }

    DOM::Window const* view() const { return m_view; }
    int detail() const { return m_detail; }

    void init_ui_event(String const& type, bool bubbles, bool cancelable, DOM::Window* view, int detail)
    {
        init_event(type, bubbles, cancelable);
        m_view = view;
        m_detail = detail;
    }

protected:
    explicit UIEvent(FlyString const& event_name)
        : Event(event_name)
    {
    }
    UIEvent(FlyString const& event_name, UIEventInit const& event_init)
        : Event(event_name, event_init)
        , m_view(event_init.view)
        , m_detail(event_init.detail)
    {
    }

    RefPtr<DOM::Window> m_view;
    int m_detail { 0 };
};

}
