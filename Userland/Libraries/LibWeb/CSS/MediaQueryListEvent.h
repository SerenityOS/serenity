/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::CSS {

struct MediaQueryListEventInit : public DOM::EventInit {
    String media { "" };
    bool matches { false };
};

class MediaQueryListEvent : public DOM::Event {
public:
    using WrapperType = Bindings::MediaQueryListEventWrapper;

    static NonnullRefPtr<MediaQueryListEvent> create(FlyString const& event_name, MediaQueryListEventInit const& event_init = {})
    {
        return adopt_ref(*new MediaQueryListEvent(event_name, event_init));
    }
    static NonnullRefPtr<MediaQueryListEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, MediaQueryListEventInit const& event_init)
    {
        return MediaQueryListEvent::create(event_name, event_init);
    }

    virtual ~MediaQueryListEvent() override = default;

    String const& media() const { return m_media; }
    bool matches() const { return m_matches; }

protected:
    MediaQueryListEvent(FlyString const& event_name, MediaQueryListEventInit const& event_init)
        : DOM::Event(event_name, event_init)
        , m_media(event_init.media)
        , m_matches(event_init.matches)
    {
    }

    String m_media;
    bool m_matches;
};
}
