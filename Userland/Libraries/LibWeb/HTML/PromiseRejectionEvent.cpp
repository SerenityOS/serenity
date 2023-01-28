/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/PromiseRejectionEvent.h>

namespace Web::HTML {

PromiseRejectionEvent* PromiseRejectionEvent::create(JS::Realm& realm, DeprecatedFlyString const& event_name, PromiseRejectionEventInit const& event_init)
{
    return realm.heap().allocate<PromiseRejectionEvent>(realm, realm, event_name, event_init).release_allocated_value_but_fixme_should_propagate_errors();
}

PromiseRejectionEvent* PromiseRejectionEvent::construct_impl(JS::Realm& realm, DeprecatedFlyString const& event_name, PromiseRejectionEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

PromiseRejectionEvent::PromiseRejectionEvent(JS::Realm& realm, DeprecatedFlyString const& event_name, PromiseRejectionEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_promise(const_cast<JS::Promise*>(event_init.promise.cell()))
    , m_reason(event_init.reason)
{
}

PromiseRejectionEvent::~PromiseRejectionEvent() = default;

void PromiseRejectionEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_promise);
    visitor.visit(m_reason);
}

JS::ThrowCompletionOr<void> PromiseRejectionEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::PromiseRejectionEventPrototype>(realm, "PromiseRejectionEvent"));

    return {};
}

}
