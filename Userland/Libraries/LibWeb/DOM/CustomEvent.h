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
    JS_OBJECT(CustomEvent, Event);

public:
    static CustomEvent* create(Bindings::WindowObject&, FlyString const& event_name, CustomEventInit const& event_init = {});
    static CustomEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, CustomEventInit const& event_init);

    CustomEvent(Bindings::WindowObject&, FlyString const& event_name);
    CustomEvent(Bindings::WindowObject&, FlyString const& event_name, CustomEventInit const& event_init);

    virtual ~CustomEvent() override;

    CustomEvent& impl() { return *this; }

    // https://dom.spec.whatwg.org/#dom-customevent-detail
    JS::Value detail() const { return m_detail; }

    virtual void visit_edges(JS::Cell::Visitor&) override;

    void init_custom_event(String const& type, bool bubbles, bool cancelable, JS::Value detail);

private:
    // https://dom.spec.whatwg.org/#dom-customevent-initcustomevent-type-bubbles-cancelable-detail-detail
    JS::Value m_detail { JS::js_null() };
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::CustomEvent& object) { return &object; }
using CustomEventWrapper = Web::DOM::CustomEvent;
}
