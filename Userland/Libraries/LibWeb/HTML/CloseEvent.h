/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

struct CloseEventInit : public DOM::EventInit {
    bool was_clean { false };
    u16 code { 0 };
    String reason { "" };
};

class CloseEvent : public DOM::Event {
    WEB_PLATFORM_OBJECT(CloseEvent, DOM::Event);

public:
    static CloseEvent* create(JS::Realm&, FlyString const& event_name, CloseEventInit const& event_init = {});
    static CloseEvent* construct_impl(JS::Realm&, FlyString const& event_name, CloseEventInit const& event_init);

    virtual ~CloseEvent() override;

    bool was_clean() const { return m_was_clean; }
    u16 code() const { return m_code; }
    String reason() const { return m_reason; }

private:
    CloseEvent(JS::Realm&, FlyString const& event_name, CloseEventInit const& event_init);

    bool m_was_clean { false };
    u16 m_code { 0 };
    String m_reason;
};

}
