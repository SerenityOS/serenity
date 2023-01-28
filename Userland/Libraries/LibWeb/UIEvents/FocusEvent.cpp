/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/UIEvents/FocusEvent.h>

namespace Web::UIEvents {

FocusEvent* FocusEvent::construct_impl(JS::Realm& realm, DeprecatedFlyString const& event_name, FocusEventInit const& event_init)
{
    return realm.heap().allocate<FocusEvent>(realm, realm, event_name, event_init);
}

FocusEvent::FocusEvent(JS::Realm& realm, DeprecatedFlyString const& event_name, FocusEventInit const& event_init)
    : UIEvent(realm, event_name)
{
    set_related_target(const_cast<DOM::EventTarget*>(event_init.related_target.ptr()));
}

FocusEvent::~FocusEvent() = default;

JS::ThrowCompletionOr<void> FocusEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::FocusEventPrototype>(realm, "FocusEvent"));

    return {};
}

}
