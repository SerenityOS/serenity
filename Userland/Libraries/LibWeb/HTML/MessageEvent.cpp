/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/MessageEvent.h>

namespace Web::HTML {

MessageEvent* MessageEvent::create(JS::Realm& realm, DeprecatedFlyString const& event_name, MessageEventInit const& event_init)
{
    return realm.heap().allocate<MessageEvent>(realm, realm, event_name, event_init).release_allocated_value_but_fixme_should_propagate_errors();
}

MessageEvent* MessageEvent::construct_impl(JS::Realm& realm, DeprecatedFlyString const& event_name, MessageEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

MessageEvent::MessageEvent(JS::Realm& realm, DeprecatedFlyString const& event_name, MessageEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_data(event_init.data)
    , m_origin(event_init.origin)
    , m_last_event_id(event_init.last_event_id)
{
}

MessageEvent::~MessageEvent() = default;

JS::ThrowCompletionOr<void> MessageEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MessageEventPrototype>(realm, "MessageEvent"));

    return {};
}

void MessageEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data);
}

}
