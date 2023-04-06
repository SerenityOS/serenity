/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/TrackEventPrototype.h>
#include <LibWeb/HTML/TrackEvent.h>

namespace Web::HTML {

WebIDL::ExceptionOr<JS::NonnullGCPtr<TrackEvent>> TrackEvent::create(JS::Realm& realm, FlyString const& event_name, TrackEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<TrackEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<TrackEvent>> TrackEvent::construct_impl(JS::Realm& realm, FlyString const& event_name, TrackEventInit const& event_init)
{
    return create(realm, event_name, event_init);
}

TrackEvent::TrackEvent(JS::Realm& realm, FlyString const& event_name, TrackEventInit const& event_init)
    : DOM::Event(realm, event_name, event_init)
    , m_track(event_init.track)
{
}

JS::ThrowCompletionOr<void> TrackEvent::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::TrackEventPrototype>(realm, "TrackEvent"));

    return {};
}

}
