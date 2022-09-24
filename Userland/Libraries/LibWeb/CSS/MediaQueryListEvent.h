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
    WEB_PLATFORM_OBJECT(MediaQueryListEvent, DOM::Event);

public:
    static MediaQueryListEvent* construct_impl(JS::Realm&, FlyString const& event_name, MediaQueryListEventInit const& event_init = {});

    virtual ~MediaQueryListEvent() override;

    String const& media() const { return m_media; }
    bool matches() const { return m_matches; }

private:
    MediaQueryListEvent(JS::Realm&, FlyString const& event_name, MediaQueryListEventInit const& event_init);

    String m_media;
    bool m_matches;
};
}
