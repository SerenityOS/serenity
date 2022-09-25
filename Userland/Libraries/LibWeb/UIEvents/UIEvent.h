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
    JS::GCPtr<HTML::Window> view;
    int detail { 0 };
};

class UIEvent : public DOM::Event {
    WEB_PLATFORM_OBJECT(UIEvent, DOM::Event);

public:
    static UIEvent* create(HTML::Window&, FlyString const& type);
    static UIEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, UIEventInit const& event_init);

    UIEvent(HTML::Window&, FlyString const& event_name);
    UIEvent(HTML::Window&, FlyString const& event_name, UIEventInit const& event_init);

    virtual ~UIEvent() override;

    HTML::Window const* view() const { return m_view.ptr(); }
    int detail() const { return m_detail; }
    virtual u32 which() const { return 0; }

    void init_ui_event(String const& type, bool bubbles, bool cancelable, HTML::Window* view, int detail)
    {
        init_event(type, bubbles, cancelable);
        m_view = view;
        m_detail = detail;
    }

protected:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::GCPtr<HTML::Window> m_view;
    int m_detail { 0 };
};

}
