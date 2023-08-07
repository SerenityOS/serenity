/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/MessageEvent.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<MessageEvent>> MessageEvent::create(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<MessageEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<MessageEvent>> MessageEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

MessageEvent::MessageEvent(JS::Realm& realm, FlyString const& event_name, MessageEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_data(event_init.data)
    , m_origin(event_init.origin)
    , m_last_event_id(event_init.last_event_id)
{
}

MessageEvent::~MessageEvent() = default;

void MessageEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MessageEventPrototype>(realm, "MessageEvent"));
}

void MessageEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data);
}

}
