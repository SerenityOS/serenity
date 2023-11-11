/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/AnimationPlaybackEvent.h>
#include <LibWeb/Bindings/Intrinsics.h>

namespace Web::Animations {

JS::NonnullGCPtr<AnimationPlaybackEvent> AnimationPlaybackEvent::create(JS::Realm& realm, FlyString const& event_name, AnimationPlaybackEventInit const& event_init)
{
    return realm.heap().allocate<AnimationPlaybackEvent>(realm, realm, event_name, event_init);
}

// https://www.w3.org/TR/web-animations-1/#dom-animationplaybackevent-animationplaybackevent
WebIDL::ExceptionOr<JS::NonnullGCPtr<AnimationPlaybackEvent>> AnimationPlaybackEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, AnimationPlaybackEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

AnimationPlaybackEvent::AnimationPlaybackEvent(JS::Realm& realm, FlyString const& event_name, AnimationPlaybackEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_current_time(event_init.current_time)
    , m_timeline_time(event_init.timeline_time)
{
}

void AnimationPlaybackEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AnimationPlaybackEventPrototype>(realm, "AnimationPlaybackEvent"));
}

}
