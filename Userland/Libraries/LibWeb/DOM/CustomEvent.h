/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
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
public:
    using WrapperType = Bindings::CustomEventWrapper;

    static NonnullRefPtr<CustomEvent> create(FlyString const& event_name, CustomEventInit const& event_init = {})
    {
        return adopt_ref(*new CustomEvent(event_name, event_init));
    }
    static NonnullRefPtr<CustomEvent> create_with_global_object(Bindings::WindowObject&, const FlyString& event_name, CustomEventInit const& event_init)
    {
        return CustomEvent::create(event_name, event_init);
    }

    virtual ~CustomEvent() override = default;

    // https://dom.spec.whatwg.org/#dom-customevent-detail
    JS::Value detail() const { return m_detail; }

    void visit_edges(JS::Cell::Visitor&);

    void init_custom_event(String const& type, bool bubbles, bool cancelable, JS::Value detail);

private:
    explicit CustomEvent(FlyString const& event_name)
        : Event(event_name)
    {
    }

    CustomEvent(FlyString const& event_name, CustomEventInit const& event_init)
        : Event(event_name, event_init)
        , m_detail(event_init.detail)
    {
    }

    // https://dom.spec.whatwg.org/#dom-customevent-initcustomevent-type-bubbles-cancelable-detail-detail
    JS::Value m_detail { JS::js_null() };
};

}
