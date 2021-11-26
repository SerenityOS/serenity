/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
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
public:
    using WrapperType = Bindings::CloseEventWrapper;

    static NonnullRefPtr<CloseEvent> create(FlyString const& event_name, CloseEventInit const& event_init = {})
    {
        return adopt_ref(*new CloseEvent(event_name, event_init));
    }
    static NonnullRefPtr<CloseEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, CloseEventInit const& event_init)
    {
        return CloseEvent::create(event_name, event_init);
    }

    virtual ~CloseEvent() override = default;

    bool was_clean() const { return m_was_clean; }
    u16 code() const { return m_code; }
    String reason() const { return m_reason; }

protected:
    CloseEvent(FlyString const& event_name, CloseEventInit const& event_init)
        : Event(event_name, event_init)
        , m_was_clean(event_init.was_clean)
        , m_code(event_init.code)
        , m_reason(event_init.reason)
    {
    }

    bool m_was_clean { false };
    u16 m_code { 0 };
    String m_reason;
};

}
