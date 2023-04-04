/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

struct TrackEventInit : public DOM::EventInit {
    JS::GCPtr<VideoTrack> track;
};

class TrackEvent : public DOM::Event {
    WEB_PLATFORM_OBJECT(TrackEvent, DOM::Event);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TrackEvent>> create(JS::Realm&, FlyString const& event_name, TrackEventInit const& event_init = {});
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<TrackEvent>> construct_impl(JS::Realm&, FlyString const& event_name, TrackEventInit const& event_init);

    // https://html.spec.whatwg.org/multipage/media.html#dom-trackevent-track
    JS::GCPtr<VideoTrack> track() const { return m_track; }

private:
    TrackEvent(JS::Realm&, FlyString const& event_name, TrackEventInit const& event_init);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    JS::GCPtr<VideoTrack> m_track;
};

}
