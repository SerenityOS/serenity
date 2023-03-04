/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/PageTransitionEvent.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<PageTransitionEvent>> PageTransitionEvent::create(JS::Realm& realm, FlyString const& event_name, PageTransitionEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<PageTransitionEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<PageTransitionEvent>> PageTransitionEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, PageTransitionEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

PageTransitionEvent::PageTransitionEvent(JS::Realm& realm, FlyString const& event_name, PageTransitionEventInit const& event_init)
    : DOM::Event(realm, event_name.to_deprecated_fly_string(), event_init)
    , m_persisted(event_init.persisted)
{
}

PageTransitionEvent::~PageTransitionEvent() = default;

JS::ThrowCompletionOr<void> PageTransitionEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::PageTransitionEventPrototype>(realm, "PageTransitionEvent"));

    return {};
}

}
