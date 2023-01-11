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
    DeprecatedString reason { "" };
};

class CloseEvent : public DOM::Event {
    WEB_PLATFORM_OBJECT(CloseEvent, DOM::Event);

public:
    static CloseEvent* create(JS::Realm&, DeprecatedFlyString const& event_name, CloseEventInit const& event_init = {});
    static CloseEvent* construct_impl(JS::Realm&, DeprecatedFlyString const& event_name, CloseEventInit const& event_init);

    virtual ~CloseEvent() override;

    bool was_clean() const { return m_was_clean; }
    u16 code() const { return m_code; }
    DeprecatedString reason() const { return m_reason; }

private:
    CloseEvent(JS::Realm&, DeprecatedFlyString const& event_name, CloseEventInit const& event_init);

    virtual void initialize(JS::Realm&) override;

    bool m_was_clean { false };
    u16 m_code { 0 };
    DeprecatedString m_reason;
};

}
