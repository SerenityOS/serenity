/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/CloseEvent.h>

namespace Web::HTML {

JS::NonnullGCPtr<CloseEvent> CloseEvent::create(JS::Realm& realm, FlyString const& event_name, CloseEventInit const& event_init)
{
    return realm.heap().allocate<CloseEvent>(realm, realm, event_name, event_init);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<CloseEvent>> CloseEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, CloseEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

CloseEvent::CloseEvent(JS::Realm& realm, FlyString const& event_name, CloseEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_was_clean(event_init.was_clean)
    , m_code(event_init.code)
    , m_reason(event_init.reason)
{
}

CloseEvent::~CloseEvent() = default;

void CloseEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CloseEventPrototype>(realm, "CloseEvent"));
}

}
