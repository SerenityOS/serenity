/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

class CloseEvent : public DOM::Event {
public:
    using WrapperType = Bindings::CloseEventWrapper;

    static NonnullRefPtr<CloseEvent> create(FlyString const& event_name, bool was_clean, u16 code, String const& reason)
    {
        return adopt_ref(*new CloseEvent(event_name, was_clean, code, reason));
    }

    virtual ~CloseEvent() override = default;

    bool was_clean() { return m_was_clean; }
    u16 code() const { return m_code; }
    String reason() const { return m_reason; }

protected:
    CloseEvent(FlyString const& event_name, bool was_clean, u16 code, String const& reason)
        : Event(event_name)
        , m_was_clean(was_clean)
        , m_code(code)
        , m_reason(reason)
    {
    }

    bool m_was_clean { false };
    u16 m_code { 0 };
    String m_reason;
};

}
