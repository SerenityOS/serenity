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

class MediaQueryListEvent final : public DOM::Event {
    JS_OBJECT(MediaQueryListEvent, DOM::Event);

public:
    static MediaQueryListEvent* create(Bindings::WindowObject&, FlyString const& event_name, MediaQueryListEventInit const& event_init = {});
    static MediaQueryListEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, MediaQueryListEventInit const& event_init);

    MediaQueryListEvent(Bindings::WindowObject&, FlyString const& event_name, MediaQueryListEventInit const& event_init);
    virtual ~MediaQueryListEvent() override;

    MediaQueryListEvent& impl() { return *this; }

    String const& media() const { return m_media; }
    bool matches() const { return m_matches; }

private:
    String m_media;
    bool m_matches;
};
}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::MediaQueryListEvent& object) { return &object; }
using MediaQueryListEventWrapper = Web::CSS::MediaQueryListEvent;
}
