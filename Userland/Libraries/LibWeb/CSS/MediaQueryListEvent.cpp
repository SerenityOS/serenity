/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MediaQueryListEventPrototype.h>
#include <LibWeb/CSS/MediaQueryListEvent.h>

namespace Web::CSS {

WebIDL::ExceptionOr<JS::NonnullGCPtr<MediaQueryListEvent>> MediaQueryListEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, MediaQueryListEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<MediaQueryListEvent>(realm, realm, event_name, event_init));
}

MediaQueryListEvent::MediaQueryListEvent(JS::Realm& realm, FlyString const& event_name, MediaQueryListEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_media(event_init.media)
    , m_matches(event_init.matches)
{
}

MediaQueryListEvent::~MediaQueryListEvent() = default;

void MediaQueryListEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MediaQueryListEventPrototype>(realm, "MediaQueryListEvent"));
}

}
