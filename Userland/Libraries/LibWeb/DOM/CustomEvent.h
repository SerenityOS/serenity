/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::DOM {

struct CustomEventInit : public EventInit {
    JS::Value detail { JS::js_null() };
};

// https://dom.spec.whatwg.org/#customevent
class CustomEvent : public Event {
    WEB_PLATFORM_OBJECT(CustomEvent, Event);

public:
    static CustomEvent* create(HTML::Window&, FlyString const& event_name, CustomEventInit const& event_init = {});
    static CustomEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, CustomEventInit const& event_init);

    CustomEvent(HTML::Window&, FlyString const& event_name);
    CustomEvent(HTML::Window&, FlyString const& event_name, CustomEventInit const& event_init);

    virtual ~CustomEvent() override;

    // https://dom.spec.whatwg.org/#dom-customevent-detail
    JS::Value detail() const { return m_detail; }

    virtual void visit_edges(JS::Cell::Visitor&) override;

    void init_custom_event(String const& type, bool bubbles, bool cancelable, JS::Value detail);

private:
    // https://dom.spec.whatwg.org/#dom-customevent-initcustomevent-type-bubbles-cancelable-detail-detail
    JS::Value m_detail { JS::js_null() };
};

}

WRAPPER_HACK(CustomEvent, Web::DOM)
