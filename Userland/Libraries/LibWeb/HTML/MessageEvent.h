/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

class MessageEvent : public DOM::Event {
public:
    using WrapperType = Bindings::MessageEventWrapper;

    static NonnullRefPtr<MessageEvent> create(FlyString const& event_name, String const& data, String const& origin)
    {
        return adopt_ref(*new MessageEvent(event_name, data, origin));
    }

    virtual ~MessageEvent() override = default;

    String const& data() const { return m_data; }
    String const& origin() const { return m_origin; }

protected:
    MessageEvent(FlyString const& event_name, String const& data, String const& origin)
        : DOM::Event(event_name)
        , m_data(data)
        , m_origin(origin)
    {
    }

    String m_data;
    String m_origin;
};

}
