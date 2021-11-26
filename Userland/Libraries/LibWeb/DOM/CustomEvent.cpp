/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/CustomEvent.h>

namespace Web::DOM {

void CustomEvent::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_detail);
}

// https://dom.spec.whatwg.org/#dom-customevent-initcustomevent
void CustomEvent::init_custom_event(String const& type, bool bubbles, bool cancelable, JS::Value detail)
{
    // 1. If this’s dispatch flag is set, then return.
    if (dispatched())
        return;

    // 2. Initialize this with type, bubbles, and cancelable.
    initialize(type, bubbles, cancelable);

    // 3. Set this’s detail attribute to detail.
    m_detail = detail;
}

}
