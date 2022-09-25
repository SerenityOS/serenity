/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

MessageEvent* MessageEvent::create(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
{
    return realm.heap().allocate<MessageEvent>(realm, realm, event_name, event_init);
}

MessageEvent* MessageEvent::create(HTML::Window& window, FlyString const& event_name, MessageEventInit const& event_init)
{
    return create(window.realm(), event_name, event_init);
}

MessageEvent* MessageEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

MessageEvent::MessageEvent(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_data(event_init.data)
    , m_origin(event_init.origin)
    , m_last_event_id(event_init.last_event_id)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "MessageEvent"));
}

MessageEvent::~MessageEvent() = default;

void MessageEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data);
}

}
