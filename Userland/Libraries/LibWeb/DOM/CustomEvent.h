/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#customevent
class CustomEvent : public Event {
public:
    using WrapperType = Bindings::CustomEventWrapper;

    static NonnullRefPtr<CustomEvent> create(FlyString const& event_name)
    {
        return adopt_ref(*new CustomEvent(event_name));
    }

    virtual ~CustomEvent() override;

    // https://dom.spec.whatwg.org/#dom-customevent-detail
    JS::Value detail() const { return m_detail; }

    void visit_edges(JS::Cell::Visitor&);

    void init_custom_event(String const& type, bool bubbles, bool cancelable, JS::Value detail);

private:
    CustomEvent(FlyString const&);

    // https://dom.spec.whatwg.org/#dom-customevent-initcustomevent-type-bubbles-cancelable-detail-detail
    JS::Value m_detail { JS::js_null() };
};

}
